import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../config/api.config';

@Injectable({ providedIn: 'root' })
export class HealthApiService {
  private readonly http = inject(HttpClient);

  check(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${API_BASE}/actuator/health`);
  }
}