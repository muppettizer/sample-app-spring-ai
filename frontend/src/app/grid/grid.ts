import { Component, OnInit } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import {ColDef, GridApi, themeQuartz, IServerSideDatasource} from 'ag-grid-community';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { SolrGridService } from './solr-grid.service';
import { GridAiService } from './grid-ai.service';
import {GridActionsCellRendererComponent} from './actions/grid-actions-cell-renderer.component';
import {PromptsCellRendererComponent} from './actions/prompts-cell-renderer.component';

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
  // theme = themeAlpine;
  theme = themeQuartz;

  gridOptions: any = {
    rowModelType: 'serverSide'
  };

  columnDefs: ColDef[] = [
    { field: 'make' },
    { field: 'model' },
    { field: 'price', filter: 'agNumberColumnFilter' },
    { field: 'country' },
    {
      headerName: 'Prompts',
      field: 'prompts',
      sortable: false,
      filter: false,
      suppressHeaderMenuButton: true,
      resizable: false,
      width: 160,
      cellRenderer: PromptsCellRendererComponent,
    },
    {
      headerName: 'Actions',
      field: 'actions',
      sortable: false,
      filter: false,
      suppressHeaderMenuButton: true,
      resizable: false,
      width: 140,
      // pinned: 'right',           // keeps it always visible when scrolling
      cellRenderer: GridActionsCellRendererComponent,
      // cellStyle: { padding: '0 4px' }
    }
  ];

  constructor(private toastr: ToastrService, private solrGridService: SolrGridService, protected gridAiService: GridAiService) {}

  ngOnInit() {
    console.log('Grid component initialized');
  }

  onGridReady(params: any) {
    console.log('Grid ready', params);
    this.gridApi = params.api;

    const datasource = this.createServerSideDatasource();
    this.gridApi.setGridOption("serverSideDatasource", datasource);
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
          error: (error) => {
            params.fail();
            this.toastr.error('Failed to load data from Solr', 'Error');
          }
        });
      }
    };
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

  askAI() {
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
    // This might not be needed anymore, but keeping for compatibility
    switch (result.action) {

      case 'filter':
        this.gridApi.setFilterModel({
          [result.payload.column]: {
            type: 'equals',
            filter: result.payload.value
          }
        });
        break;

      case 'sort':
        this.gridApi.applyColumnState({
          state: [{
            colId: result.payload.column,
            sort: result.payload.direction
          }]
        });
        break;

      default:
        console.warn('Unknown action', result);
    }
  }
}
