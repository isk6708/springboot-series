import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../apis/auth-api';

// Pages that need a logged-in user
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }

  auth.logout(false); // clear an expired token, if any
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

// Login & Register: if already logged in, go straight to the app
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? router.createUrlTree(['/todos']) : true;
};