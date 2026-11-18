# UserGuide.md — Enabling the Users Endpoints in `lab-raj-app`

This guide shows how to connect the **Users** page of the Angular app (`lab-raj-app`) to the
`/api/v1/users` endpoints of the Spring Boot app (`lab-raj`, port **8116**).

It follows **exactly the same pattern** we already used for Todos:

| Layer | Todo (done) | User (this guide) |
|---|---|---|
| Model (interfaces) | `models/task.model.ts` | `models/user.model.ts` |
| API service | `apis/todo-api.ts` → `TodoApiService` | `apis/user-api.ts` → `UserApiService` |
| List page | `pages/task/` → `TaskComponent` | `pages/user/` → `UserComponent` |
| Form (create/edit) | `pages/task-form/` → `TaskFormComponent` | `pages/user-form/` → `UserFormComponent` |
| Route | `/todos` | `/users` (already exists) |
| Navbar link | "My Todos" | "Users" (already exists) |

> The route `/users` and the navbar link already exist. The `UserComponent` is still the
> placeholder (`<p>user works!</p>`). We will replace it.

---

## 1. Know the backend API first

From `lab-raj/src/main/java/com/heitech/lab_raj/controllers/UserController.java`:

| Action | Method & URL | Body | Response |
|---|---|---|---|
| List / search | `GET /api/v1/users?keyword=xxx` (keyword optional) | – | `200` `UserDto[]` |
| Create | `POST /api/v1/users` | `UserDto` | `201` `UserDto` |
| Update | `PUT /api/v1/users/{id}` | `UserDto` | `200` `UserDto` |
| Delete | `DELETE /api/v1/users/{id}` | – | `204` (no content) |

`UserDto` fields and validation (`dtos/UserDto.java`):

| Field | Type | Rule |
|---|---|---|
| `id` | Integer | set by server |
| `fullname` | String | **required** |
| `email` | String | **required**, valid email, **must be unique** (else `409 Conflict`) |
| `password` | String | **required**, **min 6 characters** (on create **and** update) |

Things to remember:

- There is **no** `GET /api/v1/users/{id}` endpoint — the list gives us everything we need.
- `password` is `@NotBlank` in the DTO, so **every PUT must send a password too**, even if the
  user only changed the name. (The service would accept an empty password, but the DTO
  validation rejects it first with `400 Bad Request`.)
- Duplicate email → `409 Conflict` with message `Email already registered: ...`.

---

## 2. Step 0 — Allow the Angular app to call `/api/v1/users` (CORS)

`TodoController` has a `@CrossOrigin(...)` annotation, **`UserController` does not**.
Without it the browser blocks every request from `http://localhost:4200` and you will see
a CORS error in the DevTools console.

Open `lab-raj/src/main/java/com/heitech/lab_raj/controllers/UserController.java` and copy the
same annotation from `TodoController`, placed above `@RestController`:

```java
@CrossOrigin(
    origins = "http://localhost:4200",
    methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH,
        RequestMethod.OPTIONS
    },
    allowedHeaders = "*"
)
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
```

`RequestMethod` and `CrossOrigin` are already covered by
`import org.springframework.web.bind.annotation.*;`.

Restart the Spring Boot app, then quickly test with Swagger (`http://localhost:8116/swagger-ui.html`)
or curl:

```bash
curl http://localhost:8116/api/v1/users
```

---

## 3. Step 1 — Model: `src/app/models/user.model.ts`

Create a new file (same idea as `task.model.ts`: one interface for what we **receive**, one for
what we **send**).

```ts
// [ITERATION 1] Data shape matching Spring Boot UserDto (GET /api/v1/users)
//   - id is an Integer, so it arrives as a number
//   - password is also returned by the API (see section 9) – we never display it
export interface User {
  id: number;
  fullname: string;
  email: string;
  password?: string;
}

// [ITERATION 2] Payload for CREATE (POST) and UPDATE (PUT). id is set by the server.
//   - password is REQUIRED (min 6 chars) on both POST and PUT because of UserDto validation
export interface NewUser {
  fullname: string;
  email: string;
  password: string;
}
```

---

## 4. Step 2 — API service: `src/app/apis/user-api.ts`

Generate it with the CLI (from the `lab-raj-app` folder):

```bash
ng generate service apis/user-api
```

Replace the generated content with the code below (same structure as `TodoApiService`):

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NewUser, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8116/api/v1/users';

  // READ ALL (optionally filtered): GET /api/v1/users?keyword=...
  getAll(keyword?: string): Observable<User[]> {
    let params = new HttpParams();
    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    return this.http.get<User[]>(this.apiUrl, { params });
  }

  // CREATE: POST /api/v1/users
  createUser(user: NewUser): Observable<User> {
    return this.http.post<User>(this.apiUrl, user);
  }

  // UPDATE: PUT /api/v1/users/{id}
  updateUser(id: number, user: NewUser): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, user);
  }

  // DELETE: DELETE /api/v1/users/{id}
  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

> The CLI also creates `user-api.spec.ts` that imports `UserApi`. Rename the import/usage in that
> spec to `UserApiService` (same as you did in `todo-api.spec.ts`), otherwise `ng test` will fail.

`provideHttpClient()` is already registered in `app.config.ts`, so nothing else is needed there.

---

## 5. Step 3 — Form component: `src/app/pages/user-form/`

Generate it:

```bash
ng generate component pages/user-form
```

### 5.1 `user-form.ts`

Same structure as `TaskFormComponent`: an `input()` for the user being edited, two `output()`s,
template-driven form fields, and an `effect()` to fill / clear the form.

```ts
import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { NewUser, User } from '../../models/user.model';
import { UserApiService } from '../../apis/user-api';

@Component({
  selector: 'app-user-form',
  imports: [FormsModule],
  templateUrl: './user-form.html',
  styleUrl: './user-form.css',
})
export class UserFormComponent {
  private userApiService = inject(UserApiService);

  editingUser = input<User | null>(null);

  userSaved = output<User>();
  cancelled = output<void>();

  fullname = '';
  email = '';
  password = '';

  isSubmitting = signal(false);
  formError = signal('');

  constructor() {
    effect(() => {
      const current = this.editingUser();
      if (current) {
        this.fullname = current.fullname;
        this.email = current.email;
      } else {
        this.fullname = '';
        this.email = '';
      }
      // Never pre-fill the password – the user must type it again (required by UserDto)
      this.password = '';
      this.formError.set('');
    });
  }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.formError.set('');

    // Strictly uses NewUser shape (no id sent to server)
    const payload: NewUser = {
      fullname: this.fullname.trim(),
      email: this.email.trim(),
      password: this.password,
    };

    const existing = this.editingUser();
    const request$ = existing
      ? this.userApiService.updateUser(existing.id, payload)
      : this.userApiService.createUser(payload);

    request$.subscribe({
      next: (saved) => {
        this.isSubmitting.set(false);
        form.resetForm({ fullname: '', email: '', password: '' });
        this.userSaved.emit(saved);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Failed to save user:', err);
        if (err.status === 409) {
          this.formError.set('This email is already registered.');
        } else if (err.status === 400) {
          this.formError.set('Please check the fields (valid email, password min 6 characters).');
        } else {
          this.formError.set('Could not save user. Please try again.');
        }
        this.isSubmitting.set(false);
      },
    });
  }
}
```

### 5.2 `user-form.html`

```html
<form #userForm="ngForm" (ngSubmit)="onSubmit(userForm)" class="task-form-card" novalidate>
  <h3 class="form-title">{{ editingUser() ? 'Edit User' : 'New User' }}</h3>

  @if (formError()) {
    <div class="form-error">{{ formError() }}</div>
  }

  <div class="form-group">
    <label for="fullname">Full name <span class="required">*</span></label>
    <input
      id="fullname"
      name="fullname"
      type="text"
      required
      minlength="2"
      [(ngModel)]="fullname"
      #fullnameInput="ngModel"
      placeholder="e.g., Ahmad bin Ali"
    />
    @if (fullnameInput.invalid && (fullnameInput.dirty || fullnameInput.touched)) {
      <small class="validation-msg">Full name is required (at least 2 characters).</small>
    }
  </div>

  <div class="form-group">
    <label for="email">Email <span class="required">*</span></label>
    <input
      id="email"
      name="email"
      type="email"
      required
      email
      [(ngModel)]="email"
      #emailInput="ngModel"
      placeholder="e.g., ahmad@example.com"
    />
    @if (emailInput.invalid && (emailInput.dirty || emailInput.touched)) {
      <small class="validation-msg">A valid email is required.</small>
    }
  </div>

  <div class="form-group">
    <label for="password">
      Password <span class="required">*</span>
      @if (editingUser()) { <small>(re-enter to save changes)</small> }
    </label>
    <input
      id="password"
      name="password"
      type="password"
      required
      minlength="6"
      [(ngModel)]="password"
      #passwordInput="ngModel"
      placeholder="At least 6 characters"
      autocomplete="new-password"
    />
    @if (passwordInput.invalid && (passwordInput.dirty || passwordInput.touched)) {
      <small class="validation-msg">Password is required (at least 6 characters).</small>
    }
  </div>

  <div class="form-actions">
    <button type="button" class="btn-cancel" (click)="cancelled.emit()">
      Cancel
    </button>
    <button
      type="submit"
      class="btn-submit"
      [disabled]="userForm.invalid || isSubmitting()"
    >
      {{ isSubmitting() ? 'Saving...' : 'Save User' }}
    </button>
  </div>
</form>
```

### 5.3 `user-form.css`

The template above reuses the **same CSS class names** as the task form, so simply copy the
styles:

```bash
# Windows (cmd), from lab-raj-app
copy src\app\pages\task-form\task-form.css src\app\pages\user-form\user-form.css
```

---

## 6. Step 4 — List page: `src/app/pages/user/`

The component already exists. Replace its three files.

### 6.1 `user.ts`

```ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { User } from '../../models/user.model';
import { UserApiService } from '../../apis/user-api';
import { UserFormComponent } from '../user-form/user-form';

@Component({
  selector: 'app-user',
  imports: [FormsModule, UserFormComponent],
  templateUrl: './user.html',
  styleUrl: './user.css',
})
export class UserComponent implements OnInit {
  private userApiService = inject(UserApiService);

  userList = signal<User[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');

  showForm = signal(false);
  selectedUser = signal<User | null>(null);

  keyword = '';

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.userApiService.getAll(this.keyword).subscribe({
      next: (data) => {
        this.userList.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to fetch users:', err);
        this.errorMessage.set('Could not load users from server.');
        this.isLoading.set(false);
      },
    });
  }

  clearSearch(): void {
    this.keyword = '';
    this.loadUsers();
  }

  openCreateForm(): void {
    this.selectedUser.set(null);
    this.showForm.set(true);
  }

  openEditForm(user: User): void {
    this.selectedUser.set(user);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.selectedUser.set(null);
  }

  onUserSaved(savedUser: User): void {
    if (this.selectedUser()) {
      this.userList.update((list) =>
        list.map((item) => (item.id === savedUser.id ? savedUser : item))
      );
    } else {
      this.userList.update((list) => [savedUser, ...list]);
    }
    this.closeForm();
  }

  deleteUser(user: User): void {
    if (!confirm(`Delete user "${user.fullname}"?`)) {
      return;
    }

    this.userApiService.deleteUser(user.id).subscribe({
      next: () => {
        this.userList.update((list) => list.filter((item) => item.id !== user.id));
        if (this.selectedUser()?.id === user.id) {
          this.closeForm();
        }
      },
      error: (err) => {
        console.error('Failed to delete user:', err);
        this.errorMessage.set('Could not delete user.');
      },
    });
  }
}
```

### 6.2 `user.html`

```html
<section class="task-container">
  <div class="task-header">
    <div>
      <h2>Users</h2>
      <p class="task-subtitle">Users synced from Spring Boot API</p>
    </div>

    <div class="header-actions">
      <button type="button" class="btn-add" (click)="showForm() ? closeForm() : openCreateForm()">
        {{ showForm() ? '✕ Close Form' : '+ Add User' }}
      </button>

      <button type="button" class="btn-refresh" (click)="loadUsers()" [disabled]="isLoading()">
        {{ isLoading() ? 'Loading...' : '↻ Refresh' }}
      </button>
    </div>
  </div>

  <!-- Search (GET /api/v1/users?keyword=...) -->
  <form class="search-bar" (ngSubmit)="loadUsers()">
    <input
      name="keyword"
      type="search"
      [(ngModel)]="keyword"
      placeholder="Search by name or email..."
    />
    <button type="submit" class="btn-refresh">Search</button>
    @if (keyword) {
      <button type="button" class="btn-refresh" (click)="clearSearch()">Clear</button>
    }
  </form>

  @if (showForm()) {
    <app-user-form
      [editingUser]="selectedUser()"
      (userSaved)="onUserSaved($event)"
      (cancelled)="closeForm()"
    />
  }

  @if (isLoading()) {
    <div class="state-box loading">
      <span class="spinner" aria-hidden="true"></span>
      <span>Loading users...</span>
    </div>
  } @else if (errorMessage()) {
    <div class="state-box error" role="alert">
      <strong>Error:</strong> {{ errorMessage() }}
    </div>
  } @else {
    <ul class="todo-list">
      @for (user of userList(); track user.id) {
        <li class="todo-card">
          <div class="todo-main">
            <div class="todo-info">
              <h3 class="todo-title">{{ user.fullname }}</h3>
              <p class="todo-desc">{{ user.email }}</p>
            </div>
          </div>

          <div class="todo-actions">
            <span class="status-pill">#{{ user.id }}</span>

            <button type="button" class="btn-icon edit" (click)="openEditForm(user)" title="Edit user">
              ✎ Edit
            </button>

            <button type="button" class="btn-icon delete" (click)="deleteUser(user)" title="Delete user">
              🗑 Delete
            </button>
          </div>
        </li>
      } @empty {
        <li class="state-box empty">
          No users found. Click <strong>+ Add User</strong> above to create the first user!
        </li>
      }
    </ul>
  }
</section>
```

> Notice: we **never** show `user.password` in the list.

### 6.3 `user.css`

Reuse the task page styles, then add a small block for the search bar:

```bash
# Windows (cmd), from lab-raj-app
copy src\app\pages\task\task.css src\app\pages\user\user.css
```

Append at the end of `user.css`:

```css
.search-bar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.search-bar input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ccc;
  border-radius: 6px;
}
```

---

## 7. Step 5 — Routes and navbar

Nothing to change — already in place:

- `app.routes.ts` → `{ path: 'users', component: UserComponent }`
- `navbar.html` → `<a routerLink="/users" ...>Users</a>`

---

## 8. Run and test

1. Start Spring Boot (`lab-raj`) → `http://localhost:8116`
2. Start Angular (`lab-raj-app`):
   ```bash
   npm start
   ```
3. Open `http://localhost:4200/users` and check:

| Test | Expected |
|---|---|
| Page loads | List of users (or the empty message) |
| **+ Add User** with valid data | New card appears on top (`201`) |
| Add user with an email that already exists | "This email is already registered." (`409`) |
| Password with less than 6 chars | Save button disabled / validation message |
| **✎ Edit** → change name → re-enter password → Save | Card updated (`200`) |
| **🗑 Delete** → confirm | Card removed (`204`) |
| **↻ Refresh** | Reloads from server |
| Search | Filters list (see note in section 9) |

Troubleshooting:

| Symptom | Cause / Fix |
|---|---|
| Console: `blocked by CORS policy` | Step 0 not done, or Spring Boot not restarted |
| `Could not load users from server.` | Backend not running on port 8116 |
| `400` on Save when editing | Password left empty — it is required on PUT |
| `NullInjectorError: No provider for HttpClient` | `provideHttpClient()` missing in `app.config.ts` |

---

## 9. Known backend behaviour (good discussion points for class)

These are not Angular problems — they are in `lab-raj` and are worth fixing as a follow-up exercise:

1. **Search uses AND, not OR.**
   `UserService.getAllUsers` calls
   `findByFullnameContainingIgnoreCaseAndEmailContainingIgnoreCase(keyword, keyword)`,
   so a keyword only matches users whose **name and email both** contain it.
   To search by name **or** email, rename the repository method to
   `findByFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase` and update the service call.

2. **Password is returned in GET responses and stored as plain text.**
   `UserDto` includes `password`, so the list API sends it back to the browser, and the
   `PasswordEncoder` lines in `UserService` are commented out.
   Fix ideas: add `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` on `password` in
   `UserDto`, and enable BCrypt hashing (`spring-security-crypto` + `PasswordEncoder` bean).

3. **Password required on every update.**
   `UserDto.password` is `@NotBlank`, even though `UserService.updateUser` is written to keep the
   old password when none is sent. To allow "leave blank to keep current password", use a separate
   update DTO (or validation groups) without `@NotBlank` on password, then make the Angular
   password field optional when `editingUser()` is set.

4. **Deleting a user that owns private todos.**
   `todoRepository.deleteByUser(user)` is commented out in `deleteUser`, so todos with that
   `user_id` are left behind (or the delete fails if foreign keys are enforced).

---

## 10. Summary of files

| File | Action |
|---|---|
| `lab-raj/.../controllers/UserController.java` | **Edit** – add `@CrossOrigin` |
| `src/app/models/user.model.ts` | **New** |
| `src/app/apis/user-api.ts` (+ spec) | **New** (via `ng g s`) |
| `src/app/pages/user-form/user-form.ts/.html/.css` (+ spec) | **New** (via `ng g c`) |
| `src/app/pages/user/user.ts/.html/.css` | **Replace** placeholder |
| `app.routes.ts`, `navbar.html` | No change |
