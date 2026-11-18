import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../apis/auth-api';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // Pre-fill the email when we come back from the Register page (Iteration 3)
  email = this.route.snapshot.queryParamMap.get('email') ?? '';
  password = '';

  justRegistered = this.route.snapshot.queryParamMap.get('registered') === '1';

  isSubmitting = signal(false);
  formError = signal('');

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.formError.set('');

    this.auth.login({ email: this.email.trim(), password: this.password }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/todos';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Login failed:', err);
        this.formError.set(
          err.status === 401
            ? 'Incorrect email or password.'
            : 'Cannot reach the server. Is Spring Boot running on port 8117?'
        );
        this.isSubmitting.set(false);
      },
    });
  }
}