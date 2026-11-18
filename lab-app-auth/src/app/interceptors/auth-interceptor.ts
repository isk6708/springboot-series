import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../apis/auth-api';

// Public endpoints: never send a token to them
const PUBLIC_URLS = ['/login', '/users/register'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();
  const isPublicCall = PUBLIC_URLS.some((url) => req.url.endsWith(url));

  if (token && !isPublicCall) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req);
};