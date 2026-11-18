import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { JobDetails, JobFilters, JobPage } from '../../shared/models/job.model';
import { API_BASE } from '../config/api.config';

@Injectable({ providedIn: 'root' })
export class JobApiService {
  private readonly http = inject(HttpClient);

  search(filters: JobFilters): Observable<JobPage> {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    if (filters.query?.trim()) params = params.set('query', filters.query.trim());
    if (filters.location?.trim()) params = params.set('location', filters.location.trim());
    if (filters.source) params = params.set('source', filters.source);
    if (filters.relevance) params = params.set('relevance', filters.relevance);
    return this.http.get<JobPage>(`${API_BASE}/api/jobs`, { params });
  }

  getById(jobId: string): Observable<JobDetails> {
    return this.http.get<JobDetails>(`${API_BASE}/api/jobs/${encodeURIComponent(jobId)}`);
  }
}