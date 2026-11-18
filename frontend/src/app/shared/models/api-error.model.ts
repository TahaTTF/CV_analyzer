export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number; // 0 = network/CORS failure
  code: string;
  message: string;
  path: string;
  fieldErrors: FieldError[];
  traceId: string | null;
}