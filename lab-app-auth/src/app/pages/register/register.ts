import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../apis/auth-api';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class RegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  fullname = '';
  email = '';
  password = '';
  confirmPassword = '';

  isSubmitting = signal(false);
  formError = signal('');

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    // Client-side only check – the backend does not know about "confirm password"
    if (this.password !== this.confirmPassword) {
      this.formError.set('Passwords do not match.');
      return;
    }

    this.isSubmitting.set(true);
    this.formError.set('');

    this.auth
      .register({
        fullname: this.fullname.trim(),
        email: this.email.trim(),
        password: this.password,
      })
      .subscribe({
        next: (user) => {
          this.isSubmitting.set(false);
          // Go to Login, show "Account created" and pre-fill the email
          this.router.navigate(['/login'], {
            queryParams: { registered: 1, email: user.email },
          });
        },
        error: (err: HttpErrorResponse) => {
          console.error('Register failed:', err);
          if (err.status === 409) {
            this.formError.set('This email is already registered. Try signing in.');
          } else if (err.status === 400) {
            this.formError.set('Please check the fields (valid email, password min 6 characters).');
          } else {
            this.formError.set('Cannot reach the server. Is Spring Boot running on port 8117?');
          }
          this.isSubmitting.set(false);
        },
      });
  }
}