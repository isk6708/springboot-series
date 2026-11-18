import { Component, inject, OnInit, signal } from '@angular/core';
import { NewTodo, Todo } from '../../models/task.model';
import { TodoApiService } from '../../apis/todo-api';
import { TaskFormComponent } from '../task-form/task-form';

@Component({
  selector: 'app-task',
  imports: [TaskFormComponent],
  templateUrl: './task.html',
  styleUrl: './task.css',
})
export class TaskComponent implements OnInit {
  private todoApiService = inject(TodoApiService);

  todoList = signal<Todo[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');

  showForm = signal(false);
  selectedTodo = signal<Todo | null>(null);

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
      },
    });
  }

  openCreateForm(): void {
    this.selectedTodo.set(null);
    this.showForm.set(true);
  }

  openEditForm(todo: Todo): void {
    this.selectedTodo.set(todo);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.selectedTodo.set(null);
  }

  onTodoSaved(savedTodo: Todo): void {
    if (this.selectedTodo()) {
      this.todoList.update((list) =>
        list.map((item) => (item.id === savedTodo.id ? savedTodo : item))
      );
    } else {
      this.todoList.update((list) => [savedTodo, ...list]);
    }
    this.closeForm();
  }

  toggleComplete(todo: Todo): void {
    const updatedPayload: NewTodo = {
      title: todo.title,
      description: todo.description ?? null,
      completed: !todo.completed,
    };

    this.todoApiService.updateTask(todo.id, updatedPayload).subscribe({
      next: (updated) => {
        this.todoList.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item))
        );
      },
      error: (err) => {
        console.error('Failed to toggle todo:', err);
        this.errorMessage.set('Could not update task status.');
      },
    });
  }

  deleteTodo(todo: Todo): void {
    this.todoApiService.deleteTask(todo.id).subscribe({
      next: () => {
        this.todoList.update((list) =>
          list.filter((item) => item.id !== todo.id)
        );
        if (this.selectedTodo()?.id === todo.id) {
          this.closeForm();
        }
      },
      error: (err) => {
        console.error('Failed to delete todo:', err);
        this.errorMessage.set('Could not delete task.');
      },
    });
  }
}
