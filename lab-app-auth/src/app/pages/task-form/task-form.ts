import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { NewTodo, Todo } from '../../models/task.model';
import { TodoApiService } from '../../apis/todo-api';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule],
  templateUrl: './task-form.html',
  styleUrl: './task-form.css',
})
export class TaskFormComponent {
  private todoApiService = inject(TodoApiService);

  editingTodo = input<Todo | null>(null);

  todoSaved = output<Todo>();
  cancelled = output<void>();

  title = '';
  description = '';
  completed = false;

  isSubmitting = signal(false);
  formError = signal('');

  constructor() {
    effect(() => {
      const current = this.editingTodo();
      if (current) {
        this.title = current.title;
        this.description = current.description ?? '';
        this.completed = current.completed;
      } else {
        this.title = '';
        this.description = '';
        this.completed = false;
      }
      this.formError.set('');
    });
  }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.formError.set('');

    // Strictly uses NewTodo shape (no id or userId sent to server)
    const payload: NewTodo = {
      title: this.title.trim(),
      description: this.description.trim() || null,
      completed: this.completed,
    };

    const existing = this.editingTodo();
    const request$ = existing
      ? this.todoApiService.updateTask(existing.id, payload)
      : this.todoApiService.createTask(payload);

    request$.subscribe({
      next: (saved) => {
        this.isSubmitting.set(false);
        form.resetForm({ title: '', description: '', completed: false });
        this.todoSaved.emit(saved);
      },
      error: (err) => {
        console.error('Failed to save todo:', err);
        this.formError.set('Could not save task. Please try again.');
        this.isSubmitting.set(false);
      },
    });
  }
}
