const RTL_REGEX = /[\u0590-\u05FF\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/;

export function hasRtlCharacters(text: string): boolean {
  return RTL_REGEX.test(text);
}

export function getTextDirection(text: string): "rtl" | "ltr" | "auto" {
  if (!text) return "ltr";
  // Check the first strong directional character
  for (const char of text) {
    if (RTL_REGEX.test(char)) return "rtl";
    if (/[A-Za-z]/.test(char)) return "ltr";
  }
  return hasRtlCharacters(text) ? "rtl" : "ltr";
}

export function detectSpeechLang(text: string): string {
  if (/[\u0679\u067E\u0686\u0688\u0691\u0698\u06BA\u06BE\u06C1\u06D2]/.test(text)) {
    return "ur-PK";
  }
  if (/[\u0600-\u06FF]/.test(text)) {
    return "ar-SA";
  }
  if (/[\u0900-\u097F]/.test(text)) {
    return "hi-IN";
  }
  return "en-US";
}
