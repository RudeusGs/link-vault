import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const addToken = (req: any, token: string | null) => {
    return req.clone({
      setHeaders: token ? { Authorization: `Bearer ${token}` } : undefined,
      withCredentials: true
    });
  };

  const isApiRequest =
    request.url.includes('/api/') ||
    request.url.endsWith('/api') ||
    request.url.startsWith('http://localhost:8080/api');

  const isAuthEndpoint =
    request.url.includes('/auth/login') ||
    request.url.includes('/auth/register') ||
    request.url.includes('/auth/refresh') ||
    request.url.includes('/auth/logout');

  const shouldAttach = Boolean(auth.token()) && isApiRequest && !isAuthEndpoint;

  let authRequest = request;
  if (shouldAttach) {
    authRequest = addToken(request, auth.token());
  } else {
    // Even if we don't attach the token, we need withCredentials for cookies (like refresh token)
    authRequest = request.clone({ withCredentials: true });
  }

  return next(authRequest).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthEndpoint) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          return auth.refreshToken().pipe(
            switchMap((response) => {
              isRefreshing = false;
              const newToken = response.accessToken ?? response.token ?? '';
              refreshTokenSubject.next(newToken);
              return next(addToken(request, newToken));
            }),
            catchError((refreshError) => {
              isRefreshing = false;
              auth.logout();
              router.navigate(['/login']);
              return throwError(() => refreshError);
            })
          );
        } else {
          return refreshTokenSubject.pipe(
            filter((token) => token != null),
            take(1),
            switchMap((token) => {
              return next(addToken(request, token));
            })
          );
        }
      }

      return throwError(() => error);
    })
  );
};
