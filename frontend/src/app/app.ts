import { Component, signal } from '@angular/core';
import { GridComponent } from './grid/grid';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [GridComponent],
  template: `<app-grid></app-grid>`,
  styles: [':host { height: 100dvh; display: block; }']
})
export class App {
  protected readonly title = signal('frontend');
}
