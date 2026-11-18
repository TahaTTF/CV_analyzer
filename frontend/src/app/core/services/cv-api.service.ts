import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CvParseResponse } from '../../shared/models/cv.model';
import { API_BASE } from '../config/api.config';

@Injectable({ providedIn: 'root' })
export class CvApiService {
  private readonly http = inject(HttpClient);

  parse(file: File): Observable<CvParseResponse> {
    const body = new FormData();
    body.append('file', file, file.name); // never set Content-Type manually
    return this.http.post<CvParseResponse>(`${API_BASE}/api/cv/parse`, body);
  }
}
