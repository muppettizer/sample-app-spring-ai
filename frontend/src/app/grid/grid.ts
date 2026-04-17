import { Component, OnInit } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import {ColDef, GridApi, themeQuartz} from 'ag-grid-community';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { GridAiService } from './grid-ai.service';
import {NgIf} from '@angular/common';
import {GridActionsCellRendererComponent} from './actions/grid-actions-cell-renderer.component';

@Component({
  selector: 'app-grid',
  standalone: true,
  imports: [AgGridAngular, FormsModule],
  templateUrl: './grid.html',
  styles: [`
    .ask-ai-btn {
      padding: 5px 10px;
      margin-left: 5px;
      background-color: #007bff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      transition: background-color 0.3s ease;
    }
    .ask-ai-btn:hover:not(:disabled) {
      background-color: #0056b3;
    }
    .ask-ai-btn:active:not(:disabled) {
      background-color: #004085;
      transform: scale(0.98);
    }
    .ask-ai-btn:disabled {
      background-color: #6c757d;
      cursor: not-allowed;
    }
    .spinner {
      display: inline-block;
      width: 12px;
      height: 12px;
      border: 2px solid #ffffff;
      border-radius: 50%;
      border-top-color: transparent;
      animation: spin 1s ease-in-out infinite;
      margin-right: 5px;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `],
  host: { 'style': 'height: 100%; display: block;' }
})
export class GridComponent implements OnInit {

  gridApi!: GridApi;
  query = '';
  // theme = themeAlpine;
  theme = themeQuartz;

  columnDefs: ColDef[] = [
    { field: 'make' },
    { field: 'model' },
    { field: 'price', filter: 'agNumberColumnFilter' },
    { field: 'country' },
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

  rowData = [
    { make: 'Toyota', model: 'Corolla', price: 20000, country: 'Japan' },
    { make: 'Ford', model: 'Focus', price: 25000, country: 'USA' },
    { make: 'BMW', model: 'X5', price: 55000, country: 'Germany' },
    { make: 'Tesla', model: 'Model 3', price: 60000, country: 'USA' }
  ];

  constructor(private toastr: ToastrService, protected gridAiService: GridAiService) {}

  ngOnInit() {
    console.log('Grid component initialized');
  }

  onGridReady(params: any) {
    console.log('Grid ready', params);
    this.gridApi = params.api;
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
