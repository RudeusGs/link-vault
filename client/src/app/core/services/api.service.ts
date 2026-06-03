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
      .pipe(catchError((error) => this.toClientError(error)));
  }

  get<T>(path: string, params?: object): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(`${this.baseUrl}${path}`, { params: this.toParams(params) })
      .pipe(
        map((response) => this.unwrap(response)),
        catchError((error) => this.toClientError(error))
      );
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<ApiResponse<T>>(`${this.baseUrl}${path}`, body).pipe(
      map((response) => this.unwrap(response)),
      catchError((error) => this.toClientError(error))
    );
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<ApiResponse<T>>(`${this.baseUrl}${path}`, body).pipe(
      map((response) => this.unwrap(response)),
      catchError((error) => this.toClientError(error))
    );
  }

  patch<T>(path: string, body: unknown = null): Observable<T> {
    return this.http.patch<ApiResponse<T>>(`${this.baseUrl}${path}`, body).pipe(
      map((response) => this.unwrap(response)),
      catchError((error) => this.toClientError(error))
    );
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<ApiResponse<T>>(`${this.baseUrl}${path}`).pipe(
      map((response) => this.unwrap(response)),
      catchError((error) => this.toClientError(error))
    );
  }

  upload<T>(path: string, formData: FormData): Observable<T> {
    return this.http.post<ApiResponse<T>>(`${this.baseUrl}${path}`, formData).pipe(
      map((response) => this.unwrap(response)),
      catchError((error) => this.toClientError(error))
    );
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

  private toClientError(error: unknown): Observable<never> {
    if (error instanceof Error && !(error instanceof HttpErrorResponse)) {
      return throwError(() => error);
    }

    if (error instanceof HttpErrorResponse) {
      const body = error.error as Partial<ApiResponse<unknown>> | undefined;
      const message =
        body?.message ||
        error.message ||
        (error.status ? `Request failed with status ${error.status}` : 'Request failed');

      return throwError(() => new Error(message));
    }

    return throwError(() => new Error('Request failed'));
  }
}
