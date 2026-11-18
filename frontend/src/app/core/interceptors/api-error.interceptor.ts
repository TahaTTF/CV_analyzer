import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../../shared/models/api-error.model';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(catchError((err: unknown) => throwError(() => toApiError(err, req.url))));

function toApiError(err: unknown, path: string): ApiError {
  const timestamp = new Date().toISOString();

  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return {
        timestamp, status: 0, code: 'NETWORK_UNAVAILABLE', path, fieldErrors: [], traceId: null,
        message: 'Cannot reach the server. Check that the backend is running.',
      };
    }
    const body = err.error;
    if (body && typeof body === 'object' && typeof body.code === 'string') {
      return {
        timestamp: body.timestamp ?? timestamp,
        status: body.status ?? err.status,
        code: body.code,
        message: body.message ?? 'Unexpected error',
        path: body.path ?? path,
        fieldErrors: Array.isArray(body.fieldErrors) ? body.fieldErrors : [],
        traceId: body.traceId ?? null,
      };
    }
    return {
      timestamp, status: err.status, code: `HTTP_${err.status}`, path, fieldErrors: [], traceId: null,
      message: 'Unexpected server response.',
    };
  }

  return {
    timestamp, status: -1, code: 'UNKNOWN_ERROR', path, fieldErrors: [], traceId: null,
    message: 'Something went wrong.',
  };
}