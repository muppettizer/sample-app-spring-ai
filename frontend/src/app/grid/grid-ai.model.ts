export interface ScreeningChatMessage {
  role: 'user' | 'assistant' | 'system';
  message: string;
}

export interface GridAiResponse {
  filter?: Record<string, unknown> | null;
  sort?: Record<string, string>[] | null;
  columnVisibility?: Record<string, boolean> | null;
  columnSizing?: Record<string, number> | null;
}

export interface ScreeningChatHistoryResponse {
  portfolioManagerId: string;
  messages: ScreeningChatMessage[];
}

export interface ScreeningChatResponse {
  // backend always includes a portfolioManagerId (normalized by the server)
  portfolioManagerId: string;
  // assistantMessage may be null when not provided
  assistantMessage: string | null;
  // history is always returned as a List<ChatMessageDto> from the backend for chat POST responses
  history: ScreeningChatMessage[];
  // gridUpdate can be null when no structured response is present
  gridUpdate: GridAiResponse | null;
}
