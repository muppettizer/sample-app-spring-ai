import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import type { GridUpdate } from './grid-ai.model';

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

  public portfolioHoldingsAssistant(instrumentId: string, rowSnapshot: Record<string, unknown>): Observable<any> {
    return this.http.post(`${this.apiBase}/portfolio-holdings-assistant`, {
      instrumentId,
      rowSnapshot
    });
  }
}
