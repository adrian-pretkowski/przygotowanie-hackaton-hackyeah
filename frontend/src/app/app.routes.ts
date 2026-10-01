import { Routes } from '@angular/router';
import { VisionDemoComponent } from './vision/vision-demo.component';

export const routes: Routes = [
  { path: '', component: VisionDemoComponent },
  { path: '**', redirectTo: '' },
];
