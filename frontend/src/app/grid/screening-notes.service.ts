import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ScreeningNotesResponse {
  portfolioManagerId: string;
  notes: Record<string, Record<string, string>>;
}

@Injectable({
  providedIn: 'root'
})
export class ScreeningNotesService {
  private readonly apiBase = 'http://localhost:8081/api/ai';

  constructor(private http: HttpClient) {}

  public getScreeningNotes(portfolioManagerId: string): Observable<ScreeningNotesResponse> {
    return this.http.get<ScreeningNotesResponse>(
      `${this.apiBase}/screening-notes/${encodeURIComponent(portfolioManagerId)}`
    );
  }

  public saveScreeningNotes(
    portfolioManagerId: string,
    notes: Record<string, Record<string, string>>
  ): Observable<ScreeningNotesResponse> {
    return this.http.post<ScreeningNotesResponse>(
      `${this.apiBase}/screening-notes/${encodeURIComponent(portfolioManagerId)}`,
      { portfolioManagerId, notes }
    );
  }
}
