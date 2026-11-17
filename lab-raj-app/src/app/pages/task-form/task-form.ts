import { Component, inject, output, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Todo,NewTodo } from '../../models/task.model';
import { TodoApiService } from '../../apis/todo-api';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule], // Required for template-driven forms
  templateUrl: './task-form.html',
  styleUrl: './task-form.css',
})
export class TaskFormComponent {
  private todoApiService = inject(TodoApiService);

  // Emits the newly created Todo back to parent TaskComponent
  todoCreated = output<Todo>();
  cancelled = output<void>();

  // Template-driven model fields
  title = '';
  description = '';
  completed = false;

  isSubmitting = signal(false);
  formError = signal('');

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.formError.set('');

    const newTodo: NewTodo = {
      title: this.title.trim(),
      description: this.description.trim(),
      completed: this.completed,
    };

    this.todoApiService.createTask(newTodo).subscribe({
      next: (created) => {
        this.isSubmitting.set(false);
        form.resetForm({ title: '', description: '', completed: false });
        this.todoCreated.emit(created);
      },
      error: (err) => {
        console.error('Failed to create todo:', err);
        this.formError.set('Could not save task. Please try again.');
        this.isSubmitting.set(false);
      },
    });
  }
}
