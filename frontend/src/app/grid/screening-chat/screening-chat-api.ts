import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { ScreeningChatHistoryResponse, ScreeningChatResponse } from './models';

@Injectable({
  providedIn: 'root'
})
export class ScreeningChatApi {
  private readonly apiBase = 'http://localhost:8081/api/ai';

  constructor(private http: HttpClient) {}

  public getSessions(portfolioManagerId?: string): Observable<{ chatSessionId: string }[]> {
    const params = portfolioManagerId
      ? new HttpParams().set('portfolioManagerId', portfolioManagerId)
      : undefined;

    return this.http.get<{ chatSessionId: string }[]>(
      `${this.apiBase}/screening-chat/sessions`,
      params ? { params } : undefined
    );
  }

  public createSession(portfolioManagerId?: string): Observable<{ chatSessionId: string }> {
    const body = portfolioManagerId ? { portfolioManagerId } : {};
    return this.http.post<{ chatSessionId: string }>(`${this.apiBase}/screening-chat/sessions`, body);
  }

  public screeningChat(
    portfolioManagerId: string,
    chatSessionId: string,
    message: string,
    gridState: any,
    structuredSchema: any,
    selectedRows: Record<string, unknown>[]
  ): Observable<ScreeningChatResponse> {
    return this.http.post<ScreeningChatResponse>(
      `${this.apiBase}/screening-chat/sessions/${encodeURIComponent(chatSessionId)}/chat`,
      {
        portfolioManagerId,
        chatSessionId,
        message,
        gridState,
        structuredSchema,
        selectedRows
      }
    );
  }

  public getScreeningChatHistory(portfolioManagerId: string, chatSessionId?: string): Observable<ScreeningChatHistoryResponse> {
    const sessionId = chatSessionId ?? 'default';
    const params = new HttpParams().set('portfolioManagerId', portfolioManagerId);
    return this.http.get<ScreeningChatHistoryResponse>(
      `${this.apiBase}/screening-chat/sessions/${encodeURIComponent(sessionId)}/messages`,
      { params }
    );
  }

  public clearScreeningChatHistory(portfolioManagerId: string, chatSessionId?: string): Observable<void> {
    const sessionId = chatSessionId ?? 'default';
    const params = new HttpParams().set('portfolioManagerId', portfolioManagerId);
    return this.http.delete<void>(
      `${this.apiBase}/screening-chat/sessions/${encodeURIComponent(sessionId)}`,
      { params }
    );
  }
}
