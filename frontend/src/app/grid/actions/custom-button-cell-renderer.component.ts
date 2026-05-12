import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { type ICellRendererAngularComp } from 'ag-grid-angular';
import { type ICellRendererParams } from 'ag-grid-community';
import { GridAiService } from '../grid-ai.service';

@Component({
  standalone: true,
  selector: 'app-custom-button-cell-renderer',
  imports: [CommonModule],
  templateUrl: './custom-button-cell-renderer.component.html',
  styleUrl: './custom-button-cell-renderer.component.scss'
})
export class CustomButtonCellRendererComponent implements ICellRendererAngularComp {
  params!: ICellRendererParams;
  loading = false;

  constructor(
    private readonly gridAiService: GridAiService,
    private readonly toastr: ToastrService
  ) {}

  agInit(params: ICellRendererParams): void {
    this.params = params;
  }

  refresh(): boolean {
    return false;
  }

  onClick(ev: MouseEvent): void {
    ev.stopPropagation();
    const row = this.params.data as Record<string, unknown> | undefined;
    const instrumentId = typeof row?.['securityId'] === 'string' ? row['securityId'] : '';

    if (!instrumentId || this.loading) {
      if (!instrumentId) {
        this.toastr.warning('Missing security ID on row', 'Holdings');
      }
      return;
    }

    this.loading = true;
    this.gridAiService.portfolioHoldingsAssistant(instrumentId, row ?? {}).subscribe({
      next: (response) => {
        const msg = typeof response?.assistantMessage === 'string'
          ? response.assistantMessage
          : JSON.stringify(response);
        this.toastr.info(msg, 'Portfolio holdings', { timeOut: 25000, enableHtml: false });
      },
      error: () => this.toastr.error('Holdings assistant request failed', 'Holdings'),
      complete: () => {
        this.loading = false;
      }
    });
  }
}
