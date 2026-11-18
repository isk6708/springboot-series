import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { API_BASE_URL } from './api.config';
import {
  AuthUser,
  BackendUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
} from '../models/auth.model';

const TOKEN_KEY = 'lab_raj_token';
const USER_KEY = 'lab_raj_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private apiUrl = API_BASE_URL;

  private loginUrl = `${this.apiUrl}/login`;
  private registerUrl = `${this.apiUrl}/users/register`;

  // Signals = the UI (e.g. Navbar) updates automatically when we log in / out
  private token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private user = signal<AuthUser | null>(this.readUserFromStorage());

  readonly currentUser = this.user.asReadonly();

  // ---------- REGISTER (public) ----------
  // Creates the account only. The user logs in afterwards.
  register(data: RegisterRequest): Observable<AuthUser> {
    return this.http
      .post<BackendUser>(this.registerUrl, data)
      .pipe(map((u) => this.toSafeUser(u)));
  }

  // ---------- LOGIN (public) ----------
  login(credentials: LoginRequest): Observable<AuthUser> {
    return this.http.post<LoginResponse>(this.loginUrl, credentials).pipe(
      tap((res) => this.saveSession(res)),
      map(() => this.user()!)
    );
  }

  // ---------- LOGOUT ----------
  logout(redirect = true): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.token.set(null);
    this.user.set(null);
    if (redirect) {
      this.router.navigate(['/login']);
    }
  }

  // ---------- HELPERS ----------
  getToken(): string | null {
    return this.token();
  }

  isLoggedIn(): boolean {
    const t = this.token();
    return !!t && !this.isTokenExpired(t);
  }

  private saveSession(res: LoginResponse): void {
    const safeUser = this.toSafeUser(res.user);
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(safeUser));
    this.token.set(res.token);
    this.user.set(safeUser);
  }

  // Keep only safe fields – never keep the password hash in the browser
  private toSafeUser(u: BackendUser): AuthUser {
    return { id: u.id, fullname: u.fullname, email: u.email };
  }

  private readUserFromStorage(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    try {
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }

  // A JWT is "header.payload.signature". The payload is Base64URL JSON with "exp" (seconds).
  private isTokenExpired(token: string): boolean {
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const { exp } = JSON.parse(atob(payload));
      return typeof exp === 'number' && Date.now() >= exp * 1000;
    } catch {
      return true; // cannot read it → treat as expired
    }
  }
}