export type MessageRole = "user" | "model";

export interface ChatAttachment {
  id: string;
  name: string;
  mimeType: string;
  kind: "image" | "text";
  data: string; // base64 (without data: prefix) for images, or UTF-8 text content for code/text files
  previewUrl?: string;
}

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  attachments?: ChatAttachment[];
  createdAt: number;
  isError?: boolean;
}

export interface Conversation {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: number;
  updatedAt: number;
}

export type ResponseStyle = "balanced" | "concise" | "detailed";

export interface ChatSettings {
  responseStyle: ResponseStyle;
  autoScroll: boolean;
}
