import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { CvAnalysisStore } from './features/cv-analysis/cv-analysis.store';

// The CV only lives in memory, so after a refresh there is nothing to review or match.
const requireParsedCv: CanActivateFn = () =>
  inject(CvAnalysisStore).parseResponse() !== null ||
  inject(Router).createUrlTree(['/cv'], { queryParams: { session: 'expired' } });

const requireCvFile: CanActivateFn = () =>
  inject(CvAnalysisStore).file() !== null ||
  inject(Router).createUrlTree(['/cv'], { queryParams: { session: 'expired' } });

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'CV Analyzer',
    loadComponent: () =>
      import('./features/dashboard/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'cv',
    title: 'Upload your CV · CV Analyzer',
    loadComponent: () =>
      import('./features/cv-analysis/cv-upload/cv-upload').then((m) => m.CvUpload),
  },
  {
    path: 'cv/review',
    title: 'Review your profile · CV Analyzer',
    canActivate: [requireParsedCv],
    loadComponent: () =>
      import('./features/cv-analysis/profile-review/profile-review').then((m) => m.ProfileReview),
  },
  {
    path: 'recommendations',
    title: 'Recommendations · CV Analyzer',
    canActivate: [requireCvFile],
    loadComponent: () =>
      import('./features/recommendations/recommendation-page/recommendation-page').then(
        (m) => m.RecommendationPage,
      ),
  },
  // /jobs and /jobs/:jobId are added in the next step.
  { path: '**', redirectTo: '' },
];