"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import MarkdownRenderer from "@/components/MarkdownRenderer";
import { detectSpeechLang, getTextDirection } from "@/lib/rtl";
import type {
  ChatAttachment,
  ChatMessage,
  ChatSettings,
  Conversation,
  ResponseStyle,
} from "@/types/chat";

const STORAGE_CONVOS_KEY = "chatqivo_conversations_v1";
const STORAGE_ACTIVE_KEY = "chatqivo_active_id_v1";
const STORAGE_SETTINGS_KEY = "chatqivo_settings_v1";

const DEFAULT_SETTINGS: ChatSettings = {
  responseStyle: "balanced",
  autoScroll: true,
};

const SUGGESTIONS = [
  {
    icon: "✎",
    title: "Write something",
    subtitle: "Ideas, drafts and better words",
    prompt: "Help me write something clear, engaging, and well-structured.",
  },
  {
    icon: "⌘",
    title: "Understand a topic",
    subtitle: "Learn with simple explanations",
    prompt: "Explain a complex topic to me step by step in simple language.",
  },
  {
    icon: "◇",
    title: "Build a project",
    subtitle: "Turn ideas into real things",
    prompt: "Help me plan and build a modern responsive website using HTML, CSS, and JavaScript.",
  },
  {
    icon: "✧",
    title: "Explore an idea",
    subtitle: "Think, research and discover",
    prompt: "Help me explore and structure a creative new project idea.",
  },
];

const STUDIO_TEMPLATES = [
  {
    category: "Website & Code",
    title: "Full Webpage Builder",
    description: "Generate clean, responsive HTML, CSS, and JavaScript or React components.",
    prompt: "Build a clean, responsive landing page using HTML and CSS for:",
  },
  {
    category: "AI Prompts",
    title: "Pro AI Prompt Generator",
    description: "Create detailed, structured prompts with role, constraints, and output format.",
    prompt: "Create a professional, ready-to-use AI prompt (including role, context, rules, and output format) for:",
  },
  {
    category: "Multilingual & Urdu",
    title: "Urdu / Arabic / English Assistant",
    description: "Write, explain, or translate naturally in Urdu, Arabic, Hindi, Punjabi, or Roman Urdu.",
    prompt: "براہِ کرم اس موضوع پر آسان اور واضح اردو میں تفصیل سے رہنمائی کریں:",
  },
  {
    category: "Writing",
    title: "Clear Rewrite & Polish",
    description: "Improve tone, clarity, and flow without losing your voice.",
    prompt: "Rewrite the following text to be clearer, sharper, and more natural:",
  },
];

function uid(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
}

function makeTitle(seed: string): string {
  const clean = seed.trim().replace(/\s+/g, " ");
  if (!clean) return "New conversation";
  return clean.length > 36 ? `${clean.slice(0, 36).trimEnd()}…` : clean;
}

interface SpeechRecognitionInstance {
  lang: string;
  interimResults: boolean;
  continuous: boolean;
  onresult: ((event: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
  onerror: ((event: { error: string }) => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
}

export default function ChatQivoApp() {
  const [hydrated, setHydrated] = useState(false);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<"home" | "chat" | "studio">("home");
  const [settings, setSettings] = useState<ChatSettings>(DEFAULT_SETTINGS);

  const [input, setInput] = useState("");
  const [attachments, setAttachments] = useState<ChatAttachment[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const [isStreaming, setIsStreaming] = useState(false);
  const [waitingFirstToken, setWaitingFirstToken] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [copiedMsgId, setCopiedMsgId] = useState<string | null>(null);
  const [shareToast, setShareToast] = useState<string | null>(null);
  const [speakingId, setSpeakingId] = useState<string | null>(null);
  const [isListening, setIsListening] = useState(false);

  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameDraft, setRenameDraft] = useState("");
  const [settingsOpen, setSettingsOpen] = useState(false);

  const abortRef = useRef<AbortController | null>(null);
  const chatScrollRef = useRef<HTMLDivElement | null>(null);
  const homeTextareaRef = useRef<HTMLTextAreaElement | null>(null);
  const chatTextareaRef = useRef<HTMLTextAreaElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);

  // Restore saved conversations & settings from localStorage
  useEffect(() => {
    try {
      const rawConvos = localStorage.getItem(STORAGE_CONVOS_KEY);
      if (rawConvos) {
        const parsed = JSON.parse(rawConvos) as Conversation[];
        if (Array.isArray(parsed)) {
          setConversations(parsed);
          const savedActive = localStorage.getItem(STORAGE_ACTIVE_KEY);
          if (savedActive && parsed.some((c) => c.id === savedActive)) {
            setActiveId(savedActive);
            setViewMode("chat");
          }
        }
      }
      const rawSettings = localStorage.getItem(STORAGE_SETTINGS_KEY);
      if (rawSettings) {
        setSettings({ ...DEFAULT_SETTINGS, ...(JSON.parse(rawSettings) as Partial<ChatSettings>) });
      }
    } catch {
      // Ignore malformed storage
    } finally {
      setHydrated(true);
    }
  }, []);

  // Persist conversations
  useEffect(() => {
    if (!hydrated) return;
    try {
      // Strip transient previewUrl if large, keep base64 data cleanly
      localStorage.setItem(STORAGE_CONVOS_KEY, JSON.stringify(conversations));
    } catch {
      // Storage quota full fallback: keep latest 20 conversations without heavy inline attachments
      try {
        const compact = conversations.slice(0, 20).map((c) => ({
          ...c,
          messages: c.messages.map((m) => ({
            ...m,
            attachments: m.attachments?.map((a) =>
              a.kind === "image" ? { ...a, data: "", previewUrl: undefined } : a
            ),
          })),
        }));
        localStorage.setItem(STORAGE_CONVOS_KEY, JSON.stringify(compact));
      } catch {
        // Ignore
      }
    }
  }, [conversations, hydrated]);

  useEffect(() => {
    if (!hydrated) return;
    try {
      if (activeId) {
        localStorage.setItem(STORAGE_ACTIVE_KEY, activeId);
      } else {
        localStorage.removeItem(STORAGE_ACTIVE_KEY);
      }
    } catch {
      // Ignore
    }
  }, [activeId, hydrated]);

  useEffect(() => {
    if (!hydrated) return;
    try {
      localStorage.setItem(STORAGE_SETTINGS_KEY, JSON.stringify(settings));
    } catch {
      // Ignore
    }
  }, [settings, hydrated]);

  const activeConversation = useMemo(
    () => conversations.find((c) => c.id === activeId) ?? null,
    [conversations, activeId]
  );

  const filteredConversations = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return conversations;
    return conversations.filter(
      (c) =>
        c.title.toLowerCase().includes(q) ||
        c.messages.some((m) => m.content.toLowerCase().includes(q))
    );
  }, [conversations, searchQuery]);

  // Auto-scroll chat thread smoothly when near bottom
  const scrollToBottom = useCallback(() => {
    const el = chatScrollRef.current;
    if (!el) return;
    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    if (distanceFromBottom < 260) {
      el.scrollTop = el.scrollHeight;
    }
  }, []);

  useEffect(() => {
    if (viewMode === "chat" && settings.autoScroll) {
      scrollToBottom();
    }
  }, [activeConversation?.messages, viewMode, settings.autoScroll, scrollToBottom]);

  // Auto-resize active textarea
  useEffect(() => {
    const el = viewMode === "chat" ? chatTextareaRef.current : homeTextareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.min(el.scrollHeight, 180)}px`;
  }, [input, viewMode]);

  const stopGeneration = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    setIsStreaming(false);
    setWaitingFirstToken(false);
  }, []);

  const streamCompletion = useCallback(
    async (conversationId: string, messagesForApi: ChatMessage[], aiMsgId: string) => {
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;

      setIsStreaming(true);
      setWaitingFirstToken(true);
      setErrorMsg(null);

      try {
        const res = await fetch("/api/chat", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          signal: controller.signal,
          body: JSON.stringify({
            responseStyle: settings.responseStyle,
            messages: messagesForApi
              .filter((m) => !m.isError)
              .map((m) => ({
                role: m.role,
                content: m.content,
                attachments: m.attachments?.filter((a) => a.data.length > 0),
              })),
          }),
        });

        if (!res.ok) {
          let errText = "Unable to get a response from ChatQivo.";
          try {
            const data = (await res.json()) as { error?: string };
            if (data.error) errText = data.error;
          } catch {
            // Non-JSON response
          }
          throw new Error(errText);
        }

        if (!res.body) {
          throw new Error("Streaming response body is unavailable.");
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        let accumulated = "";

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const events = buffer.split("\n\n");
          buffer = events.pop() ?? "";

          for (const evt of events) {
            const lines = evt.split("\n");
            for (const line of lines) {
              const trimmed = line.trim();
              if (!trimmed.startsWith("data:")) continue;
              const dataStr = trimmed.slice(5).trim();
              if (dataStr === "[DONE]") continue;

              let parsed: { text?: string; error?: string };
              try {
                parsed = JSON.parse(dataStr) as { text?: string; error?: string };
              } catch {
                continue;
              }

              if (parsed.error) {
                throw new Error(parsed.error);
              }
              if (parsed.text) {
                accumulated += parsed.text;
                setWaitingFirstToken(false);
                setConversations((prev) =>
                  prev.map((c) =>
                    c.id === conversationId
                      ? {
                          ...c,
                          updatedAt: Date.now(),
                          messages: c.messages.map((m) =>
                            m.id === aiMsgId ? { ...m, content: accumulated } : m
                          ),
                        }
                      : c
                  )
                );
              }
            }
          }
        }

        if (!accumulated.trim()) {
          throw new Error("ChatQivo returned an empty response. Please try again.");
        }
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") {
          // User stopped generation; keep whatever streamed so far, or remove empty placeholder
          setConversations((prev) =>
            prev.map((c) =>
              c.id === conversationId
                ? {
                    ...c,
                    messages: c.messages.filter(
                      (m) => !(m.id === aiMsgId && m.content.trim().length === 0)
                    ),
                  }
                : c
            )
          );
          return;
        }

        const friendly =
          err instanceof Error && err.message
            ? err.message
            : "Network error while contacting ChatQivo. Please try again.";

        setErrorMsg(friendly);
        // Clean up empty AI placeholder if nothing streamed
        setConversations((prev) =>
          prev.map((c) =>
            c.id === conversationId
              ? {
                  ...c,
                  messages: c.messages.filter(
                    (m) => !(m.id === aiMsgId && m.content.trim().length === 0)
                  ),
                }
              : c
          )
        );
      } finally {
        if (abortRef.current === controller) {
          abortRef.current = null;
        }
        setIsStreaming(false);
        setWaitingFirstToken(false);
      }
    },
    [settings.responseStyle]
  );

  const handleSend = useCallback(
    async (overridePrompt?: string) => {
      if (isStreaming) return;

      const text = (overridePrompt ?? input).trim();
      const currentAttachments = overridePrompt ? [] : attachments;

      if (!text && currentAttachments.length === 0) {
        setErrorMsg("Please enter a message or attach a file before sending.");
        return;
      }

      setErrorMsg(null);
      if (!overridePrompt) {
        setInput("");
        setAttachments([]);
      }

      const now = Date.now();
      const userMsg: ChatMessage = {
        id: uid(),
        role: "user",
        content: text,
        attachments: currentAttachments.length > 0 ? currentAttachments : undefined,
        createdAt: now,
      };
      const aiPlaceholder: ChatMessage = {
        id: uid(),
        role: "model",
        content: "",
        createdAt: now + 1,
      };

      let targetConvoId = activeId;
      let historyForApi: ChatMessage[] = [];

      if (!targetConvoId || viewMode !== "chat") {
        targetConvoId = uid();
        const titleSeed = text || currentAttachments[0]?.name || "New conversation";
        const newConvo: Conversation = {
          id: targetConvoId,
          title: makeTitle(titleSeed),
          messages: [userMsg, aiPlaceholder],
          createdAt: now,
          updatedAt: now,
        };
        historyForApi = [userMsg];
        setConversations((prev) => [newConvo, ...prev]);
        setActiveId(targetConvoId);
        setViewMode("chat");
      } else {
        const existing = conversations.find((c) => c.id === targetConvoId);
        historyForApi = [...(existing?.messages ?? []), userMsg];
        setConversations((prev) =>
          prev.map((c) =>
            c.id === targetConvoId
              ? {
                  ...c,
                  updatedAt: now,
                  messages: [...c.messages, userMsg, aiPlaceholder],
                }
              : c
          )
        );
      }

      setSidebarOpen(false);
      setTimeout(() => {
        if (chatScrollRef.current) {
          chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
        }
      }, 30);

      await streamCompletion(targetConvoId, historyForApi, aiPlaceholder.id);
    },
    [isStreaming, input, attachments, activeId, viewMode, conversations, streamCompletion]
  );

  const handleRegenerate = useCallback(async () => {
    if (isStreaming || !activeConversation) return;

    const msgs = activeConversation.messages;
    const lastUserIdx = [...msgs].reverse().findIndex((m) => m.role === "user");
    if (lastUserIdx === -1) return;
    const actualUserIdx = msgs.length - 1 - lastUserIdx;

    const keptMessages = msgs.slice(0, actualUserIdx + 1);
    const aiPlaceholder: ChatMessage = {
      id: uid(),
      role: "model",
      content: "",
      createdAt: Date.now(),
    };

    setConversations((prev) =>
      prev.map((c) =>
        c.id === activeConversation.id
          ? { ...c, updatedAt: Date.now(), messages: [...keptMessages, aiPlaceholder] }
          : c
      )
    );

    await streamCompletion(activeConversation.id, keptMessages, aiPlaceholder.id);
  }, [isStreaming, activeConversation, streamCompletion]);

  const handleNewChat = useCallback(() => {
    stopGeneration();
    setActiveId(null);
    setViewMode("home");
    setInput("");
    setAttachments([]);
    setErrorMsg(null);
    setSidebarOpen(false);
    setTimeout(() => homeTextareaRef.current?.focus(), 40);
  }, [stopGeneration]);

  const handleSelectConversation = useCallback(
    (id: string) => {
      stopGeneration();
      setActiveId(id);
      setViewMode("chat");
      setErrorMsg(null);
      setSidebarOpen(false);
    },
    [stopGeneration]
  );

  const handleDeleteConversation = useCallback(
    (id: string) => {
      if (id === activeId) {
        stopGeneration();
        setActiveId(null);
        setViewMode("home");
      }
      setConversations((prev) => prev.filter((c) => c.id !== id));
    },
    [activeId, stopGeneration]
  );

  const handleCommitRename = useCallback((id: string) => {
    const trimmed = renameDraft.trim();
    if (trimmed) {
      setConversations((prev) =>
        prev.map((c) => (c.id === id ? { ...c, title: trimmed, updatedAt: Date.now() } : c))
      );
    }
    setRenamingId(null);
    setRenameDraft("");
  }, [renameDraft]);

  const handleShare = useCallback(
    async (textOverride?: string) => {
      const content =
        textOverride ||
        (activeConversation
          ? `${activeConversation.title}\n\n${activeConversation.messages
              .map((m) => `${m.role === "user" ? "You" : "ChatQivo"}:\n${m.content}`)
              .join("\n\n")}`
          : window.location.href);

      try {
        if (navigator.share && textOverride === undefined && activeConversation) {
          await navigator.share({
            title: `ChatQivo — ${activeConversation.title}`,
            text: content,
          });
          return;
        }
        await navigator.clipboard.writeText(content);
        setShareToast("✓ Copied");
        setTimeout(() => setShareToast(null), 1600);
      } catch {
        setShareToast("✓ Ready");
        setTimeout(() => setShareToast(null), 1600);
      }
    },
    [activeConversation]
  );

  const handleCopyMessage = useCallback(async (id: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedMsgId(id);
      setTimeout(() => setCopiedMsgId(null), 1600);
    } catch {
      // Ignore
    }
  }, []);

  const handleSpeakToggle = useCallback(
    (id: string, text: string) => {
      if (typeof window === "undefined" || !("speechSynthesis" in window)) {
        setErrorMsg("Text-to-speech is not supported in this browser.");
        return;
      }
      if (speakingId === id) {
        window.speechSynthesis.cancel();
        setSpeakingId(null);
        return;
      }
      window.speechSynthesis.cancel();
      const plain = text.replace(/```[\s\S]*?```/g, " code block ").replace(/[#*_`>]/g, "");
      const utterance = new SpeechSynthesisUtterance(plain);
      utterance.lang = detectSpeechLang(plain);
      utterance.onend = () => setSpeakingId(null);
      utterance.onerror = () => setSpeakingId(null);
      setSpeakingId(id);
      window.speechSynthesis.speak(utterance);
    },
    [speakingId]
  );

  const handleFilePick = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    e.target.value = "";
    if (files.length === 0) return;

    const loaded: ChatAttachment[] = [];
    for (const file of files.slice(0, 4)) {
      if (file.type.startsWith("image/")) {
        if (file.size > 8 * 1024 * 1024) {
          setErrorMsg(`"${file.name}" is larger than 8MB. Please choose a smaller image.`);
          continue;
        }
        const dataUrl = await new Promise<string>((resolve, reject) => {
          const reader = new FileReader();
          reader.onload = () => resolve(String(reader.result ?? ""));
          reader.onerror = () => reject(new Error("Read error"));
          reader.readAsDataURL(file);
        });
        const base64 = dataUrl.split(",")[1] ?? "";
        loaded.push({
          id: uid(),
          name: file.name,
          mimeType: file.type,
          kind: "image",
          data: base64,
          previewUrl: dataUrl,
        });
      } else {
        if (file.size > 250 * 1024) {
          setErrorMsg(`"${file.name}" exceeds the 250KB text attachment limit.`);
          continue;
        }
        const text = await file.text();
        loaded.push({
          id: uid(),
          name: file.name,
          mimeType: file.type || "text/plain",
          kind: "text",
          data: text,
        });
      }
    }
    if (loaded.length > 0) {
      setAttachments((prev) => [...prev, ...loaded]);
      setErrorMsg(null);
    }
  }, []);

  const handleVoiceInput = useCallback(() => {
    if (isListening) {
      recognitionRef.current?.stop();
      setIsListening(false);
      return;
    }

    const win = window as unknown as {
      SpeechRecognition?: new () => SpeechRecognitionInstance;
      webkitSpeechRecognition?: new () => SpeechRecognitionInstance;
    };
    const SpeechRec = win.SpeechRecognition || win.webkitSpeechRecognition;
    if (!SpeechRec) {
      setErrorMsg("Voice input is not supported in this browser. Try Chrome or Edge.");
      return;
    }

    const rec = new SpeechRec();
    rec.lang = navigator.language || "en-US";
    rec.interimResults = false;
    rec.continuous = false;
    rec.onresult = (event) => {
      const transcript = event.results[0]?.[0]?.transcript ?? "";
      if (transcript) {
        setInput((prev) => (prev ? `${prev} ${transcript}` : transcript));
      }
    };
    rec.onerror = () => {
      setIsListening(false);
    };
    rec.onend = () => {
      setIsListening(false);
    };
    recognitionRef.current = rec;
    setIsListening(true);
    rec.start();
  }, [isListening]);

  const handleExportHistory = useCallback(() => {
    const blob = new Blob([JSON.stringify(conversations, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `chatqivo-history-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }, [conversations]);

  const handleTextareaKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      if (!isStreaming) {
        void handleSend();
      }
    }
  };

  const topTitle =
    viewMode === "studio"
      ? "Prompt Studio"
      : activeConversation
        ? activeConversation.title
        : "New chat";

  const renderPromptBox = (isChatMode: boolean) => (
    <div className="promptbox">
      {attachments.length > 0 && (
        <div className="attachment-strip">
          {attachments.map((att) => (
            <div key={att.id} className="attachment-chip">
              {att.kind === "image" && att.previewUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={att.previewUrl} alt={att.name} />
              ) : (
                <span>📄</span>
              )}
              <span>{att.name}</span>
              <button
                type="button"
                aria-label={`Remove ${att.name}`}
                onClick={() =>
                  setAttachments((prev) => prev.filter((item) => item.id !== att.id))
                }
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      <textarea
        ref={isChatMode ? chatTextareaRef : homeTextareaRef}
        value={input}
        dir="auto"
        onChange={(e) => {
          setInput(e.target.value);
          if (errorMsg) setErrorMsg(null);
        }}
        onKeyDown={handleTextareaKeyDown}
        placeholder={isListening ? "Listening… speak now" : "Message ChatQivo..."}
        rows={1}
      />

      <div className="prompttools">
        <div className="tools">
          <button
            type="button"
            className="tool"
            title="Attach image or code/text file"
            aria-label="Attach file"
            onClick={() => fileInputRef.current?.click()}
          >
            ＋
          </button>
          <button
            type="button"
            className={`tool ${isListening ? "listening" : ""}`}
            title="Voice input"
            aria-label="Voice input"
            onClick={handleVoiceInput}
          >
            ♩
          </button>
          <button
            type="button"
            className="model"
            title="Open settings"
            onClick={() => setSettingsOpen(true)}
          >
            ChatQivo AI ⌄
          </button>
        </div>

        {isStreaming ? (
          <button
            type="button"
            className="send stop"
            aria-label="Stop generating"
            title="Stop generating"
            onClick={stopGeneration}
          >
            ■
          </button>
        ) : (
          <button
            type="button"
            className="send"
            aria-label="Send"
            disabled={!input.trim() && attachments.length === 0}
            onClick={() => void handleSend()}
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M12 19V5m-7 7 7-7 7 7" />
            </svg>
          </button>
        )}
      </div>
    </div>
  );

  return (
    <div className="shell">
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept="image/*,.txt,.md,.json,.js,.jsx,.ts,.tsx,.py,.html,.css,.csv,.xml,.sql"
        style={{ display: "none" }}
        onChange={handleFilePick}
      />

      <div
        className={`sidebar-backdrop ${sidebarOpen ? "open" : ""}`}
        onClick={() => setSidebarOpen(false)}
      />

      <aside className={`sidebar ${sidebarOpen ? "open" : ""}`}>
        <button type="button" className="brand" onClick={handleNewChat}>
          <svg viewBox="0 0 40 40" fill="none" aria-hidden="true">
            <path d="M20 2 37 11.5v17L20 38 3 28.5v-17L20 2Z" fill="#08766d" />
            <path d="M20 2 37 11.5 20 21 3 11.5 20 2Z" fill="#74cbb7" />
            <path d="M3 11.5 20 21v17L3 28.5v-17Z" fill="#0b625b" />
            <path d="M37 11.5 20 21v17l17-9.5v-17Z" fill="#064c49" />
            <path
              d="M20 2v19M3 11.5 20 21l17-9.5M20 21v17"
              stroke="#174b45"
              strokeWidth="1.2"
            />
          </svg>
          <b>
            Chat<span>Qivo</span>
          </b>
        </button>

        <button type="button" className="newchat" onClick={handleNewChat}>
          ＋ <span>New chat</span>
        </button>

        <div className="search">
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            aria-hidden="true"
          >
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-4-4" />
          </svg>
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search chats"
            aria-label="Search chats"
          />
        </div>

        <div className="label">Workspace</div>
        <button
          type="button"
          className={`nav ${viewMode === "home" ? "active" : ""}`}
          onClick={handleNewChat}
        >
          ⌂ <span>Home</span>
        </button>
        <button
          type="button"
          className={`nav ${viewMode === "studio" ? "active" : ""}`}
          onClick={() => {
            setViewMode("studio");
            setSidebarOpen(false);
          }}
        >
          ▧ <span>Prompt Studio</span>
        </button>

        <div className="history">
          <div className="label" style={{ marginTop: 14 }}>
            Recent chats
          </div>
          {filteredConversations.length === 0 ? (
            <div className="history-empty">
              {searchQuery.trim()
                ? "No matching chats found."
                : "Your conversations will appear here."}
            </div>
          ) : (
            filteredConversations.map((convo) => (
              <div
                key={convo.id}
                className={`history-item ${
                  viewMode === "chat" && convo.id === activeId ? "active" : ""
                }`}
              >
                {renamingId === convo.id ? (
                  <input
                    className="history-rename-input"
                    value={renameDraft}
                    autoFocus
                    onChange={(e) => setRenameDraft(e.target.value)}
                    onBlur={() => handleCommitRename(convo.id)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") handleCommitRename(convo.id);
                      if (e.key === "Escape") setRenamingId(null);
                    }}
                  />
                ) : (
                  <button
                    type="button"
                    className="history-title"
                    title={convo.title}
                    onClick={() => handleSelectConversation(convo.id)}
                  >
                    {convo.title}
                  </button>
                )}

                <div className="history-item-actions">
                  <button
                    type="button"
                    className="icon-mini-btn"
                    title="Rename conversation"
                    aria-label="Rename conversation"
                    onClick={(e) => {
                      e.stopPropagation();
                      setRenamingId(convo.id);
                      setRenameDraft(convo.title);
                    }}
                  >
                    ✎
                  </button>
                  <button
                    type="button"
                    className="icon-mini-btn danger"
                    title="Delete conversation"
                    aria-label="Delete conversation"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteConversation(convo.id);
                    }}
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="bottom">
          <button
            type="button"
            className="nav"
            onClick={() => {
              setSettingsOpen(true);
              setSidebarOpen(false);
            }}
          >
            ⚙ <span>Settings</span>
          </button>
          <div className="userbox">
            <div className="avatar">CQ</div>
            <div>
              <strong>Guest User</strong>
              <small>Personal workspace</small>
            </div>
          </div>
        </div>
      </aside>

      <main className="main">
        <header className="top">
          <div className="top-left">
            <button
              type="button"
              className="hamb"
              aria-label="Toggle menu"
              onClick={() => setSidebarOpen((o) => !o)}
            >
              ☰
            </button>
            <div className="page-title">{topTitle}</div>
            <span className="badge">{isStreaming ? "Generating…" : "Gemini AI"}</span>
          </div>
          <div className="top-right">
            <button type="button" className="topbtn" onClick={() => void handleShare()}>
              {shareToast ?? "↗ Share"}
            </button>
            <button
              type="button"
              className="topbtn"
              aria-label="Settings"
              onClick={() => setSettingsOpen(true)}
            >
              ⚙
            </button>
          </div>
        </header>

        {viewMode === "home" && (
          <section className="home">
            <div className="hero">
              <div className="spark">
                <i /> YOUR AI SPACE, REIMAGINED
              </div>
              <h1>
                What can I help <em>you</em> with?
              </h1>
              <p className="sub">
                A little curiosity can lead to something brilliant.
                <br />
                Start with a question, an idea, or a task.
              </p>

              {errorMsg && (
                <div className="error-banner" role="alert" style={{ marginBottom: 14 }}>
                  <span>{errorMsg}</span>
                  <div className="error-banner-actions">
                    <button type="button" onClick={() => setErrorMsg(null)}>
                      Dismiss
                    </button>
                  </div>
                </div>
              )}

              {renderPromptBox(false)}

              <div className="suggestions">
                {SUGGESTIONS.map((item) => (
                  <button
                    key={item.title}
                    type="button"
                    className="suggest"
                    onClick={() => {
                      setInput(item.prompt);
                      homeTextareaRef.current?.focus();
                    }}
                  >
                    <span className="ico">{item.icon}</span>
                    <strong>{item.title}</strong>
                    <small>{item.subtitle}</small>
                  </button>
                ))}
              </div>
              <div className="hint">
                ChatQivo can make mistakes. Check important information.
              </div>
            </div>
            <div className="home-footer">
              <span>Private conversations</span>
              <span>·</span>
              <span>Made for curious minds</span>
            </div>
          </section>
        )}

        {viewMode === "studio" && (
          <section className="studio">
            <h2>Prompt Studio</h2>
            <p className="studio-sub">
              Pick a ready-made starter for coding, writing, AI prompts, or multilingual Urdu/Arabic conversations.
            </p>
            <div className="studio-grid">
              {STUDIO_TEMPLATES.map((tpl) => (
                <button
                  key={tpl.title}
                  type="button"
                  className="studio-card"
                  onClick={() => {
                    setActiveId(null);
                    setViewMode("home");
                    setInput(tpl.prompt);
                    setTimeout(() => homeTextareaRef.current?.focus(), 40);
                  }}
                >
                  <span className="studio-cat">{tpl.category}</span>
                  <strong>{tpl.title}</strong>
                  <p>{tpl.description}</p>
                </button>
              ))}
            </div>
          </section>
        )}

        {viewMode === "chat" && (
          <section className="chatpage">
            <div className="chat-scroll" ref={chatScrollRef}>
              <div className="thread">
                <div className="date-label">Today · ChatQivo conversation</div>

                {activeConversation?.messages.map((msg, idx) => {
                  const isLastModel =
                    msg.role === "model" &&
                    idx === (activeConversation?.messages.length ?? 0) - 1;
                  const showTyping = isLastModel && isStreaming && waitingFirstToken && !msg.content;

                  if (msg.role === "user") {
                    const dir = getTextDirection(msg.content);
                    return (
                      <div key={msg.id} className="msg user">
                        <div className="bubble" dir={dir}>
                          {msg.attachments && msg.attachments.length > 0 && (
                            <div className="msg-user-attachments">
                              {msg.attachments.map((att) =>
                                att.kind === "image" && (att.previewUrl || att.data) ? (
                                  // eslint-disable-next-line @next/next/no-img-element
                                  <img
                                    key={att.id}
                                    className="msg-user-img"
                                    src={
                                      att.previewUrl ||
                                      `data:${att.mimeType};base64,${att.data}`
                                    }
                                    alt={att.name}
                                  />
                                ) : (
                                  <span key={att.id} className="msg-user-file">
                                    📄 {att.name}
                                  </span>
                                )
                              )}
                            </div>
                          )}
                          {msg.content}
                        </div>
                        <div className="msgicon">You</div>
                      </div>
                    );
                  }

                  return (
                    <div key={msg.id} className="msg ai">
                      <div className="msgicon">CQ</div>
                      <div className="bubble">
                        <div className="ai-name">ChatQivo</div>

                        {showTyping ? (
                          <div className="typing-indicator" aria-live="polite">
                            <span className="typing-dot" />
                            <span className="typing-dot" />
                            <span className="typing-dot" />
                            <span style={{ marginLeft: 4 }}>Thinking…</span>
                          </div>
                        ) : (
                          <MarkdownRenderer content={msg.content} />
                        )}

                        {!(isLastModel && isStreaming) && msg.content.trim().length > 0 && (
                          <div className="msg-actions">
                            <button
                              type="button"
                              onClick={() => void handleCopyMessage(msg.id, msg.content)}
                            >
                              {copiedMsgId === msg.id ? "✓ Copied" : "Copy"}
                            </button>
                            <button
                              type="button"
                              onClick={() => void handleShare(msg.content)}
                            >
                              Share
                            </button>
                            <button
                              type="button"
                              onClick={() => handleSpeakToggle(msg.id, msg.content)}
                            >
                              {speakingId === msg.id ? "Stop audio" : "Listen"}
                            </button>
                            {isLastModel && (
                              <button type="button" onClick={() => void handleRegenerate()}>
                                Regenerate
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}

                {errorMsg && (
                  <div className="error-banner" role="alert">
                    <span>{errorMsg}</span>
                    <div className="error-banner-actions">
                      <button type="button" onClick={() => void handleRegenerate()}>
                        Retry
                      </button>
                      <button type="button" onClick={() => setErrorMsg(null)}>
                        Dismiss
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="chat-compose-area">
              {isStreaming && (
                <div className="stop-bar">
                  <button
                    type="button"
                    className="stop-pill"
                    onClick={stopGeneration}
                  >
                    ■ Stop generating
                  </button>
                </div>
              )}

              {renderPromptBox(true)}
              <div className="hint">
                ChatQivo can make mistakes. Check important information.
              </div>
            </div>
          </section>
        )}
      </main>

      {settingsOpen && (
        <div className="modal-overlay" onClick={() => setSettingsOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>ChatQivo Settings</h3>
              <button
                type="button"
                className="modal-close"
                aria-label="Close settings"
                onClick={() => setSettingsOpen(false)}
              >
                ✕
              </button>
            </div>

            <div className="modal-field">
              <label htmlFor="responseStyleSelect">Response Style</label>
              <select
                id="responseStyleSelect"
                value={settings.responseStyle}
                onChange={(e) =>
                  setSettings((prev) => ({
                    ...prev,
                    responseStyle: e.target.value as ResponseStyle,
                  }))
                }
              >
                <option value="balanced">Balanced (Clear & direct)</option>
                <option value="concise">Concise (Short & scannable)</option>
                <option value="detailed">Detailed (Thorough explanations)</option>
              </select>
            </div>

            <div className="modal-field">
              <label style={{ display: "flex", alignItems: "center", gap: 8, cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={settings.autoScroll}
                  onChange={(e) =>
                    setSettings((prev) => ({ ...prev, autoScroll: e.target.checked }))
                  }
                />
                Auto-scroll to new messages while streaming
              </label>
            </div>

            <div style={{ fontSize: 12, color: "var(--muted)", lineHeight: 1.5 }}>
              Saved conversations in browser storage: <strong>{conversations.length}</strong>.
              API requests are securely routed through <code>POST /api/chat</code>.
            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="btn-secondary"
                disabled={conversations.length === 0}
                onClick={handleExportHistory}
              >
                Export JSON
              </button>
              <button
                type="button"
                className="btn-danger"
                disabled={conversations.length === 0}
                onClick={() => {
                  stopGeneration();
                  setConversations([]);
                  setActiveId(null);
                  setViewMode("home");
                  setSettingsOpen(false);
                }}
              >
                Clear all chats
              </button>
              <button
                type="button"
                className="btn-primary"
                onClick={() => setSettingsOpen(false)}
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
