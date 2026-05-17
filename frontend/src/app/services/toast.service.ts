import { Injectable, signal } from '@angular/core';

export interface Toast {
  message: string;
  type: 'success' | 'error' | 'info';
  id: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private nextId = 0;
  toasts = signal<Toast[]>([]);

  show(message: string, type: 'success' | 'error' | 'info' = 'info') {
    const id = this.nextId++;
    const newToast: Toast = { message, type, id };
    this.toasts.update(current => [...current, newToast]);

    setTimeout(() => {
      this.remove(id);
    }, 5000);
  }

  remove(id: number) {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }
}
