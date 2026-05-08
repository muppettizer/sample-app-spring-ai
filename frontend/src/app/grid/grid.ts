import { Component, OnInit } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridApi, IServerSideDatasource, SetFilterValuesFuncParams, themeQuartz } from 'ag-grid-community';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { GridAiService } from './grid-ai.service';
import { SolrGridService } from './solr-grid.service';

@Component({
  selector: 'app-grid',
  standalone: true,
  imports: [AgGridAngular, FormsModule],
  templateUrl: './grid.html',
  styleUrl: './grid.scss',
  host: { 'style': 'height: 100%; display: block;' }
})
export class GridComponent implements OnInit {

  gridApi!: GridApi;
  query = '';
  theme = themeQuartz;

  gridOptions: any = {
    rowModelType: 'serverSide'
  };

  columnDefs: ColDef[] = [
    { field: 'securityId', headerName: 'Security ID', minWidth: 140, filter: 'agTextColumnFilter' },
    { field: 'securityName', headerName: 'Security Name', minWidth: 210, filter: 'agTextColumnFilter' },
    { field: 'issuer', headerName: 'Issuer', minWidth: 180, filter: 'agTextColumnFilter' },
    {
      field: 'assetClass',
      headerName: 'Asset Class',
      minWidth: 140,
      filter: 'agSetColumnFilter',
      filterParams: {
        values: (params: SetFilterValuesFuncParams) => this.loadFacetFilterValues(params, 'assetClass')
      }
    },
    {
      field: 'country',
      headerName: 'Country',
      minWidth: 120,
      filter: 'agSetColumnFilter',
      filterParams: {
        values: (params: SetFilterValuesFuncParams) => this.loadFacetFilterValues(params, 'country')
      }
    },
    {
      field: 'currency',
      headerName: 'CCY',
      minWidth: 100,
      maxWidth: 120,
      filter: 'agSetColumnFilter',
      filterParams: {
        values: (params: SetFilterValuesFuncParams) => this.loadFacetFilterValues(params, 'currency')
      }
    },
    { field: 'rating', headerName: 'Rating', minWidth: 110, filter: 'agTextColumnFilter' },
    { field: 'esgRiskLevel', headerName: 'ESG Risk', minWidth: 120, filter: 'agTextColumnFilter' },
    { field: 'riskScore', headerName: 'Risk Score', minWidth: 120, filter: 'agNumberColumnFilter' },
    { field: 'sanctionsFlag', headerName: 'Sanctions', minWidth: 120, filter: 'agTextColumnFilter' },
    { field: 'screeningStatus', headerName: 'Screening Status', minWidth: 170, filter: 'agTextColumnFilter' },
    { field: 'lastReviewDate', headerName: 'Last Review', minWidth: 140, filter: 'agDateColumnFilter' }
  ];

  defaultColDef: ColDef = {
    sortable: true,
    filter: true,
    resizable: true
  };

  constructor(
    private toastr: ToastrService,
    protected gridAiService: GridAiService,
    private solrGridService: SolrGridService
  ) {}

  ngOnInit() {
    console.log('Grid component initialized');
  }

  onGridReady(params: any) {
    console.log('Grid ready', params);
    this.gridApi = params.api;
    this.gridApi.setGridOption('serverSideDatasource', this.createServerSideDatasource());
  }

  getStructuredSchemaSafe() {
    if ((this.gridApi as any).getStructuredSchema) {
      return (this.gridApi as any).getStructuredSchema();
    }

    return {
      columns: this.columnDefs.map(c => ({
        name: c.field,
        type: 'string'
      }))
    };
  }

  private createServerSideDatasource(): IServerSideDatasource {
    return {
      getRows: (params) => {
        this.solrGridService.fetchSolrRows(params).subscribe({
          next: ({rows, lastRow}) => {
            params.success({
              rowData: rows,
              rowCount: lastRow
            });
          },
          error: () => {
            params.fail();
            this.toastr.error('Failed to load Solr rows', 'Error');
          }
        });
      }
    };
  }

  private loadFacetFilterValues(params: SetFilterValuesFuncParams, field: string): void {
    this.solrGridService.fetchFacetValues(field).subscribe({
      next: (values) => params.success(values),
      error: () => {
        this.toastr.error(`Failed to load ${field} filter values`, 'Facet error');
        params.success([]);
      }
    });
  }

  askAI() {
    if (!this.query.trim()) {
      this.toastr.warning('Type a screening instruction first', 'Prompt required');
      return;
    }

    this.gridAiService.askAI(this.query, this.gridApi.getState(), this.getStructuredSchemaSafe()).subscribe({
      next: (result) => {
        this.applyResult(result);
      },
      error: (error) => {
        this.toastr.error('Network error or server unavailable', 'Error');
      }
    });
  }

  applyResult(result: any) {
    if (!result || typeof result !== 'object') {
      this.toastr.error('AI response is not valid JSON object', 'Invalid response');
      return;
    }

    if (result.filter && typeof result.filter === 'object') {
      this.gridApi.setFilterModel(result.filter);
    }

    if (Array.isArray(result.sort) && result.sort.length > 0) {
      this.gridApi.applyColumnState({
        state: result.sort.map((entry: any) => ({
          colId: entry?.colId,
          sort: entry?.sort
        })).filter((entry: any) => !!entry.colId && !!entry.sort),
        defaultState: { sort: null }
      });
    }

    if (result.columnVisibility && typeof result.columnVisibility === 'object') {
      const visibilityState = Object.entries(result.columnVisibility).map(([colId, visible]) => ({
        colId,
        hide: !Boolean(visible)
      }));
      this.gridApi.applyColumnState({ state: visibilityState });
    }

    if (result.columnSizing && typeof result.columnSizing === 'object') {
      const sizingState = Object.entries(result.columnSizing).map(([colId, width]) => ({
        colId,
        width: Number(width)
      })).filter(entry => Number.isFinite(entry.width));
      this.gridApi.applyColumnState({ state: sizingState });
    }

    this.toastr.success('AI screening instructions applied', 'Done');
  }

  runPreset(preset: string) {
    this.query = preset;
    this.askAI();
  }
}
