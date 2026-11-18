import { Routes } from '@angular/router';
import { TaskComponent } from './pages/task/task';
import { UserComponent } from './pages/user/user';
import { LoginComponent } from './pages/login/login';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'todos', pathMatch: 'full' },
  { path: 'todos', component: TaskComponent },
  { path: 'users', component: UserComponent },
  { path: '**', redirectTo: 'todos' }
];
