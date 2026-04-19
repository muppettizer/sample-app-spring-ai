import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GridDataService {
  private solrUrl = 'http://localhost:8983/solr/cars/select';

  constructor(private http: HttpClient) {}

  getData(request: any): Observable<any> {
    let params = new HttpParams()
      .set('q', '*:*')
      .set('wt', 'json')
      .set('rows', (request.endRow - request.startRow).toString())
      .set('start', request.startRow.toString());

    // Handle filters
    if (request.filterModel) {
      for (const [key, filter] of Object.entries(request.filterModel)) {
        if ((filter as any).type === 'equals') {
          params = params.append('fq', `${key}:${(filter as any).filter}`);
        }
        // Add more filter types as needed
      }
    }

    // Handle sorting
    if (request.sortModel && request.sortModel.length > 0) {
      const sortStr = request.sortModel.map((s: any) => `${s.colId} ${s.sort}`).join(',');
      params = params.set('sort', sortStr);
    }

    return this.http.get(this.solrUrl, { params });
  }
}
