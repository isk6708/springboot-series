import { Routes } from '@angular/router';
import { TaskComponent } from './pages/task/task';
import { UserComponent } from './pages/user/user';
import { LoginComponent } from './pages/login/login';
import { RegisterComponent } from './pages/register/register';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  // { path: '', redirectTo: 'todos', pathMatch: 'full' },
  { path: 'todos', component: TaskComponent },
  { path: 'users', component: UserComponent },
  { path: '**', redirectTo: 'todos' }
];
