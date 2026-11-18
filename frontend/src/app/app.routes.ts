import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'CV Analyzer',
    loadComponent: () =>
      import('./features/dashboard/dashboard/dashboard').then((m) => m.Dashboard),
  },
  // The other pages (/cv, /cv/review, /recommendations, /jobs, /jobs/:jobId) are added in the next steps.
  { path: 'cv',
    title: 'Upload your CV · CV Analyzer',
    loadComponent: () =>
      import('./features/cv-analysis/cv-upload/cv-upload').then((m) => m.CvUpload),
  }, { path: '**', redirectTo: '' },
];