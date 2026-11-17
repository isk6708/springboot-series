import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Todo} from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TodoApiService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8116/api/v1/todos';

  // [ITERATION 2] READ ALL: GET /api/todos
  getAll(): Observable<Todo[]> {
    return this.http.get<Todo[]>(this.apiUrl);
  }
  
}
