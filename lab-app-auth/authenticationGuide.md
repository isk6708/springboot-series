# authenticationGuide.md — Adding JWT Login to `lab-raj-app`

This guide adds **authentication** to the Angular app (`lab-raj-app`) using the JWT security
already built in the Spring Boot app (`lab-raj`).

We build it in **small iterations**. After each iteration, run the app and do the
**✅ Check** before moving on.

| Iteration | What we build | New / changed files |
|---|---|---|
| 0 | Backend ready: port, CORS, first user | `SecurityConfig.java`, `apis/api.config.ts` |
| 1 | Auth model + `AuthService` (login, store token, logout) | `models/auth.model.ts`, `apis/auth.ts` |
| 2 | Login page | `pages/login/*`, `app.routes.ts` |
| 3 | HTTP interceptor: send `Authorization: Bearer <token>` | `interceptors/auth-interceptor.ts`, `app.config.ts` |
| 4 | Route guard: block pages until logged in | `guards/auth-guard.ts`, `app.routes.ts` |
| 5 | Navbar: show user + Logout | `navbar/*` |
| 6 | Handle expired / invalid token | `interceptors/auth-interceptor.ts`, `pages/login/*` |
| 7 *(optional)* | Register page (sign up) | `pages/register/*` |
| 8 *(optional)* | "My Private Todos" using the logged-in user | `apis/todo-api.ts`, `pages/my-todos/*` |

---

## How JWT login works in this project (read first)

```
 Angular                                   Spring Boot (lab-raj :8117)
 ───────                                   ───────────────────────────
 1. POST /api/v1/login  {email,password} ─▶ LoginController
                                            LoginService checks BCrypt password
 2. ◀─ 200 { token, type:"Bearer", user }   JwtUtil creates a signed token (10 hours)

 3. Save token in localStorage

 4. GET /api/v1/todos                     ─▶ JwtRequestFilter reads the header
    Authorization: Bearer eyJhbGciOi...      validates token → request is "authenticated"
 5. ◀─ 200 [ ...todos ]

    No token / bad token                 ─▶ Spring Security blocks the request
 6. ◀─ 403 (or 401)                          → Angular logs out and goes to /login
```

What the backend gives us (from the `lab-raj` source):

| Item | Value |
|---|---|
| Base URL | `http://localhost:8117/api/v1` (**port changed from 8116 to 8117**) |
| Login | `POST /api/v1/login` — **public** (`permitAll`) |
| Login request body | `{ "email": "...", "password": "..." }` (`LoginRequest`) |
| Login success | `200` → `{ "token": "eyJ...", "type": "Bearer", "user": { "id", "fullname", "email", "password" } }` |
| Login failure | `401` with plain-text body `Error: Incorrect username or password` |
| Protected | `/api/v1/todos/**`, `/api/v1/users/**`, everything else (`anyRequest().authenticated()`) |
| Header to send | `Authorization: Bearer <token>` (read by `JwtRequestFilter`) |
| Token lifetime | 10 hours (hard-coded in `JwtUtil.createToken`) |
| Token content | `sub` = user email, `iat`, `exp` |
| Passwords | Stored as **BCrypt** hashes (`UserService` + `SecurityConfig.passwordEncoder`) |

> ⚠️ Without a token, Spring Security in this project returns **403 Forbidden** (not 401), because
> no custom `AuthenticationEntryPoint` is configured. Our Angular code handles **both** 401 and 403.

---

## Iteration 0 — Get the backend ready

### 0.1 Port changed → one place for the base URL (Angular)

`application.properties` now has `server.port=8117`, but `todo-api.ts` still calls `8116`.
Instead of fixing every service, create **one constant**:

`src/app/apis/api.config.ts`

```ts
// Single place for the backend address. Change it here only.
export const API_BASE_URL = 'http://localhost:8117/api/v1';
```

Then use it in `todo-api.ts` (and `user-api.ts` if you already did `UserGuide.md`):

```ts
import { API_BASE_URL } from './api.config';
// ...
private apiUrl = `${API_BASE_URL}/todos`;
```

Also update the text badge in `header/header.html` (`http://localhost:8116/...` → `8117`).

### 0.2 CORS for *all* endpoints, including `/login` (Spring Boot)

`SecurityConfig` has `.cors(Customizer.withDefaults())`. With no CORS bean, Spring only uses the
`@CrossOrigin` annotations on controllers. **`LoginController` and `UserController` have none**, so
the browser will block the login call from `http://localhost:4200`.

The clean fix is **one global CORS bean** in `SecurityConfig.java` (Spring Security picks it up
automatically through `.cors(Customizer.withDefaults())`):

```java
// add these imports
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// add this bean inside SecurityConfig
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:4200"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

> `Authorization` **must** be in the allowed headers, otherwise the browser's preflight
> (`OPTIONS`) request fails as soon as we start sending the token.
> After this, the `@CrossOrigin(...)` on `TodoController` is no longer needed (keeping it is harmless).

### 0.3 Create the first user (with a BCrypt password)

Two facts make this tricky:

1. Users created **before** BCrypt was switched on have **plain-text** passwords in SQLite.
   `passwordEncoder.matches(...)` will fail for them → they **cannot log in**.
2. `POST /api/v1/users` is now **protected**, so you cannot create a user without logging in first.
   (The `spring.security.user.name=admin` lines in `application.properties` are **not used**,
   because the project has its own `UserDetailsService` = `LoginService`.)

**Recommended:** allow anyone to **create** a user (sign-up), but keep list/update/delete protected.
In `SecurityConfig.securityFilterChain`, add this line **before** the `/api/v1/users/**` rule
(order matters — the first matching rule wins):

```java
import org.springframework.http.HttpMethod;
// ...
.requestMatchers("/api/v1/login").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()   // sign-up
// ... swagger + static rules ...
.requestMatchers("/api/v1/todos/**", "/api/v1/users/**").authenticated()
```

Restart Spring Boot, then create a user (Windows `cmd`):

```bat
curl -X POST http://localhost:8117/api/v1/users ^
  -H "Content-Type: application/json" ^
  -d "{\"fullname\":\"Admin Lab\",\"email\":\"admin@heitech.com\",\"password\":\"secret123\"}"
```

### ✅ Check (Iteration 0)

```bat
:: 1. Login → copy the "token" value from the response
curl -X POST http://localhost:8117/api/v1/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"admin@heitech.com\",\"password\":\"secret123\"}"

:: 2. Without token → 403
curl -i http://localhost:8117/api/v1/todos

:: 3. With token → 200 + JSON list
curl -i http://localhost:8117/api/v1/todos -H "Authorization: Bearer PASTE_TOKEN_HERE"
```

You can also use Swagger (`http://localhost:8117/swagger-ui.html`): call `/api/v1/login`, copy the
token, click **Authorize** 🔒, paste the token.

Now open the Angular app: the Todos page should show **"Could not load todos from server."**
That is expected — Angular does not send a token yet.

---

## Iteration 1 — Auth model + `AuthService`

### 1.1 Model: `src/app/models/auth.model.ts`

```ts
// What we SEND to POST /api/v1/login (matches LoginRequest.java)
export interface LoginRequest {
  email: string;
  password: string;
}

// The user we keep in the browser (no password!)
export interface AuthUser {
  id: number;
  fullname: string;
  email: string;
}

// What we RECEIVE from POST /api/v1/login (matches LoginResponse.java)
// Note: backend UserDto also contains the BCrypt password hash – we drop it.
export interface LoginResponse {
  token: string;
  type: string; // "Bearer"
  user: AuthUser & { password?: string };
}
```

### 1.2 Service: `src/app/apis/auth.ts`

```bash
ng generate service apis/auth
```

Replace the content:

```ts
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { AuthUser, LoginRequest, LoginResponse } from '../models/auth.model';

const TOKEN_KEY = 'lab_raj_token';
const USER_KEY = 'lab_raj_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private loginUrl = `${API_BASE_URL}/login`;

  // Signals = the UI updates automatically when we log in / out
  private token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private user = signal<AuthUser | null>(this.readUserFromStorage());

  readonly currentUser = this.user.asReadonly();

  // ---------- LOGIN ----------
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
    // Keep only safe fields – never store the password hash
    const safeUser: AuthUser = {
      id: res.user.id,
      fullname: res.user.fullname,
      email: res.user.email,
    };
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(safeUser));
    this.token.set(res.token);
    this.user.set(safeUser);
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
```

Teaching points:

- **Why `localStorage`?** It survives a page refresh. (Trade-off: any script on the page can read
  it — XSS risk. An `HttpOnly` cookie is safer but needs backend changes. Good discussion topic.)
- **Why decode `exp` in the browser?** Only to know *when* to log out. The browser cannot verify
  the signature — **only the server trusts the token**.
- Fix the generated `auth.spec.ts` to import `AuthService` (same as `todo-api.spec.ts`).

### ✅ Check (Iteration 1)

`ng build` (or `npm start`) compiles with no errors. Nothing visible changes yet.

---

## Iteration 2 — Login page

```bash
ng generate component pages/login
```

### 2.1 `pages/login/login.ts`

```ts
import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../apis/auth';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  email = '';
  password = '';

  isSubmitting = signal(false);
  formError = signal('');

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.formError.set('');

    this.auth.login({ email: this.email.trim(), password: this.password }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/todos';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Login failed:', err);
        this.formError.set(
          err.status === 401
            ? 'Incorrect email or password.'
            : 'Cannot reach the server. Is Spring Boot running on port 8117?'
        );
        this.isSubmitting.set(false);
      },
    });
  }
}
```

### 2.2 `pages/login/login.html`

Same look as the task form (same CSS class names):

```html
<form #loginForm="ngForm" (ngSubmit)="onSubmit(loginForm)" class="task-form-card" novalidate>
  <h3 class="form-title">Sign in</h3>

  @if (formError()) {
    <div class="form-error">{{ formError() }}</div>
  }

  <div class="form-group">
    <label for="email">Email <span class="required">*</span></label>
    <input
      id="email" name="email" type="email" required email
      [(ngModel)]="email" #emailInput="ngModel"
      placeholder="admin@heitech.com" autocomplete="username"
    />
    @if (emailInput.invalid && (emailInput.dirty || emailInput.touched)) {
      <small class="validation-msg">A valid email is required.</small>
    }
  </div>

  <div class="form-group">
    <label for="password">Password <span class="required">*</span></label>
    <input
      id="password" name="password" type="password" required
      [(ngModel)]="password" #passwordInput="ngModel"
      autocomplete="current-password"
    />
    @if (passwordInput.invalid && (passwordInput.dirty || passwordInput.touched)) {
      <small class="validation-msg">Password is required.</small>
    }
  </div>

  <div class="form-actions">
    <button type="submit" class="btn-submit" [disabled]="loginForm.invalid || isSubmitting()">
      {{ isSubmitting() ? 'Signing in...' : 'Sign in' }}
    </button>
  </div>
</form>
```

### 2.3 `pages/login/login.css`

```bat
copy src\app\pages\task-form\task-form.css src\app\pages\login\login.css
```

Optionally add at the end to centre the card:

```css
:host {
  display: block;
  max-width: 420px;
  margin: 2rem auto;
}
```

### 2.4 Route: `app.routes.ts`

```ts
import { LoginComponent } from './pages/login/login';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'todos', pathMatch: 'full' },
  { path: 'todos', component: TaskComponent },
  { path: 'users', component: UserComponent },
  { path: '**', redirectTo: 'todos' }
];
```

### ✅ Check (Iteration 2)

1. Open `http://localhost:4200/login`.
2. Wrong password → "Incorrect email or password."
3. Correct login → goes to `/todos`.
4. DevTools → **Application → Local Storage** → you see `lab_raj_token` and `lab_raj_user`.
5. Paste the token into <https://jwt.io> → payload has `sub` (your email) and `exp`.
6. The Todos page **still fails** — the token is saved but not sent. Next iteration fixes that.

> If login shows "Cannot reach the server" and the console says **CORS**, redo step 0.2.

---

## Iteration 3 — Interceptor: attach the token to every request

An **interceptor** runs for every `HttpClient` call. We add the header in one place instead of in
every service.

```bash
ng generate interceptor interceptors/auth
```

Replace `src/app/interceptors/auth-interceptor.ts` (the CLI file name may be `auth-interceptor.ts`):

```ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../apis/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  // Do not send a token to the login endpoint
  const isLoginCall = req.url.endsWith('/login');

  if (token && !isLoginCall) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req);
};
```

Register it in `app.config.ts`:

```ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './interceptors/auth-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
```

### ✅ Check (Iteration 3)

1. Log in, open `/todos` → the list loads again. Add / edit / delete work.
2. DevTools → **Network** → click the `todos` request → **Request Headers** shows
   `Authorization: Bearer eyJ...`.
3. Delete `lab_raj_token` in Local Storage, refresh → Todos fails again (403).
   No redirect yet — that is Iterations 4 and 6.

---

## Iteration 4 — Route guard: protect the pages

A **guard** decides if the router may open a page. If the user is not logged in, we send them to
`/login` and remember where they wanted to go (`returnUrl`).

```bash
ng generate guard guards/auth --functional
```

(Choose **CanActivate** if the CLI asks.) Replace `src/app/guards/auth-guard.ts`:

```ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../apis/auth';

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

// The login page: if already logged in, go straight to the app
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? router.createUrlTree(['/todos']) : true;
};
```

Update `app.routes.ts`:

```ts
import { authGuard, guestGuard } from './guards/auth-guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: '', redirectTo: 'todos', pathMatch: 'full' },
  { path: 'todos', component: TaskComponent, canActivate: [authGuard] },
  { path: 'users', component: UserComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'todos' }
];
```

> A guard is **only for user experience**. The real protection is on the server
> (`SecurityConfig`). Anyone can bypass an Angular guard with DevTools — they still cannot get
> data without a valid token.

### ✅ Check (Iteration 4)

| Action | Expected |
|---|---|
| Logged out, open `/users` | Redirect to `/login?returnUrl=%2Fusers` |
| Log in from there | Lands on `/users` (not `/todos`) |
| Logged in, open `/login` | Redirect to `/todos` |
| Refresh the page while logged in | Still logged in (token in localStorage) |

---

## Iteration 5 — Navbar: who is logged in + Logout

### 5.1 `navbar/navbar.ts`

```ts
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../apis/auth';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class NavbarComponent {
  protected auth = inject(AuthService);

  logout(): void {
    this.auth.logout();
  }
}
```

### 5.2 `navbar/navbar.html`

```html
<nav class="app-navbar" aria-label="Main Navigation">
  <div class="nav-container">
    @if (auth.isLoggedIn()) {
      <a routerLink="/todos" routerLinkActive="active" class="nav-link">
        <span class="nav-icon" aria-hidden="true">☑</span>
        <span>My Todos</span>
      </a>

      <a routerLink="/users" routerLinkActive="active" class="nav-link">
        <span class="nav-icon" aria-hidden="true">👥</span>
        <span>Users</span>
      </a>

      <span class="nav-spacer"></span>

      <span class="nav-user" [title]="auth.currentUser()?.email">
        👤 {{ auth.currentUser()?.fullname }}
      </span>
      <button type="button" class="nav-link nav-logout" (click)="logout()">
        ⎋ Logout
      </button>
    } @else {
      <a routerLink="/login" routerLinkActive="active" class="nav-link">
        <span class="nav-icon" aria-hidden="true">🔑</span>
        <span>Login</span>
      </a>
    }
  </div>
</nav>
```

### 5.3 Add to `navbar/navbar.css`

```css
.nav-spacer { flex: 1; }

.nav-user {
  align-self: center;
  font-size: 0.9rem;
  opacity: 0.85;
}

.nav-logout {
  border: none;
  background: transparent;
  cursor: pointer;
  font: inherit;
}
```

> Because `token` and `user` are **signals** inside `AuthService`, the navbar updates by itself
> the moment you log in or out — no manual refresh needed.

### ✅ Check (Iteration 5)

1. Logged out → navbar shows only **Login**.
2. Log in → **My Todos**, **Users**, your full name and **Logout** appear immediately.
3. Logout → back to `/login`, Local Storage keys are gone, Back button cannot open `/todos`.

---

## Iteration 6 — Expired or invalid token

The token lives 10 hours. When it expires (or the user deletes / edits it, or the server's
`jwt.secret` changes), the backend refuses the request. We want Angular to **log out cleanly**
instead of showing a broken page.

### 6.1 Make it easy to test

Temporarily shorten the lifetime in `lab-raj/.../util/JwtUtil.java` (the 5-minute line is already
there, commented). For a fast demo use 1 minute:

```java
// long jwtExpirationInMs = 1000 * 60 * 60 * 10;
long jwtExpirationInMs = 1000 * 60 * 1;   // 1 minute – TESTING ONLY
```

Restart Spring Boot and log in again. **Remember to change it back afterwards.**

### 6.2 Catch 401/403 in the interceptor

Update `interceptors/auth-interceptor.ts`:

```ts
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../apis/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getToken();
  const isLoginCall = req.url.endsWith('/login');

  if (token && !isLoginCall) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // This backend answers 403 when the token is missing/invalid/expired
      // (sometimes 401 or 500 for an expired token – see "Backend notes").
      const authProblem = err.status === 401 || err.status === 403;

      if (authProblem && !isLoginCall) {
        auth.logout(false);
        router.navigate(['/login'], {
          queryParams: { returnUrl: router.url, reason: 'expired' },
        });
      }
      return throwError(() => err);
    })
  );
};
```

> In this lab every 403 means "not authenticated". Once you add **roles** (e.g. ADMIN only), a 403
> can also mean "logged in but not allowed" — then you should **not** log the user out on 403.

### 6.3 Tell the user why they were sent to login

In `login.ts` add:

```ts
sessionExpired = this.route.snapshot.queryParamMap.get('reason') === 'expired';
```

In `login.html`, under the title:

```html
@if (sessionExpired) {
  <div class="form-error">Your session has expired. Please sign in again.</div>
}
```

### ✅ Check (Iteration 6)

| Test | Expected |
|---|---|
| Wait > 1 minute, click **↻ Refresh** on Todos | Sent to `/login` with "session has expired" |
| Wait > 1 minute, navigate to `/users` | Guard sees expired `exp` → `/login` (no API call at all) |
| Change one character of `lab_raj_token` in Local Storage, refresh data | Sent to `/login` |
| Log in again | Returns to the page you were on |

If the expired request comes back as **500** instead of 403, see *Backend notes* below — the
guard still protects page navigation because it checks `exp` in the browser.

---

## Iteration 7 *(optional)* — Register page

Only if you made `POST /api/v1/users` public in step 0.3.

- `ng generate component pages/register`
- Form fields: `fullname`, `email`, `password` (min 6) — same as the user form in `UserGuide.md`.
- On submit: `POST ${API_BASE_URL}/users` with `{ fullname, email, password }`.
  - `201` → navigate to `/login` (optionally pre-fill the email).
  - `409` → "Email already registered".
  - `400` → validation message.
- Route: `{ path: 'register', component: RegisterComponent, canActivate: [guestGuard] }`
- Add a link on the login page: `<a routerLink="/register">Create an account</a>` (import
  `RouterLink` in `LoginComponent`).

Discussion: the interceptor does not attach a token here (the user is logged out), which is fine
because this endpoint is `permitAll`.

---

## Iteration 8 *(optional)* — "My Private Todos"

The backend already has per-user todos under `/api/v1/todos/protected?userId=...`. Now that we
know who is logged in (`auth.currentUser().id`), we can use them.

Add to `TodoApiService`:

```ts
getMyTodos(userId: number): Observable<Todo[]> {
  return this.http.get<Todo[]>(`${this.apiUrl}/protected`, { params: { userId } });
}

createMyTodo(userId: number, todo: NewTodo): Observable<Todo> {
  return this.http.post<Todo>(`${this.apiUrl}/protected`, todo, { params: { userId } });
}

updateMyTodo(id: number, userId: number, todo: NewTodo): Observable<Todo> {
  return this.http.put<Todo>(`${this.apiUrl}/protected/${id}`, todo, { params: { userId } });
}

deleteMyTodo(id: number, userId: number): Observable<void> {
  return this.http.delete<void>(`${this.apiUrl}/protected/${id}`, { params: { userId } });
}
```

Then build a `pages/my-todos` page the same way as `pages/task` (copy and swap the service calls,
reading `userId` from `AuthService.currentUser()!.id`), add route `/my-todos` with `authGuard`, and a
navbar link.

> 🔎 **Security discussion for students:** the backend trusts the `userId` **query parameter**, not
> the token. A logged-in user can change `userId=1` to `userId=2` and read someone else's todos.
> The correct design: the server takes the user from the JWT (`SecurityContextHolder` /
> `@AuthenticationPrincipal`) and ignores any `userId` from the client.

---

## Backend notes (good follow-up exercises)

| # | Observation in `lab-raj` | Effect | Suggested fix |
|---|---|---|---|
| 1 | No `AuthenticationEntryPoint` | Missing/invalid token → **403** instead of **401** | `http.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))` |
| 2 | `JwtRequestFilter` does not catch JWT exceptions | Expired/tampered token throws inside the filter → 403 or 500 | Wrap `extractUsername` in `try/catch (JwtException e)` and continue without authentication |
| 3 | `jwt.expiration=86400000` in properties | Not used — `JwtUtil` hard-codes 10 hours | Read it with `@Value("${jwt.expiration}")` |
| 4 | `LoginResponse.user` is a `UserDto` | The BCrypt **password hash** is sent to the browser | Return a DTO without `password`, or mark it `@JsonProperty(access = WRITE_ONLY)` |
| 5 | `@ResponseStatus(CREATED)` on `/login` but returns `ResponseEntity.ok` | Status is actually **200** | Remove the annotation (login does not create anything) |
| 6 | `/protected` todos trust `userId` param | Users can read others' todos | Use the user from the JWT |
| 7 | `jwt.secret` is in `application.properties` | Secret committed to git | Use an environment variable in real projects |
| 8 | Old users have plain-text passwords | They cannot log in after BCrypt | Re-create them, or reset their password via `PUT /users/{id}` |

---

## Final file summary

| File | Iteration | Action |
|---|---|---|
| `lab-raj/.../configs/SecurityConfig.java` | 0 | CORS bean, (optional) public `POST /users` |
| `src/app/apis/api.config.ts` | 0 | **New** – `API_BASE_URL` (port 8117) |
| `src/app/apis/todo-api.ts`, `header/header.html` | 0 | Use new base URL / port |
| `src/app/models/auth.model.ts` | 1 | **New** |
| `src/app/apis/auth.ts` (+ spec) | 1 | **New** – `AuthService` |
| `src/app/pages/login/*` | 2, 6 | **New** |
| `src/app/interceptors/auth-interceptor.ts` | 3, 6 | **New** |
| `src/app/app.config.ts` | 3 | `withInterceptors([authInterceptor])` |
| `src/app/guards/auth-guard.ts` | 4 | **New** – `authGuard`, `guestGuard` |
| `src/app/app.routes.ts` | 2, 4 | `/login` route + guards |
| `src/app/navbar/*` | 5 | User name + Logout |
| `src/app/pages/register/*` | 7 | *Optional* |
| `src/app/pages/my-todos/*`, `todo-api.ts` | 8 | *Optional* |
