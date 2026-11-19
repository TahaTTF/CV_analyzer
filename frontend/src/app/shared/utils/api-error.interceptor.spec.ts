import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../../core/interceptors/api-error.interceptor';
import { ApiError } from '../../shared/models/api-error.model';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  function failWith(body: string | number | boolean | object | null, status: number): ApiError {
    let captured!: ApiError;
    http.get('/api/test').subscribe({ error: (e: ApiError) => (captured = e) });
    ctrl.expectOne('/api/test').flush(body, { status, statusText: 'Error' });
    return captured;
  }

  it('keeps the problem+json fields from Spring', () => {
    const err = failWith(
      { code: 'DATABASE_UNAVAILABLE', status: 500, message: 'down', path: '/api/test',
        fieldErrors: [], traceId: 'abc123', timestamp: 't' },
      500,
    );
    expect(err.code).toBe('DATABASE_UNAVAILABLE');
    expect(err.traceId).toBe('abc123');
  });

  it('treats a gateway error without a body as the backend being unreachable', () => {
    const err = failWith('proxy error', 500);
    expect(err.code).toBe('NETWORK_UNAVAILABLE');
  });

  it('maps a network failure (status 0) to NETWORK_UNAVAILABLE', () => {
    let captured!: ApiError;
    http.get('/api/test').subscribe({ error: (e: ApiError) => (captured = e) });
    ctrl.expectOne('/api/test').error(new ProgressEvent('error'));
    expect(captured.code).toBe('NETWORK_UNAVAILABLE');
    expect(captured.status).toBe(0);
  });

  it('keeps other HTTP statuses as generic codes', () => {
    expect(failWith('nope', 418).code).toBe('HTTP_418');
  });
});