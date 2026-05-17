import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [ngClass]="toast.type" (click)="toastService.remove(toast.id)">
          <div class="toast-icon">
            @if (toast.type === 'error') { ⚠️ }
            @else if (toast.type === 'success') { ✓ }
            @else { ℹ }
          </div>
          <div class="toast-message">{{ toast.message }}</div>
          <div class="toast-close">×</div>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 24px;
      right: 24px;
      z-index: 10000;
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-width: 400px;
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      border-radius: 12px;
      background: rgba(30, 30, 40, 0.85);
      backdrop-filter: blur(10px);
      -webkit-backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
      color: #fff;
      cursor: pointer;
      animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
      transition: transform 0.2s, opacity 0.2s;
    }
    .toast:hover {
      transform: translateY(-2px);
      border-color: rgba(255, 255, 255, 0.2);
    }
    .toast.success {
      border-left: 4px solid #10b981;
    }
    .toast.error {
      border-left: 4px solid #ef4444;
    }
    .toast.info {
      border-left: 4px solid #3b82f6;
    }
    .toast-icon {
      font-size: 1.2rem;
    }
    .toast-message {
      font-size: 0.9rem;
      font-weight: 500;
      flex-grow: 1;
    }
    .toast-close {
      font-size: 1.2rem;
      opacity: 0.5;
      margin-left: 8px;
    }
    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateX(100px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }
  `]
})
export class ToastContainerComponent {
  toastService = inject(ToastService);
}
