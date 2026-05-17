import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../../services/toast.service';

interface PostMortemResponse {
  id: string;
  incidentId: string;
  severity: string;
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
  selector: 'app-incident-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container incident-detail-page">
      <div class="back-link">
        <a routerLink="/">← Voltar ao Painel</a>
      </div>

      @if (loading()) {
        <div class="loading-state">
          <span class="spinner"></span> Carregando detalhes do Post-Mortem...
        </div>
      } @else if (postMortem()) {
        @let pm = postMortem()!;
        <div class="postmortem-header">
          <div class="title-section">
            <span class="badge" [ngClass]="pm.severity.toLowerCase()">{{ pm.severity }}</span>
            <h1>{{ pm.title }}</h1>
          </div>
          <div class="actions">
            <button (click)="download('markdown')" class="btn btn-secondary btn-icon">
              📥 Exportar MD
            </button>
            <button (click)="download('pdf')" class="btn btn-primary btn-icon">
              📥 Exportar PDF
            </button>
          </div>
        </div>

        <div class="postmortem-grid">
          <div class="main-card">
            <h2>Resumo Executivo</h2>
            <p class="section-content">{{ pm.summary }}</p>
          </div>

          <div class="side-grid">
            <div class="info-card">
              <h4>Metadados</h4>
              <div class="meta-item">
                <span class="meta-label">ID do Incidente:</span>
                <span class="meta-value">{{ pm.incidentId }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Criado Em:</span>
                <span class="meta-value">{{ pm.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="sections-container">
          <div class="section-card">
            <h2>Cronologia dos Fatos (Timeline)</h2>
            <div class="section-content whitespace-pre">{{ pm.timeline }}</div>
          </div>

          <div class="section-card">
            <h2>Causa Raiz (Root Cause)</h2>
            <div class="section-content whitespace-pre">{{ pm.rootCause }}</div>
          </div>

          <div class="section-card">
            <h2>Impacto (Impact Description)</h2>
            <div class="section-content whitespace-pre">{{ pm.impact }}</div>
          </div>

          <div class="section-card">
            <h2>Estratégia de Detecção</h2>
            <div class="section-content whitespace-pre">{{ pm.detection }}</div>
          </div>

          @if (pm.contributingFactors) {
            <div class="section-card">
              <h2>Fatores Contribuintes</h2>
              <div class="section-content whitespace-pre">{{ pm.contributingFactors }}</div>
            </div>
          }

          @if (pm.actionItems) {
            <div class="section-card">
              <h2>Plano de Ações Corretivas</h2>
              <div class="section-content whitespace-pre">{{ pm.actionItems }}</div>
            </div>
          }

          @if (pm.lessonsLearned) {
            <div class="section-card">
              <h2>Lições Aprendidas</h2>
              <div class="section-content whitespace-pre">{{ pm.lessonsLearned }}</div>
            </div>
          }
        </div>
      } @else {
        <div class="error-state">
          <h3>Post-Mortem não encontrado</h3>
          <p>O relatório solicitado não pôde ser carregado.</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .incident-detail-page {
      padding-top: 40px;
      display: flex;
      flex-direction: column;
      gap: 32px;
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
    .loading-state, .error-state {
      text-align: center;
      padding: 60px 0;
      color: var(--text-secondary);
      font-size: 1.1rem;
      background: rgba(31, 40, 51, 0.4);
      border-radius: 16px;
      border: 1px solid var(--border-light);
    }
    .spinner {
      display: inline-block;
      width: 20px;
      height: 20px;
      border: 3px solid rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      border-top-color: #fff;
      animation: spin 1s ease-in-out infinite;
      margin-right: 12px;
      vertical-align: middle;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .postmortem-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 24px;
      border-bottom: 1px solid var(--border-light);
      padding-bottom: 24px;
    }
    .title-section {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .title-section h1 {
      font-size: 2rem;
      font-weight: 800;
      color: #fff;
    }
    .badge {
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .badge.p1 {
      background: rgba(239, 68, 68, 0.2);
      color: #ef4444;
      border: 1px solid rgba(239, 68, 68, 0.4);
    }
    .badge.p2 {
      background: rgba(245, 158, 11, 0.2);
      color: #f59e0b;
      border: 1px solid rgba(245, 158, 11, 0.4);
    }
    .badge.p3 {
      background: rgba(234, 179, 8, 0.2);
      color: #eab308;
      border: 1px solid rgba(234, 179, 8, 0.4);
    }
    .badge.p4 {
      background: rgba(59, 130, 246, 0.2);
      color: #3b82f6;
      border: 1px solid rgba(59, 130, 246, 0.4);
    }
    .actions {
      display: flex;
      gap: 16px;
    }
    .btn {
      padding: 12px 20px;
      border-radius: 8px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      border: none;
    }
    .btn-primary {
      background: var(--primary-glow);
      color: #fff;
      box-shadow: 0 4px 15px rgba(111, 66, 193, 0.4);
    }
    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(111, 66, 193, 0.6);
    }
    .btn-secondary {
      background: rgba(255, 255, 255, 0.08);
      color: #fff;
      border: 1px solid var(--border-light);
    }
    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.15);
    }
    .postmortem-grid {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 32px;
    }
    @media (max-width: 768px) {
      .postmortem-grid {
        grid-template-columns: 1fr;
      }
    }
    .main-card, .section-card, .info-card {
      background: rgba(31, 40, 51, 0.3);
      border: 1px solid var(--border-light);
      border-radius: 16px;
      padding: 32px;
      backdrop-filter: blur(10px);
    }
    .main-card h2, .section-card h2 {
      font-size: 1.4rem;
      font-weight: 700;
      color: #fff;
      margin-bottom: 16px;
      border-left: 4px solid #6f42c1;
      padding-left: 12px;
    }
    .section-content {
      color: var(--text-secondary);
      line-height: 1.6;
      font-size: 0.95rem;
    }
    .whitespace-pre {
      white-space: pre-wrap;
    }
    .info-card h4 {
      font-size: 1.1rem;
      color: #fff;
      margin-bottom: 20px;
      border-bottom: 1px solid var(--border-light);
      padding-bottom: 10px;
    }
    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin-bottom: 16px;
    }
    .meta-label {
      color: var(--text-secondary);
      font-size: 0.8rem;
      font-weight: 600;
      opacity: 0.7;
    }
    .meta-value {
      color: #fff;
      font-size: 0.9rem;
      font-family: monospace;
      word-break: break-all;
    }
    .sections-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
  `]
})
export class IncidentDetailComponent implements OnInit {
  loading = signal(true);
  postMortem = signal<PostMortemResponse | null>(null);

  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private toastService = inject(ToastService);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.http.get<PostMortemResponse>(`${environment.apiUrl}/incidents/${id}/postmortem`)
        .subscribe({
          next: (res) => {
            this.postMortem.set(res);
            this.loading.set(false);
          },
          error: () => {
            this.loading.set(false);
          }
        });
    }
  }

  download(format: 'markdown' | 'pdf') {
    const pm = this.postMortem();
    if (!pm) return;

    this.toastService.show(`Iniciando download do ${format === 'pdf' ? 'PDF' : 'Markdown'}...`, 'info');

    this.http.get(`${environment.apiUrl}/incidents/${pm.incidentId}/postmortem/export`, {
      params: { format },
      responseType: 'blob',
      observe: 'response'
    }).subscribe({
      next: (response: HttpResponse<Blob>) => {
        const blob = response.body;
        if (!blob) {
          this.toastService.show('Erro ao descarregar arquivo (corpo vazio).', 'error');
          return;
        }

        let filename = `postmortem-${pm.incidentId}.${format === 'pdf' ? 'pdf' : 'md'}`;
        const contentDisposition = response.headers.get('Content-Disposition');
        if (contentDisposition) {
          const matches = /filename="?([^"]+)"?/g.exec(contentDisposition);
          if (matches && matches[1]) {
            filename = matches[1];
          }
        }

        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        this.toastService.show('Download concluído!', 'success');
      },
      error: () => {
        // Displayed automatically by HTTP Interceptor
      }
    });
  }
}
