import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HealthApiService } from '../../../core/services/health-api.service';
import { CvAnalysisStore } from '../../cv-analysis/cv-analysis.store';

type BackendStatus = 'checking' | 'up' | 'down';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly store = inject(CvAnalysisStore);

  readonly backend = signal<BackendStatus>('checking');
  readonly hasCv = () => this.store.file() !== null;

  readonly steps = [
    { n: '01', title: 'Upload your CV', text: 'Drop a PDF or DOCX with selectable text, up to 5 MB.' },
    { n: '02', title: 'Review extracted skills', text: 'See your target role, skills, experience and education as the parser understood them.' },
    { n: '03', title: 'Explore your matches', text: 'Get ranked IT jobs with matched and missing skills and a clear explanation.' },
  ];

  constructor() {
    inject(HealthApiService).check().subscribe({
      next: (r: unknown) => this.backend.set(
        typeof r === 'object' && r !== null && 'status' in r && r.status === 'UP'
          ? 'up'
          : 'down'
      ),
      error: () => this.backend.set('down'),
    });
  }
}