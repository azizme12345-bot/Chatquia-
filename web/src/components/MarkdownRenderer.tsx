"use client";

import React, { useState } from "react";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkGfm from "remark-gfm";
import { getTextDirection } from "@/lib/rtl";

function extractText(node: React.ReactNode): string {
  if (typeof node === "string" || typeof node === "number") return String(node);
  if (Array.isArray(node)) return node.map(extractText).join("");
  if (React.isValidElement<{ children?: React.ReactNode }>(node)) {
    return extractText(node.props.children);
  }
  return "";
}

function CodeBlock({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  const [copied, setCopied] = useState(false);
  const rawCode = extractText(children).replace(/\n$/, "");
  const langMatch = /language-([^\s]+)/.exec(className || "");
  const lang = langMatch?.[1] || "code";

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(rawCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      // Clipboard blocked
    }
  };

  return (
    <div className="code-card">
      <div className="code-card-header">
        <span>{lang}</span>
        <button type="button" className="copy-code-btn" onClick={handleCopy}>
          {copied ? "✓ Copied" : "Copy code"}
        </button>
      </div>
      <pre>
        <code className={className}>{children}</code>
      </pre>
    </div>
  );
}

export default function MarkdownRenderer({ content }: { content: string }) {
  return (
    <div className="md-body" dir="auto">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        components={{
          p({ children }) {
            const dir = getTextDirection(extractText(children));
            return <p dir={dir}>{children}</p>;
          },
          h1({ children }) {
            const dir = getTextDirection(extractText(children));
            return <h1 dir={dir}>{children}</h1>;
          },
          h2({ children }) {
            const dir = getTextDirection(extractText(children));
            return <h2 dir={dir}>{children}</h2>;
          },
          h3({ children }) {
            const dir = getTextDirection(extractText(children));
            return <h3 dir={dir}>{children}</h3>;
          },
          li({ children }) {
            const dir = getTextDirection(extractText(children));
            return <li dir={dir}>{children}</li>;
          },
          blockquote({ children }) {
            const dir = getTextDirection(extractText(children));
            return <blockquote dir={dir}>{children}</blockquote>;
          },
          a({ href, children }) {
            return (
              <a href={href} target="_blank" rel="noopener noreferrer">
                {children}
              </a>
            );
          },
          table({ children }) {
            return (
              <div className="md-table-wrap">
                <table>{children}</table>
              </div>
            );
          },
          pre({ children }) {
            const child = Array.isArray(children) ? children[0] : children;
            if (React.isValidElement<{ className?: string; children?: React.ReactNode }>(child)) {
              return (
                <CodeBlock className={child.props.className}>
                  {child.props.children}
                </CodeBlock>
              );
            }
            return <pre>{children}</pre>;
          },
          code({ className, children }) {
            const isBlock = Boolean(className?.includes("language-") || className?.includes("hljs"));
            if (isBlock) {
              return <code className={className}>{children}</code>;
            }
            return <code className="md-inline-code">{children}</code>;
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
