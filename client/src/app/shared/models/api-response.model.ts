export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  errorCode?: string | null;
  details?: unknown;
  timestamp: string;
}

export interface HealthData {
  application: string;
  status: string;
  timestamp: string;
}
