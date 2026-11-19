import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CvApiService } from './cv-api.service';
import { JobApiService } from './job-api.service';
import { RecommendationApiService } from './recommendation-api.service';

describe('API services', () => {
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('CvApiService posts the file in the multipart field "file" without a manual Content-Type', () => {
    const file = new File(['content'], 'cv.pdf', { type: 'application/pdf' });
    TestBed.inject(CvApiService).parse(file).subscribe();

    const req = ctrl.expectOne('/api/cv/parse');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('file')).toBeTruthy();
    expect(req.request.headers.has('Content-Type')).toBe(false);
    req.flush({});
  });

  it('RecommendationApiService sends topK as a query parameter', () => {
    const file = new File(['content'], 'cv.pdf');
    TestBed.inject(RecommendationApiService).recommend(file, 5).subscribe();

    const req = ctrl.expectOne((r) => r.url === '/api/recommendations/from-cv');
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('topK')).toBe('5');
    req.flush({});
  });

  it('JobApiService.search serializes filters and omits empty ones', () => {
    TestBed.inject(JobApiService)
      .search({ page: 2, size: 20, query: ' data ', location: '', source: 'ADZUNA', relevance: 'DEFAULT' })
      .subscribe();

    const req = ctrl.expectOne((r) => r.url === '/api/jobs');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.get('query')).toBe('data');
    expect(req.request.params.has('location')).toBe(false);
    expect(req.request.params.get('source')).toBe('ADZUNA');
    req.flush({});
  });

  it('JobApiService.getById encodes the identifier', () => {
    TestBed.inject(JobApiService).getById('a/b').subscribe();
    ctrl.expectOne('/api/jobs/a%2Fb').flush({});
  });
});