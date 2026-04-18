import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import type { ICellRendererAngularComp } from 'ag-grid-angular';
import type { ICellRendererParams } from 'ag-grid-community';

@Component({
  standalone: true,
  selector: 'app-prompts-cell-renderer',
  imports: [CommonModule],
  templateUrl: './prompts-cell-renderer.component.html',
  styleUrl: './prompts-cell-renderer.component.scss'
})
export class PromptsCellRendererComponent implements ICellRendererAngularComp {
  agInit(params: ICellRendererParams): void {
    // Initialize with data if needed
  }

  refresh(params: ICellRendererParams): boolean {
    // Return false to indicate component does not need to be re-rendered
    return false;
  }
}
