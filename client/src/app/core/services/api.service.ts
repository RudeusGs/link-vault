import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiResponse, HealthData } from '../models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getHealth(): Observable<ApiResponse<HealthData>> {
    return this.http
      .get<ApiResponse<HealthData>>(`${this.baseUrl}/health`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  get<T>(path: string, params?: object): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(`${this.baseUrl}${path}`, { params: this.toParams(params) })
      .pipe(
        map((response) => this.unwrap(response)),
        catchError((error) => this.handleError(error))
      );
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .post<ApiResponse<T>>(`${this.baseUrl}${path}`, body)
      .pipe(
        map((response) => this.unwrap(response)),
        catchError((error) => this.handleError(error))
      );
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .put<ApiResponse<T>>(`${this.baseUrl}${path}`, body)
      .pipe(
        map((response) => this.unwrap(response)),
        catchError((error) => this.handleError(error))
      );
  }

  patch<T>(path: string, body: unknown = null): Observable<T> {
    return this.http
      .patch<ApiResponse<T>>(`${this.baseUrl}${path}`, body)
      .pipe(
        map((response) => this.unwrap(response)),
        catchError((error) => this.handleError(error))
      );
  }

  delete<T>(path: string): Observable<T> {
    return this.http
      .delete<ApiResponse<T>>(`${this.baseUrl}${path}`)
      .pipe(
        map((response) => this.unwrap(response)),
        catchError((error) => this.handleError(error))
      );
  }

  upload<T>(path: string, formData: FormData): Observable<T> {
    return this.http
      .post<ApiResponse<T>>(`${this.baseUrl}${path}`, formData)
      .pipe(
        map((response) => this.unwrap(response)),
        catchError((error) => this.handleError(error))
      );
  }

  download(path: string): Observable<Blob> {
    return this.http
      .get(`${this.baseUrl}${path}`, { responseType: 'blob' })
      .pipe(catchError((error) => this.handleError(error)));
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    if (!response.success) {
      throw new Error(response.message || 'Request failed');
    }

    return response.data;
  }

  private toParams(params?: object): HttpParams {
    let httpParams = new HttpParams();
    if (!params) {
      return httpParams;
    }

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return httpParams;
  }

  private handleError(error: unknown): Observable<never> {
    if (error instanceof HttpErrorResponse) {
      const payload = error.error as { message?: string; errors?: unknown } | string | null;
      if (payload && typeof payload === 'object') {
        const message = typeof payload.message === 'string' ? payload.message : '';
        if (message) {
          return throwError(() => new Error(this.composePayloadMessage(message, payload.errors)));
        }
      }

      if (error.status === 0) {
        return throwError(() => new Error('Không kết nối được backend. Kiểm tra container server trước nha.'));
      }

      if (error.status === 502) {
        return throwError(() => new Error('Backend đang chưa sẵn sàng hoặc đã crash. Xem log linkvault-server.'));
      }

      if (error.status === 401) {
        return throwError(() => new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'));
      }

      if (error.status === 403) {
        return throwError(() => new Error('Bạn không có quyền thực hiện thao tác này.'));
      }

      if (error.status === 404) {
        return throwError(() => new Error('Không tìm thấy dữ liệu được yêu cầu.'));
      }

      if (error.status >= 500) {
        return throwError(() => new Error('Backend đang gặp lỗi. Vui lòng thử lại sau hoặc kiểm tra log server.'));
      }

      return throwError(() => new Error(error.statusText || `HTTP ${error.status}`));
    }

    return throwError(() => (error instanceof Error ? error : new Error('Có lỗi không xác định')));
  }

  private composePayloadMessage(message: string, errors: unknown): string {
    const details = this.formatValidationErrors(errors);
    return details ? `${message}: ${details}` : message;
  }

  private formatValidationErrors(errors: unknown): string {
    if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
      return '';
    }

    return Object.entries(errors as Record<string, unknown>)
      .map(([field, value]) => `${field} ${String(value)}`)
      .join('; ');
  }
}

