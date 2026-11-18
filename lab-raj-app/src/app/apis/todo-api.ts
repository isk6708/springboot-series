import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {NewTodo, Todo} from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TodoApiService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8116/api/v1/todos';

  // [ITERATION 2] READ ALL: GET /api/todos
  getAll(): Observable<Todo[]> {
    return this.http.get<Todo[]>(this.apiUrl);
  }

  createTask(todo: NewTodo): Observable<Todo> {
    return this.http.post<Todo>(this.apiUrl, todo);
  }

  updateTask(id: number, todo: NewTodo): Observable<Todo> {
    return this.http.put<Todo>(`${this.apiUrl}/${id}`, todo);
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
  
}
