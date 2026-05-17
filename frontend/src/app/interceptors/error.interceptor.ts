import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'Ocorreu um erro inesperado.';

      if (error.error) {
        const problemDetail = error.error;
        if (problemDetail.detail) {
          errorMessage = problemDetail.detail;
        } else if (problemDetail.title) {
          errorMessage = problemDetail.title;
        } else if (typeof problemDetail === 'string') {
          try {
            const parsed = JSON.parse(problemDetail);
            errorMessage = parsed.detail || parsed.title || errorMessage;
          } catch (e) {
            errorMessage = problemDetail;
          }
        }
      } else if (error.message) {
        errorMessage = error.message;
      }

      toastService.show(errorMessage, 'error');
      return throwError(() => error);
    })
  );
};
