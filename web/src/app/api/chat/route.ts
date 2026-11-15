import { GoogleGenAI, type Content, type Part } from "@google/genai";
import { NextRequest } from "next/server";
import { buildSystemInstruction } from "@/lib/systemPrompt";
import type { ChatAttachment, ResponseStyle } from "@/types/chat";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

interface IncomingMessage {
  role: "user" | "model";
  content: string;
  attachments?: ChatAttachment[];
}

interface IncomingBody {
  messages?: IncomingMessage[];
  responseStyle?: ResponseStyle;
}

const MAX_MESSAGES = 60;
const MAX_IMAGE_BASE64_CHARS = 12 * 1024 * 1024; // ~9 MB binary
const MAX_TEXT_ATTACHMENT_CHARS = 120_000;

function jsonError(status: number, message: string, code: string) {
  return Response.json(
    { error: message, code },
    { status, headers: { "Cache-Control": "no-store" } }
  );
}

function classifyGeminiError(err: unknown): { status: number; message: string; code: string } {
  const raw = err instanceof Error ? err.message : String(err ?? "");
  const lower = raw.toLowerCase();

  if (
    lower.includes("api_key_invalid") ||
    lower.includes("api key not valid") ||
    lower.includes("invalid api key") ||
    lower.includes("permission_denied") ||
    lower.includes("401") ||
    lower.includes("403")
  ) {
    return {
      status: 401,
      code: "INVALID_API_KEY",
      message: "The configured Gemini API key appears invalid or lacks permission. Please verify GEMINI_API_KEY in your server environment."
    };
  }

  if (lower.includes("429") || lower.includes("quota") || lower.includes("resource_exhausted") || lower.includes("rate")) {
    return {
      status: 429,
      code: "RATE_LIMIT",
      message: "Gemini API rate limit or quota reached. Please wait a moment and try again."
    };
  }

  if (lower.includes("not found") || lower.includes("404") || lower.includes("model")) {
    return {
      status: 400,
      code: "MODEL_ERROR",
      message: "The configured Gemini model could not be reached. Please check GEMINI_MODEL in your server environment."
    };
  }

  if (lower.includes("fetch") || lower.includes("network") || lower.includes("econn") || lower.includes("timeout")) {
    return {
      status: 502,
      code: "NETWORK_ERROR",
      message: "Unable to reach the Gemini AI service right now. Please check your connection and try again."
    };
  }

  return {
    status: 500,
    code: "AI_ERROR",
    message: "ChatQivo encountered an error while generating a response. Please try again."
  };
}

export async function POST(request: NextRequest) {
  const apiKey = process.env.GEMINI_API_KEY?.trim();
  const modelName = process.env.GEMINI_MODEL?.trim() || "gemini-2.5-flash";

  if (!apiKey || apiKey === "your_gemini_api_key") {
    return jsonError(
      500,
      "Server configuration incomplete: GEMINI_API_KEY is not set. Add GEMINI_API_KEY in Vercel Environment Variables or .env.local.",
      "MISSING_API_KEY"
    );
  }

  let body: IncomingBody;
  try {
    body = (await request.json()) as IncomingBody;
  } catch {
    return jsonError(400, "Invalid request body. Expected JSON.", "INVALID_JSON");
  }

  if (!Array.isArray(body.messages) || body.messages.length === 0) {
    return jsonError(400, "Please enter a message before sending.", "EMPTY_MESSAGES");
  }

  const trimmedMessages = body.messages.slice(-MAX_MESSAGES);
  const contents: Content[] = [];

  for (const msg of trimmedMessages) {
    if (msg.role !== "user" && msg.role !== "model") {
      return jsonError(400, "Invalid message role in conversation history.", "INVALID_ROLE");
    }

    const parts: Part[] = [];
    const textContent = typeof msg.content === "string" ? msg.content.trim() : "";

    if (Array.isArray(msg.attachments)) {
      for (const att of msg.attachments) {
        if (!att || typeof att.data !== "string") continue;
        if (att.kind === "image") {
          if (att.data.length > MAX_IMAGE_BASE64_CHARS) {
            return jsonError(400, `Attached image "${att.name}" exceeds the 9MB size limit.`, "ATTACHMENT_TOO_LARGE");
          }
          if (!att.mimeType?.startsWith("image/")) {
            return jsonError(400, `Unsupported image type for "${att.name}".`, "UNSUPPORTED_ATTACHMENT");
          }
          parts.push({
            inlineData: {
              mimeType: att.mimeType,
              data: att.data
            }
          });
        } else if (att.kind === "text") {
          const safeText = att.data.slice(0, MAX_TEXT_ATTACHMENT_CHARS);
          parts.push({
            text: `[Attached file: ${att.name}]\n\`\`\`\n${safeText}\n\`\`\``
          });
        }
      }
    }

    if (textContent.length > 0) {
      parts.push({ text: textContent });
    } else if (parts.length > 0 && msg.role === "user") {
      parts.push({ text: "Please analyze the attached file(s) and help me with them." });
    }

    if (parts.length > 0) {
      const last = contents[contents.length - 1];
      if (last && last.role === msg.role) {
        last.parts = [...(last.parts ?? []), ...parts];
      } else if (contents.length === 0 && msg.role !== "user") {
        // Skip leading model turns before first user turn
        continue;
      } else {
        contents.push({
          role: msg.role,
          parts
        });
      }
    }
  }

  if (contents.length === 0 || contents[contents.length - 1].role !== "user") {
    return jsonError(400, "Message cannot be empty.", "EMPTY_PROMPT");
  }

  const responseStyle: ResponseStyle =
    body.responseStyle === "concise" || body.responseStyle === "detailed"
      ? body.responseStyle
      : "balanced";

  const ai = new GoogleGenAI({ apiKey });
  const encoder = new TextEncoder();

  try {
    const streamResult = await ai.models.generateContentStream({
      model: modelName,
      contents,
      config: {
        systemInstruction: buildSystemInstruction(responseStyle),
        temperature: 0.7,
        topP: 0.95
      }
    });

    const readable = new ReadableStream({
      async start(controller) {
        let aborted = false;
        const onAbort = () => {
          aborted = true;
          try {
            controller.close();
          } catch {
            // Stream already closed
          }
        };
        request.signal.addEventListener("abort", onAbort, { once: true });

        try {
          for await (const chunk of streamResult) {
            if (aborted || request.signal.aborted) break;
            const text = chunk.text;
            if (text) {
              const payload = JSON.stringify({ text });
              controller.enqueue(encoder.encode(`data: ${payload}\n\n`));
            }
          }
          if (!aborted && !request.signal.aborted) {
            controller.enqueue(encoder.encode("data: [DONE]\n\n"));
            controller.close();
          }
        } catch (streamErr) {
          if (!aborted && !request.signal.aborted) {
            const classified = classifyGeminiError(streamErr);
            const errPayload = JSON.stringify({
              error: classified.message,
              code: classified.code
            });
            controller.enqueue(encoder.encode(`data: ${errPayload}\n\n`));
            controller.close();
          }
        } finally {
          request.signal.removeEventListener("abort", onAbort);
        }
      }
    });

    return new Response(readable, {
      status: 200,
      headers: {
        "Content-Type": "text/event-stream; charset=utf-8",
        "Cache-Control": "no-cache, no-transform",
        Connection: "keep-alive",
        "X-Accel-Buffering": "no"
      }
    });
  } catch (err) {
    const classified = classifyGeminiError(err);
    return jsonError(classified.status, classified.message, classified.code);
  }
}
