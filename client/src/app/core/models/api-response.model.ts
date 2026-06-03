export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: unknown;
}

export interface HealthData {
  application: string;
  status: string;
  timestamp: string;
}
