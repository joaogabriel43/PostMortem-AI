import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../services/toast.service';

interface PostMortemResponse {
  id: string;
  incidentId: string;
  title: string;
  summary: string;
  timeline: string;
  rootCause: string;
  impact: string;
  detection: string;
  contributingFactors?: string;
  actionItems?: string;
  lessonsLearned?: string;
  createdAt: string;
}

@Component({
  selector: 'app-incident-new',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container new-incident-page">
      <div class="back-link">
        <a routerLink="/">← Voltar ao Painel</a>
      </div>

      <div class="form-card">
        <h2>Relatar Novo Incidente</h2>
        <p class="subtitle">Insira as informações do incidente e os logs correspondentes para que a IA analise e crie o relatório.</p>

        <form (ngSubmit)="submitIncident()" #incidentForm="ngForm">
          <div class="form-group">
            <label for="projectName">Nome do Projeto</label>
            <input 
              type="text" 
              id="projectName" 
              name="projectName" 
              [(ngModel)]="projectName" 
              required 
              placeholder="Ex: PaymentGateway"
              [disabled]="loading()"
            />
          </div>

          <div class="form-group">
            <label for="serviceName">Nome do Serviço</label>
            <input 
              type="text" 
              id="serviceName" 
              name="serviceName" 
              [(ngModel)]="serviceName" 
              required 
              placeholder="Ex: auth-service"
              [disabled]="loading()"
            />
          </div>

          <div class="form-group">
            <label for="rawLog">Logs Brutos de Produção</label>
            <textarea 
              id="rawLog" 
              name="rawLog" 
              [(ngModel)]="rawLog" 
              required 
              rows="10" 
              placeholder="Cole os logs brutos da falha aqui (JSON, Stack Trace ou Plain Text)..."
              [disabled]="loading()"
            ></textarea>
          </div>

          <button 
            type="submit" 
            class="submit-btn" 
            [disabled]="loading() || !incidentForm.form.valid"
          >
            @if (loading()) {
              <span class="spinner"></span> Analisando Logs com IA Resiliente...
            } @else {
              Gerar Post-Mortem Automático
            }
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .new-incident-page {
      max-width: 800px;
      padding-top: 40px;
    }
    .back-link {
      margin-bottom: 24px;
    }
    .back-link a {
      color: var(--accent-blue);
      font-size: 0.95rem;
      font-weight: 500;
      transition: opacity 0.2s;
    }
    .back-link a:hover {
      opacity: 0.8;
    }
    .form-card {
      background: rgba(31, 40, 51, 0.4);
      border: 1px solid var(--border-light);
      border-radius: 16px;
      padding: 40px;
      backdrop-filter: blur(10px);
    }
    .form-card h2 {
      font-size: 1.8rem;
      font-weight: 700;
      margin-bottom: 8px;
      color: #fff;
    }
    .subtitle {
      color: var(--text-secondary);
      font-size: 0.95rem;
      margin-bottom: 32px;
      line-height: 1.5;
    }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 24px;
    }
    .form-group label {
      color: #fff;
      font-size: 0.9rem;
      font-weight: 600;
    }
    .form-group input, .form-group textarea {
      background: rgba(11, 12, 16, 0.6);
      border: 1px solid var(--border-light);
      border-radius: 8px;
      padding: 12px 16px;
      color: #fff;
      font-size: 0.95rem;
      outline: none;
      transition: border-color 0.2s;
      width: 100%;
    }
    .form-group input:focus, .form-group textarea:focus {
      border-color: #6f42c1;
    }
    .form-group input:disabled, .form-group textarea:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .submit-btn {
      width: 100%;
      background: var(--primary-glow);
      color: #fff;
      padding: 14px;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 700;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      box-shadow: 0 4px 15px rgba(111, 66, 193, 0.4);
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .submit-btn:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(111, 66, 193, 0.6);
    }
    .submit-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      box-shadow: none;
    }
    .spinner {
      width: 20px;
      height: 20px;
      border: 3px solid rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      border-top-color: #fff;
      animation: spin 1s ease-in-out infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class IncidentNewComponent {
  projectName = '';
  serviceName = '';
  rawLog = '';

  loading = signal(false);

  private http = inject(HttpClient);
  private router = inject(Router);
  private toastService = inject(ToastService);

  submitIncident() {
    if (!this.projectName || !this.serviceName || !this.rawLog) return;

    this.loading.set(true);

    const payload = {
      projectName: this.projectName,
      serviceName: this.serviceName,
      rawLog: this.rawLog
    };

    this.http.post<PostMortemResponse>(`${environment.apiUrl}/incidents`, payload)
      .subscribe({
        next: (response) => {
          this.loading.set(false);
          this.toastService.show('Post-Mortem gerado com sucesso!', 'success');
          // Navigate to details screen using the incidentId
          this.router.navigate(['/incidents', response.incidentId]);
        },
        error: (err) => {
          this.loading.set(false);
          // Handled by HTTP error interceptor, showing toast automatically
        }
      });
  }
}
