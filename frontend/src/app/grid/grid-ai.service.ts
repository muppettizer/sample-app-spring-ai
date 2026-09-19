import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import type { ScreeningChatResponse, GridUpdate, ScreeningChatHistoryResponse } from './grid-ai.model';

@Injectable({
  providedIn: 'root'
})
export class GridAiService {
  public readonly loading = signal(false);
  private readonly apiBase = 'http://localhost:8081/api/ai';

  constructor(private http: HttpClient) {}

  public askAI(userQuery: string, gridState: any, structuredSchema: any): Observable<GridUpdate> {
    this.loading.set(true);
    return this.http.post(`${this.apiBase}/grid-query`, {
      userQuery,
      gridState,
      structuredSchema
    }).pipe(
      finalize(() => this.loading.set(false))
    );
  }

  public screeningChat(
    portfolioManagerId: string,
    message: string,
    gridState: any,
    structuredSchema: any,
    selectedRows: Record<string, unknown>[]
  ): Observable<ScreeningChatResponse> {
    return this.http.post<ScreeningChatResponse>(`${this.apiBase}/screening-chat`, {
      portfolioManagerId,
      message,
      gridState,
      structuredSchema,
      selectedRows
    });
  }

  public getScreeningChatHistory(portfolioManagerId: string): Observable<ScreeningChatHistoryResponse> {
    return this.http.get<ScreeningChatHistoryResponse>(`${this.apiBase}/screening-chat/${encodeURIComponent(portfolioManagerId)}/history`);
  }

  public clearScreeningChatHistory(portfolioManagerId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/screening-chat/${encodeURIComponent(portfolioManagerId)}/history`);
  }

  public portfolioHoldingsAssistant(instrumentId: string, rowSnapshot: Record<string, unknown>): Observable<any> {
    return this.http.post(`${this.apiBase}/portfolio-holdings-assistant`, {
      instrumentId,
      rowSnapshot
    });
  }
}
