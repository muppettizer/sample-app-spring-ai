import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, map, Observable, throwError} from 'rxjs';
import {FilterModel, IServerSideGetRowsParams} from 'ag-grid-community';

@Injectable({providedIn: 'root'})
export class SolrGridService {
    private http = inject(HttpClient);
    private solrEndpoint = 'http://localhost:8983/solr/security-screener';

    private buildSolrQuery(
        params: IServerSideGetRowsParams
    ): string {
        console.log("[SolrGridService] Building Solr query");
        this.logParams(params);

        const start = params.request.startRow ?? 0;
        const end = params.request.endRow ?? 100;
        const rows = end - start;
        const sortModel = params.request.sortModel ?? [];
        const sort = sortModel.length
            ? `${sortModel[0].colId} ${sortModel[0].sort}`
            : '';

        const filterModel = params.request.filterModel ?? {};
        console.log("[SolrGridService] Filter model:", JSON.stringify(filterModel, null, 2));
        const filterQueries = this.mapFilterModelToSolr(filterModel);

        const query = `*:*`;

        const solrParams = new URLSearchParams({
            q: query,
            start: start.toString(),
            rows: rows.toString(),
            wt: 'json',
            ...(sort && {sort}),
        });

        filterQueries.forEach(fq => solrParams.append('fq', fq));

      console.log("[SolrGridService] Solr query parameters:", solrParams.toString());

      return `${this.solrEndpoint}/select?${solrParams.toString()}`;
    }

    private mapFilterModelToSolr(filterModel: FilterModel): string[] {
      console.log("[SolrGridService] Mapping filter model to Solr filter queries:", JSON.stringify(filterModel, null, 2));
        const fqs: string[] = [];

        for (const [colId, filter] of Object.entries(filterModel ?? {})) {
            if (!filter || typeof filter !== 'object') {
                continue;
            }

            if (filter.filterType === 'set') {
                const setFilter = this.mapSetFilter(colId, filter);
                if (setFilter) {
                    fqs.push(setFilter);
                }
            } else if (filter.filterType === 'text') {
                const textFilter = this.mapTextFilter(colId, filter);
                if (textFilter) {
                    fqs.push(textFilter);
                }
            } else if (filter.filterType === 'number') {
                const numberFilter = this.mapNumberFilter(colId, filter);
                if (numberFilter) {
                    fqs.push(numberFilter);
                }
            }
        }
        console.log("[SolrGridService] Mapped Solr filter queries:", JSON.stringify(fqs, null, 2));
        return fqs;
    }

    private mapTextFilter(colId: string, filter: any): string {
        const value = filter?.filter ?? '';
        if (value === '' || value === null || value === undefined) {
            return '';
        }

        switch (filter.type) {
            case 'contains': return `${colId}:*${value}*`;
            case 'equals': return `${colId}:"${this.escapeSolrValue(String(value))}"`;
            case 'startsWith': return `${colId}:${value}*`;
            case 'endsWith': return `${colId}:*${value}`;
            default: return `${colId}:*${value}*`;
        }
    }

    private mapNumberFilter(colId: string, filter: any): string {
        const value = filter?.filter;
        if (value === undefined || value === null || value === '') {
            return '';
        }

        switch (filter.type) {
            case 'equals':
                return `${colId}:${value}`;

            case 'greaterThan':
                return `${colId}:{${value} TO *}`;

            case 'greaterThanOrEqual':
                return `${colId}:[${value} TO *]`;

            case 'lessThan':
                return `${colId}:{* TO ${value}}`;

            case 'lessThanOrEqual':
                return `${colId}:[* TO ${value}]`;

            case 'inRange':
                return `${colId}:[${filter.filter} TO ${filter.filterTo}]`;

            default:
                return `${colId}:${value}`;
        }
    }

    private mapSetFilter(colId: string, filter: any): string {
        const values = Array.isArray(filter?.values)
            ? filter.values.filter((value: unknown) => value !== null && value !== undefined && String(value).trim() !== '')
            : [];

        if (!values.length) {
            return '';
        }

        const quotedValues = values.map((value: unknown) => `"${this.escapeSolrValue(String(value))}"`);
        if (quotedValues.length === 1) {
            return `${colId}:${quotedValues[0]}`;
        }

        return `${colId}:(${quotedValues.join(' OR ')})`;
    }

    private escapeSolrValue(value: string): string {
        return value.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
    }

    private logParams(params: IServerSideGetRowsParams): void {
        const {
            startRow,
            endRow,
            sortModel,
            filterModel
        } = params.request;

      console.log(`Server-Side Row Params:\n`, JSON.stringify(filterModel, null, 2));

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
