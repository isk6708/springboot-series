import { Component,inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Todo } from '../../models/task.model';
import { TodoApiService } from '../../apis/todo-api';

@Component({
  selector: 'app-task',
  imports: [CommonModule],
  templateUrl: './task.html',
  styleUrl: './task.css',
})
export class TaskComponent implements OnInit {
  private todoApiService = inject(TodoApiService);

  todoList: Todo[] = [];
  isLoading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadTodos();
  }

  loadTodos(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.todoApiService.getAll().subscribe({
      next: (data) => {
        this.todoList = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to fetch todos:', err);
        this.errorMessage = 'Could not load todos from server.';
        this.isLoading = false;
      }
    });
  }
}
