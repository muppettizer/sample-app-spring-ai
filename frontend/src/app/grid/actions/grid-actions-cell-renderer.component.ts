import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import {ICellRendererParams, StructuredSchemaParams} from 'ag-grid-community';
import { GridRowAction } from './grid-row-action.model';
import {GridAiService} from '../grid-ai.service';

@Component({
  selector: 'app-grid-actions-cell-renderer',
  standalone: true,
  templateUrl: './grid-actions-cell-renderer.component.html',
  styleUrl: './grid-actions-cell-renderer.component.scss'
})
export class GridActionsCellRendererComponent implements ICellRendererAngularComp {
  private params!: ICellRendererParams;

  loadingAction: string | null = null;

  readonly actions: GridRowAction[] = [
    {
      id: 'find-similar',
      label: 'Find Similar',
      icon: '🔍',
      userQuery: (row: any) => {
        const safeRow = Object.fromEntries(
          Object.entries(row).filter(([, v]) => v !== null && typeof v !== 'object' && typeof v !== 'function')
        );
        return `Find rows similar to this one: ${JSON.stringify(safeRow)}.
        Return an ag-grid filterModel JSON object I can apply to the grid
        to surface similar rows. Only return valid ag-grid filter model JSON, nothing else.`;
      }
    }
  ];

  constructor(private readonly gridAiService: GridAiService) {}

  agInit(params: ICellRendererParams): void {
    this.params = params;
  }

  refresh(): boolean {
    return false;
  }

  runAction(action: GridRowAction): void {
    const rowData = this.params.data;
    const gridApi = this.params.api;

    const gridState = gridApi.getState();

    // const ssParams: StructuredSchemaParams = {
    //   exclude: ['pivot', 'rowGroup'],
    //   columns: { price: { description: 'USD', includeSetValues: true } }
    // };
    // const structuredSchema = gridApi.getStructuredSchema(
    //   ssParams
    // );
    const structuredSchema = gridApi.getStructuredSchema();

    this.loadingAction = action.id;

    this.gridAiService
      .askAI(action.userQuery(rowData), gridState, structuredSchema)
      .subscribe({
        next: (response: any) => this.applyAiFilterModel(response, gridApi),
        error: (err) => {
          console.error(`[GridAction] ${action.id} failed`, err);
          this.loadingAction = null;
        },
        complete: () => {
          this.loadingAction = null;
        }
      });
  }

  private applyAiFilterModel(response: any, gridApi: any): void {
    const filterModel = response?.filterModel ?? response;

    if (!filterModel || typeof filterModel !== 'object') {
      console.warn('[GridAction] No valid filterModel in AI response', response);
      return;
    }

    gridApi.setFilterModel(filterModel);
    gridApi.ensureIndexVisible(0);
  }
}
