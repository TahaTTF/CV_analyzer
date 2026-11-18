import { Injectable, signal } from '@angular/core';
import { CvParseResponse } from '../../shared/models/cv.model';
import { RecommendationResponse } from '../../shared/models/recommendation.model';

// Memory only. Never put the CV or its data in localStorage, sessionStorage or URLs.
@Injectable({ providedIn: 'root' })
export class CvAnalysisStore {
  readonly file = signal<File | null>(null);
  readonly parseResponse = signal<CvParseResponse | null>(null);
  readonly recommendationResponse = signal<RecommendationResponse | null>(null);

  setParsed(file: File, response: CvParseResponse): void {
    this.file.set(file);
    this.parseResponse.set(response);
    this.recommendationResponse.set(null);
  }

  reset(): void {
    this.file.set(null);
    this.parseResponse.set(null);
    this.recommendationResponse.set(null);
  }
}