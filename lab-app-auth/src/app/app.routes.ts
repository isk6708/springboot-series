import { Routes } from '@angular/router';
import { TaskComponent } from './pages/task/task';
import { UserComponent } from './pages/user/user';
import { LoginComponent } from './pages/login/login';
import { RegisterComponent } from './pages/register/register';
import { authGuard, guestGuard } from './guards/auth-guard';


export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  // { path: '', redirectTo: 'todos', pathMatch: 'full' },
  { path: 'todos', component: TaskComponent, canActivate: [authGuard] },
  { path: 'users', component: UserComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'todos' }
];
