import {Component, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AgGridAngular} from 'ag-grid-angular';
import {
  ColDef, FilterModel,
  GridApi,
  IRowNode,
  IServerSideDatasource,
  Note,
  NotesDataSource,
  RowSelectedEvent,
  RowSelectionOptions,
  SetFilterValuesFuncParams,
  themeQuartz,
  ValueFormatterParams
} from 'ag-grid-community';
import {FormsModule} from '@angular/forms';
import {ToastrService} from 'ngx-toastr';
import {GridAiService} from './grid-ai.service';
import {ScreeningNotesService} from './screening-notes.service';
import {SolrGridService} from './solr-grid.service';
import {CustomButtonCellRendererComponent} from './actions/custom-button-cell-renderer.component';
import {GridUpdate, ScreeningChatMessage, ScreeningChatResponse} from './grid-ai.model';
import {finalize} from 'rxjs/operators';

@Component({
  selector: 'app-grid',
  standalone: true,
  imports: [CommonModule, AgGridAngular, FormsModule, CustomButtonCellRendererComponent],
  templateUrl: './grid.html',
  styleUrl: './grid.scss',
  host: { 'style': 'height: 100%; display: block;' }
})
export class GridComponent implements OnInit {

  gridApi!: GridApi;
  query = '';
  portfolioManagerId = 'pm-001';
  chatInput = '';
  chatLoading = signal(false);
  chatMessages: ScreeningChatMessage[] = [];
  theme = themeQuartz;

  rowSelection: RowSelectionOptions = {
    mode: 'singleRow'
  };

  private readonly noteState = new Map<string, Record<string, Note | undefined>>();

  readonly maxSelected = 1;
  readonly selectedRowCount = signal(0);

  private toApiNotesMap(): Record<string, Record<string, string>> {
    return Object.fromEntries(
      Array.from(this.noteState.entries()).map(([rowKey, notes]) => [
        rowKey,
        Object.fromEntries(
          Object.entries(notes).filter(([, note]) => !!note).map(([columnKey, note]) => [columnKey, note?.text ?? ''])
        )
      ])
    );
  }

  hasSelectedRow(): boolean {
    return this.selectedRowCount() > 0;
  }

  onRowSelected(event: RowSelectedEvent): void {
    const selectedCount = event.api.getSelectedRows().length;
    this.selectedRowCount.set(selectedCount);

    if (selectedCount > this.maxSelected) {
      event.node.setSelected(false);
      this.selectedRowCount.set(event.api.getSelectedRows().length);
    }
  }

  gridOptions: any = {
    rowModelType: 'serverSide',
    notesDataSource: this.createNotesDataSource()
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
    private screeningNotesService: ScreeningNotesService,
    private solrGridService: SolrGridService
  ) {}

  private getRowKey(rowNode: IRowNode): string {
    const rowData = rowNode?.data as Record<string, unknown> | undefined;
    const securityId = rowData?.['securityId'];
    return String(securityId ?? rowNode?.id ?? 'unknown-row');
  }

  private createNotesDataSource(): NotesDataSource {
    return {
      getNote: ({ rowNode, column }) => {
        const rowKey = this.getRowKey(rowNode);
        const columnKey = String(column.getColId());
        const rowNotes = this.noteState.get(rowKey);
        return rowNotes?.[columnKey];
      },
      setNote: ({ rowNode, column, note }) => {
        const rowKey = this.getRowKey(rowNode);
        const columnKey = String(column.getColId());
        const rowNotes = this.noteState.get(rowKey) ?? {};

        if (note) {
          rowNotes[columnKey] = note;
          this.noteState.set(rowKey, rowNotes);
        } else {
          delete rowNotes[columnKey];
          if (Object.keys(rowNotes).length === 0) {
            this.noteState.delete(rowKey);
          } else {
            this.noteState.set(rowKey, rowNotes);
          }
        }

        this.screeningNotesService.saveScreeningNotes(this.portfolioManagerId, this.toApiNotesMap()).subscribe({
          error: () => this.toastr.error('Failed to persist screening notes', 'Notes error')
        });

        this.gridApi?.refreshNotes?.({
          rowNodes: [rowNode],
          columns: [column]
        });
      }
    };
  }

  ngOnInit() {
    console.log('Grid component initialized');
    this.loadChatHistory();
    this.loadScreeningNotes();
  }

  private loadScreeningNotes(): void {
    this.screeningNotesService.getScreeningNotes(this.portfolioManagerId).subscribe({
      next: (response) => {
        this.noteState.clear();
        Object.entries(response.notes ?? {}).forEach(([rowKey, columns]) => {
          const noteMap: Record<string, Note | undefined> = {};
          Object.entries(columns).forEach(([columnKey, text]) => {
            if (text && typeof text === 'string') {
              noteMap[columnKey] = { text };
            }
          });
          if (Object.keys(noteMap).length > 0) {
            this.noteState.set(rowKey, noteMap);
          }
        });
      },
      error: () => {
        this.noteState.clear();
      }
    });
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

  applyResult(result: GridUpdate) {
    if (!result || typeof result !== 'object') {
      this.toastr.error('AI response is not valid JSON object', 'Invalid response');
      return;
    }

    console.log(
      'Applying state:',
      JSON.stringify(result, null, 2)
    );

    const filterModel = result?.filterModel as FilterModel;
    this.gridApi.setFilterModel(filterModel);
    // const sortModel = result?.sortModel;
    // const hiddenColIds = result?.columnVisibility as string[];
    // const columnSizingModel = result?.columnSizingModel as ColumnSizingModel[];

    // console.log(
    //   'Applying filter:',
    //   JSON.stringify(filterModel, null, 2)
    // );

    // console.log(
    //   'Applying sort:',
    //   JSON.stringify(sortModel, null, 2)
    // );

    // console.log(
    //   'Applying hiddenColIds:',
    //   JSON.stringify(hiddenColIds, null, 2)
    // );

    // console.log(
    //   'Applying columnSizingModel:',
    //   JSON.stringify(columnSizingModel, null, 2)
    // );

    // this.gridApi.setFilterModel(filterModel);

    // console.log(
    //   'Grid filter model:',
    //   JSON.stringify(this.gridApi.getFilterModel(), null, 2)
    // );

    // this.gridApi.applyColumnState

    //
    // if (Array.isArray(result.sort) && result.sort.length > 0) {
    //   this.gridApi.applyColumnState({
    //     state: result.sort.map((entry: any) => ({
    //       colId: entry?.colId,
    //       sort: entry?.sort
    //     })).filter((entry: any) => !!entry.colId && !!entry.sort),
    //     defaultState: { sort: null }
    //   });
    // }
    //
    // if (result.columnVisibility && typeof result.columnVisibility === 'object') {
    //   const visibilityState = Object.entries(result.columnVisibility).map(([colId, visible]) => ({
    //     colId,
    //     hide: !visible
    //   }));
    //   this.gridApi.applyColumnState({ state: visibilityState });
    // }
    //
    // if (result.columnSizing && typeof result.columnSizing === 'object') {
    //   const sizingState = Object.entries(result.columnSizing).map(([colId, width]) => ({
    //     colId,
    //     width: Number(width)
    //   })).filter(entry => Number.isFinite(entry.width));
    //   this.gridApi.applyColumnState({ state: sizingState });
    // }
    //
    // // Important with Server-Side Row Model
    // this.gridApi.refreshServerSide({purge: true});
    //
    // this.toastr.success('AI screening instructions applied', 'Done');
  }

  runPreset(preset: string): void {
    if (!this.hasSelectedRow()) {
      this.toastr.warning('Select a row before running a preset', 'Selection required');
      return;
    }

    this.chatInput = preset;
    this.sendScreeningChat();
  }

  sendScreeningChat(): void {
    const message = this.chatInput.trim();

    if (!message || this.chatLoading()) {
      return;
    }

    this.chatLoading.set(true);

    this.gridAiService.screeningChat(
      this.portfolioManagerId,
      message,
      this.gridApi?.getState?.() ?? {},
      this.getStructuredSchemaSafe(),
      this.gridApi?.getSelectedRows?.() ?? []
    )
    .pipe(
      finalize(() => this.chatLoading.set(false))
    )
    .subscribe({
      next: (response: ScreeningChatResponse) => {
        console.log('Screening chat response:', response);
        console.log('explanation:', response?.assistantMessage);
        console.log('gridUpdate:', response?.gridUpdate);

        this.chatMessages = response?.history ?? [];

        if (response?.gridUpdate) {
          console.log('Applying grid update:', response.gridUpdate);
          this.applyResult(response.gridUpdate);
          this.toastr.success(
            'Grid updated based on AI screening',
            'Grid Updated'
          );
        } else {
          console.warn('No gridUpdate in response');
        }
      },
      error: (err) => {
        console.error('Chat error:', err);
        this.toastr.error(
          'Failed to send screening chat message',
          'Chat error'
        );
      }
    });
  }

  clearGridFilters(): void {
    this.gridApi.setFilterModel(null);
  }

  clearChatHistory(): void {
    this.gridAiService.clearScreeningChatHistory(this.portfolioManagerId).subscribe({
      next: () => { this.chatMessages = []; },
      error: () => this.toastr.error('Failed to clear chat messages', 'Chat error')
    });
  }

  private loadChatHistory(): void {
    this.gridAiService.getScreeningChatHistory(this.portfolioManagerId).subscribe({
      next: (response) => {
        this.chatMessages = response?.messages ?? [];
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
