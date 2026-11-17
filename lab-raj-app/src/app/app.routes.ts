import { Routes } from '@angular/router';
import { TaskComponent } from './pages/task/task';

export const routes: Routes = [
  { path: '', redirectTo: 'todos', pathMatch: 'full' },
  { path: 'todos', component: TaskComponent },
  { path: 'users', component: TaskComponent },
  { path: '**', redirectTo: 'todos' }
];
