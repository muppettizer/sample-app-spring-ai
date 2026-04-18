import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import type {ICellRendererAngularComp} from 'ag-grid-angular';
import type {ICellRendererParams} from 'ag-grid-community';
import {GridAiService} from '../grid-ai.service';

@Component({
  standalone: true,
  selector: 'app-prompts-cell-renderer',
  imports: [CommonModule],
  templateUrl: './prompts-cell-renderer.component.html',
  styleUrl: './prompts-cell-renderer.component.scss'
})
export class PromptsCellRendererComponent implements ICellRendererAngularComp {
  private params!: ICellRendererParams;

  constructor(private gridAiService: GridAiService) {
  }

  agInit(params: ICellRendererParams): void {
    this.params = params;
  }

  refresh(params: ICellRendererParams): boolean {
    this.agInit(params);
    return false;
  }

  onViewPrompts(): void {
    const rowData = this.params.data;
    const gridState = this.params.api.getState();
    const structuredSchema = this.params.api.getStructuredSchema();

    const userQuery = `Generate some interesting prompts or questions about this security: ${JSON.stringify(rowData)}.
    Return a list of helpful analysis prompts I can use to explore this data.`;

    this.gridAiService.askAI(userQuery, gridState, structuredSchema).subscribe({
      next: (response) => {
        console.log('View Prompts Response:', response);
        // Handle response - display prompts to user
      },
      error: (error) => {
        console.error('View Prompts Error:', error);
      }
    });
  }

  onFindSimilar(): void {
    const rowData = this.params.data;
    const gridState = this.params.api.getState();
    const structuredSchema = this.params.api.getStructuredSchema();

    const userQuery = `Find securities similar to this one based on:
- asset class
- issuer / counterparty
- credit quality
- maturity profile
- coupon / yield / spread characteristics
- seniority / collateral (if applicable)

Current security:
${JSON.stringify(rowData)}

Return ONLY a valid ag-grid filterModel JSON that would surface economically similar instruments.
Do not include any explanation or text.`;

    this.gridAiService.askAI(userQuery, gridState, structuredSchema).subscribe({
      next: (response) => {
        console.log('Find Similar Response:', response);
        this.applyFilterModel(response);
      },
      error: (error) => {
        console.error('Find Similar Error:', error);
      }
    });
  }

  onFindSafer(): void {
    const rowData = this.params.data;
    const gridState = this.params.api.getState();
    const structuredSchema = this.params.api.getStructuredSchema();

    const userQuery = `Find securities that are lower risk than this one based on:
- higher credit rating (or equivalent internal rating)
- lower spread / yield premium
- shorter duration or maturity
- higher seniority (senior preferred over subordinated)
- stronger collateral coverage (if applicable)

Keep other attributes broadly similar (same asset class / issuer type where possible).

Current security:
${JSON.stringify(rowData)}

Return ONLY a valid ag-grid filterModel JSON. Do not include any explanation or text.`;

    this.gridAiService.askAI(userQuery, gridState, structuredSchema).subscribe({
      next: (response) => {
        console.log('Find Safer Response:', response);
        this.applyFilterModel(response);
      },
      error: (error) => {
        console.error('Find Safer Error:', error);
      }
    });
  }

  onAddToWatchlist(): void {
    const rowData = this.params.data;
    const gridState = this.params.api.getState();
    const structuredSchema = this.params.api.getStructuredSchema();

    const userQuery = `Add this security to my watchlist: ${JSON.stringify(rowData)}.
    Confirm the security has been added to the watchlist and provide a brief summary.`;

    this.gridAiService.askAI(userQuery, gridState, structuredSchema).subscribe({
      next: (response) => {
        console.log('Add to Watchlist Response:', response);
        // Handle response - show confirmation
      },
      error: (error) => {
        console.error('Add to Watchlist Error:', error);
      }
    });
  }

  private applyFilterModel(response: any): void {
    const filterModel = response?.filterModel ?? response;

    if (!filterModel || typeof filterModel !== 'object') {
      console.warn('No valid filterModel in AI response', response);
      return;
    }

    this.params.api.setFilterModel(filterModel);
    this.params.api.ensureIndexVisible(0);
  }
}
