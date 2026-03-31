/** Matches the backend's structured error response from GlobalExceptionHandler */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
