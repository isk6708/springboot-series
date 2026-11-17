import { Component, inject, OnInit, signal } from '@angular/core';
import { Todo } from '../../models/task.model';
import { TodoApiService } from '../../apis/todo-api';

@Component({
  selector: 'app-task',
  imports: [],
  templateUrl: './task.html',
  styleUrl: './task.css',
})
export class TaskComponent implements OnInit {
  private todoApiService = inject(TodoApiService);

  todoList = signal<Todo[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');

  ngOnInit(): void {
    this.loadTodos();
  }

  loadTodos(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.todoApiService.getAll().subscribe({
      next: (data) => {
        this.todoList.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to fetch todos:', err);
        this.errorMessage.set('Could not load todos from server.');
        this.isLoading.set(false);
      }
    });
  }
}
