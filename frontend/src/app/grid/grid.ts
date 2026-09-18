import { Component, OnInit } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridApi, IServerSideDatasource, RowSelectionOptions, SetFilterValuesFuncParams, themeQuartz, ValueFormatterParams } from 'ag-grid-community';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { GridAiService } from './grid-ai.service';
import { SolrGridService } from './solr-grid.service';
import { CustomButtonCellRendererComponent } from './actions/custom-button-cell-renderer.component';
import type { ChatMessageDto, ScreeningChatResponse } from './grid-ai.model';

@Component({
  selector: 'app-grid',
  standalone: true,
  imports: [AgGridAngular, FormsModule, CustomButtonCellRendererComponent],
  templateUrl: './grid.html',
  styleUrl: './grid.scss',
  host: { 'style': 'height: 100%; display: block;' }
})
export class GridComponent implements OnInit {

  gridApi!: GridApi;
  query = '';
  portfolioManagerId = 'pm-001';
  chatInput = '';
  chatLoading = false;
  chatMessages: ChatMessageDto[] = [];
  theme = themeQuartz;
  rowSelection: RowSelectionOptions = {
    mode: 'multiRow'
  };

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
    {
      field: 'marginRate',
      headerName: 'Margin Rate',
      minWidth: 130,
      filter: 'agNumberColumnFilter',
      valueFormatter: (params: ValueFormatterParams) =>
        typeof params.value === 'number' ? `${params.value.toFixed(2)}%` : String(params.value ?? '')
    },
    { field: 'sanctionsFlag', headerName: 'Sanctions', minWidth: 120, filter: 'agTextColumnFilter' },
    { field: 'screeningStatus', headerName: 'Screening Status', minWidth: 170, filter: 'agTextColumnFilter' },
    { field: 'lastReviewDate', headerName: 'Last Review', minWidth: 140, filter: 'agDateColumnFilter' },
    {
      colId: 'holdingsActions',
      headerName: 'Actions',
      pinned: 'right',
      width: 150,
      sortable: false,
      filter: false,
      suppressHeaderMenuButton: true,
      cellRenderer: CustomButtonCellRendererComponent
    },
    {
      colId: 'aiBetterMargin',
      headerName: 'AI',
      minWidth: 220,
      sortable: false,
      filter: false,
      suppressHeaderMenuButton: true,
      valueGetter: () => 'Find Similar (Better Margin)',
      cellRenderer: () => '<button class="ai-row-btn">Find Similar (Better Margin)</button>',
      onCellClicked: (params) => this.findSimilarBetterMargin(params?.data)
    }
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
    this.loadChatHistory();
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
        hide: !visible
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

  sendScreeningChat(): void {
     const message = this.chatInput.trim();
     if (!message || this.chatLoading) {
       return;
     }

     this.chatLoading = true;
      this.gridAiService.screeningChat(
       this.portfolioManagerId,
       message,
       this.gridApi?.getState?.() ?? {},
       this.getStructuredSchemaSafe(),
       this.gridApi?.getSelectedRows?.() ?? []
      ).subscribe({
        next: (response: ScreeningChatResponse) => {
         console.log('Screening chat response:', response);
         console.log('gridUpdate:', response?.gridUpdate);

          this.chatMessages = response?.history ?? [];
         this.chatInput = '';

         // Apply GridAiResponse to update grid if available
         if (response?.gridUpdate) {
           console.log('Applying grid update:', response.gridUpdate);
           this.applyResult(response.gridUpdate);
           this.toastr.success('Grid updated based on AI screening', 'Grid Updated');
         } else {
           console.warn('No gridUpdate in response');
         }
       },
       error: (err) => {
         console.error('Chat error:', err);
         this.toastr.error('Failed to send screening chat message', 'Chat error');
       },
       complete: () => { this.chatLoading = false; }
     });
   }

  clearChatHistory(): void {
    this.gridAiService.clearScreeningChatHistory(this.portfolioManagerId).subscribe({
      next: () => { this.chatMessages = []; },
      error: () => this.toastr.error('Failed to clear chat history', 'Chat error')
    });
  }

  private loadChatHistory(): void {
    this.gridAiService.getScreeningChatHistory(this.portfolioManagerId).subscribe({
      next: (response) => {
        this.chatMessages = response?.history ?? [];
      },
      error: () => {
        this.chatMessages = [];
      }
    });
  }

  private findSimilarBetterMargin(rowData: any): void {
    if (!rowData) {
      this.toastr.warning('No row selected for AI action', 'No data');
      return;
    }

    const currentMargin = Number(rowData.marginRate);
    const marginTarget = Number.isFinite(currentMargin) ? Math.max(0, currentMargin - 0.15) : null;

    const query = `Find securities similar to this one but with a better (lower) margin rate.
Current security: ${JSON.stringify(rowData)}.
Requirements:
- Keep assetClass, country, currency, rating, and issuer profile as similar as possible
- Margin rate must be lower than current${marginTarget !== null ? ` and ideally <= ${marginTarget.toFixed(2)}` : ''}
- Prefer lower riskScore if possible
- Return AG Grid state JSON only.`;

    this.gridAiService.askAI(query, this.gridApi.getState(), this.getStructuredSchemaSafe()).subscribe({
      next: (result) => this.applyResult(result),
      error: () => this.toastr.error('Failed to run AI better margin action', 'Error')
    });
  }
}
