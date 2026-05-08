import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, map, Observable, throwError} from 'rxjs';
import {IServerSideGetRowsParams} from 'ag-grid-community';

@Injectable({providedIn: 'root'})
export class SolrGridService {
    private http = inject(HttpClient);
    private solrEndpoint = 'http://localhost:8983/solr/cars';

    private buildSolrQuery(
        params: IServerSideGetRowsParams
    ): string {
        const start = params.request.startRow ?? 0;
        const end = params.request.endRow ?? 100;
        const rows = end - start;
        const sortModel = params.request.sortModel ?? [];
        const sort = sortModel.length
            ? `${sortModel[0].colId} ${sortModel[0].sort}`
            : '';

        const filterModel = params.request.filterModel ?? {};
        const filterQueries = this.mapFilterModelToSolr(filterModel);

        console.log("[SolrGridService] Building Solr query");
        this.logParams(params);

        const query = `*:*`;

        const solrParams = new URLSearchParams({
            q: query,
            start: start.toString(),
            rows: rows.toString(),
            wt: 'json',
            ...(sort && {sort}),
        });

        filterQueries.forEach(fq => solrParams.append('fq', fq));

        return `${this.solrEndpoint}/select?${solrParams.toString()}`;
    }

    private mapFilterModelToSolr(filterModel: any): string[] {
        const fqs: string[] = [];
        for (const colId in filterModel) {
            const filter = filterModel[colId];
            if (filter.filterType === 'set') {
                fqs.push(this.mapSetFilter(colId, filter));
            } else if (filter.filterType === 'text') {
                fqs.push(this.mapTextFilter(colId, filter));
            } else if (filter.filterType === 'number') {
                fqs.push(this.mapNumberFilter(colId, filter));
            }
        }
        return fqs;
    }

    private mapTextFilter(colId: string, filter: any): string {
        switch (filter.type) {
            case 'contains': return `${colId}:*${filter.filter}*`;
            case 'equals': return `${colId}:"${filter.filter}"`;
            case 'startsWith': return `${colId}:${filter.filter}*`;
            case 'endsWith': return `${colId}:*${filter.filter}`;
            default: return `${colId}:*${filter.filter}*`;
        }
    }

    private mapNumberFilter(colId: string, filter: any): string {
        switch (filter.type) {
            case 'equals': return `${colId}:${filter.filter}`;
            case 'greaterThan': return `${colId}:[${filter.filter + 1} TO *]`;
            case 'lessThan': return `${colId}:[* TO ${filter.filter - 1}]`;
            case 'inRange': return `${colId}:[${filter.filter} TO ${filter.filterTo}]`;
            default: return `${colId}:${filter.filter}`;
        }
    }

    private mapSetFilter(colId: string, filter: any): string {
        const values = Array.isArray(filter?.values) ? filter.values : [];
        if (!values.length) {
            return '*:*';
        }

        const orValues = values
            .map((value: unknown) => `"${String(value).replace(/"/g, '\\"')}"`)
            .join(' OR ');
        return `${colId}:(${orValues})`;
    }

    private logParams(params: IServerSideGetRowsParams): void {
        const {
            startRow,
            endRow,
            sortModel,
            filterModel
        } = params.request;

        const logObject: any = {
            pagination: {startRow, endRow},
            sorting: sortModel?.map(s => ({column: s.colId, direction: s.sort})),
            filtering: filterModel
        };

        console.log(`Server-Side Row Request:\n`, JSON.stringify(logObject, null, 2));
    }

    fetchSolrRows(
        params: IServerSideGetRowsParams
    ): Observable<{ rows: any[]; lastRow: number }> {
        const url = this.buildSolrQuery(params);

        return this.http.get(url).pipe(
            map((res: any) => {
                return {
                    rows: res.response.docs,
                    lastRow: res.response.numFound,
                };
            }),
            catchError(error => {
                console.error(`[SolrGridService] HTTP error`, error);
                return throwError(() => error);
            })
        );
    }

    fetchFacetValues(field: string): Observable<string[]> {
        const solrParams = new URLSearchParams({
            q: '*:*',
            rows: '0',
            wt: 'json',
            facet: 'true',
            'facet.limit': '-1',
            'facet.sort': 'index',
            'facet.mincount': '1',
            'facet.field': field
        });

        const url = `${this.solrEndpoint}/select?${solrParams.toString()}`;
        return this.http.get(url).pipe(
            map((res: any) => {
                const rawValues = res?.facet_counts?.facet_fields?.[field];
                if (!Array.isArray(rawValues)) {
                    return [];
                }

                const values: string[] = [];
                for (let i = 0; i < rawValues.length; i += 2) {
                    const value = rawValues[i];
                    if (typeof value === 'string' && value.trim().length > 0) {
                        values.push(value);
                    }
                }
                return values;
            }),
            catchError(error => {
                console.error(`[SolrGridService] Facet query error for ${field}`, error);
                return throwError(() => error);
            })
        );
    }
}
