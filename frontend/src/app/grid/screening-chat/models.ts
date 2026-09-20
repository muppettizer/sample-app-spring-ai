export interface ScreeningChatMessage {
  role: 'user' | 'assistant' | 'system';
  message: string;
}

export interface GridUpdate {
  filterModel?: Record<string, unknown> | null;
  sortModel?: Record<string, string>[] | null;
  columnVisibility?: Record<string, boolean> | null;
  columnSizingModel?: Record<string, number> | null;
}

export interface ScreeningChatHistoryResponse {
  portfolioManagerId: string;
  chatSessionId?: string;
  messages: ScreeningChatMessage[];
}

export interface ScreeningChatResponse {
  portfolioManagerId: string;
  chatSessionId?: string;
  assistantMessage: string | null;
  history: ScreeningChatMessage[];
  gridUpdate: GridUpdate | null;
}
