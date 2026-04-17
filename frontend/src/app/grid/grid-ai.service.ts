import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GridAiService {
  public readonly loading = signal(false);

  constructor(private http: HttpClient) {}

  public askAI(userQuery: string, gridState: any, structuredSchema: any): Observable<any> {
    this.loading.set(true);
    return this.http.post('http://localhost:8081/api/ai/grid-query', {
      userQuery,
      gridState,
      structuredSchema
    }).pipe(
      finalize(() => this.loading.set(false))
    );
  }
}
