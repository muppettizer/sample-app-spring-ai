import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { AllEnterpriseModule, ModuleRegistry } from 'ag-grid-enterprise';

ModuleRegistry.registerModules([AllEnterpriseModule]);

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
