# ChatQivo — All-in-One AI Chat Website

Production-ready Next.js App Router website powered by the official `@google/genai` SDK and designed for deployment on Vercel.

## Environment Variables

Add these in `.env.local` for local development or in **Vercel Project Settings → Environment Variables**:

- `GEMINI_API_KEY` — Your Google Gemini API key (server-side only; never use `NEXT_PUBLIC_`).
- `GEMINI_MODEL` — Supported Gemini model ID (default: `gemini-2.5-flash`).

## Deploy to Vercel

1. Import this repository into Vercel.
2. Set **Root Directory** to `web`.
3. Add `GEMINI_API_KEY` and `GEMINI_MODEL` in **Settings → Environment Variables**.
4. Deploy.
