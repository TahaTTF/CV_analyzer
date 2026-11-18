import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RecommendationResponse } from '../../shared/models/recommendation.model';
import { API_BASE } from '../config/api.config';

@Injectable({ providedIn: 'root' })
export class RecommendationApiService {
  private readonly http = inject(HttpClient);

  recommend(file: File, topK = 10): Observable<RecommendationResponse> {
    const body = new FormData();
    body.append('file', file, file.name);
    const params = new HttpParams().set('topK', topK);
    return this.http.post<RecommendationResponse>(
      `${API_BASE}/api/recommendations/from-cv`, body, { params });
  }
}