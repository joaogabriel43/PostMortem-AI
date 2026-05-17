import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface ProjectHistoryItem {
  id: string; // The incident id
  title: string;
  severity: string;
  status: string;
  createdAt: string;
}

interface PageResult<T> {
  data: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

@Component({
  selector: 'app-project-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container project-history-page">
      <div class="back-link">
        <a routerLink="/">← Voltar ao Painel</a>
      </div>

      <div class="header-section">
        <h2>Histórico de Post-Mortems: <span class="accent-text">{{ projectName() }}</span></h2>
        <p class="subtitle">Lista cronológica paginada de todos os incidentes relatados para este projeto.</p>
      </div>

      @if (loading()) {
        <div class="loading-state">
          <span class="spinner"></span> Carregando histórico...
        </div>
      } @else {
        @if (pageResult(); as res) {
          @if (res.data.length > 0) {
            <div class="table-container">
              <table class="history-table">
                <thead>
                  <tr>
                    <th>Severidade</th>
                    <th>Status</th>
                    <th>Título do Post-Mortem</th>
                    <th>Registrado Em</th>
                    <th class="actions-header">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of res.data; track item.id) {
                    <tr>
                      <td>
                        <span class="badge" [ngClass]="item.severity.toLowerCase()">{{ item.severity }}</span>
                      </td>
                      <td>
                        <span class="status-indicator" [ngClass]="item.status.toLowerCase()">
                          {{ item.status }}
                        </span>
                      </td>
                      <td class="pm-title">{{ item.title }}</td>
                      <td class="date-cell">{{ item.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                      <td class="actions-cell">
                        <a [routerLink]="['/incidents', item.id]" class="view-btn">
                          Ver Relatório →
                        </a>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="pagination">
              <button 
                [disabled]="currentPage() === 0" 
                (click)="goToPage(currentPage() - 1)" 
                class="btn btn-nav"
              >
                ← Anterior
              </button>
              
              <span class="page-info">
                Página {{ currentPage() + 1 }} de {{ res.totalPages }} ({{ res.totalElements }} total)
              </span>

              <button 
                [disabled]="currentPage() >= res.totalPages - 1" 
                (click)="goToPage(currentPage() + 1)" 
                class="btn btn-nav"
              >
                Próxima →
              </button>
            </div>
          } @else {
            <div class="empty-state">
              <h3>Nenhum Post-Mortem registrado</h3>
              <p>Nenhum log ou incidente foi relatado para o projeto "{{ projectName() }}" ainda.</p>
              <a routerLink="/incidents/new" class="btn btn-primary margin-top">Relatar Primeiro Incidente</a>
            </div>
          }
        }
      }
    </div>
  `,
  styles: [`
    .project-history-page {
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
    .header-section h2 {
      font-size: 2rem;
      font-weight: 800;
      color: #fff;
      margin-bottom: 8px;
    }
    .accent-text {
      background: var(--primary-glow);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .subtitle {
      color: var(--text-secondary);
      font-size: 0.95rem;
    }
    .loading-state, .empty-state {
      text-align: center;
      padding: 60px 0;
      color: var(--text-secondary);
      font-size: 1.1rem;
      background: rgba(31, 40, 51, 0.4);
      border-radius: 16px;
      border: 1px solid var(--border-light);
      backdrop-filter: blur(10px);
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
    .table-container {
      background: rgba(31, 40, 51, 0.3);
      border: 1px solid var(--border-light);
      border-radius: 16px;
      overflow: hidden;
      backdrop-filter: blur(10px);
    }
    .history-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    .history-table th, .history-table td {
      padding: 18px 24px;
    }
    .history-table th {
      background: rgba(255, 255, 255, 0.02);
      color: #fff;
      font-size: 0.85rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-bottom: 1px solid var(--border-light);
    }
    .history-table td {
      border-bottom: 1px solid var(--border-light);
      color: var(--text-secondary);
      font-size: 0.95rem;
    }
    .history-table tr:last-child td {
      border-bottom: none;
    }
    .badge {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 800;
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
    .status-indicator {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 0.85rem;
      font-weight: 600;
      text-transform: capitalize;
    }
    .status-indicator::before {
      content: '';
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    .status-indicator.resolved {
      color: #10b981;
    }
    .status-indicator.resolved::before {
      background: #10b981;
    }
    .status-indicator.investigating {
      color: #3b82f6;
    }
    .status-indicator.investigating::before {
      background: #3b82f6;
    }
    .pm-title {
      font-weight: 600;
      color: #fff;
    }
    .actions-cell, .actions-header {
      text-align: right;
    }
    .view-btn {
      color: var(--accent-blue);
      font-weight: 600;
      font-size: 0.9rem;
      transition: opacity 0.2s;
    }
    .view-btn:hover {
      opacity: 0.8;
    }
    .pagination {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 16px;
    }
    .page-info {
      color: var(--text-secondary);
      font-size: 0.9rem;
    }
    .btn-nav {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border-light);
      color: #fff;
      padding: 10px 20px;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
      transition: background 0.2s;
    }
    .btn-nav:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.12);
    }
    .btn-nav:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
    .margin-top {
      margin-top: 24px;
    }
    .btn-primary {
      background: var(--primary-glow);
      color: #fff;
      padding: 12px 24px;
      border-radius: 8px;
      font-weight: 600;
      display: inline-block;
      box-shadow: 0 4px 15px rgba(111, 66, 193, 0.4);
    }
  `]
})
export class ProjectHistoryComponent implements OnInit {
  projectName = signal('');
  loading = signal(true);
  pageResult = signal<PageResult<ProjectHistoryItem> | null>(null);
  currentPage = signal(0);
  pageSize = 10;

  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  ngOnInit() {
    const name = this.route.snapshot.paramMap.get('name');
    if (name) {
      this.projectName.set(name);
      this.fetchHistory(this.currentPage());
    }
  }

  fetchHistory(page: number) {
    this.loading.set(true);
    this.http.get<PageResult<ProjectHistoryItem>>(`${environment.apiUrl}/projects/${this.projectName()}/postmortems`, {
      params: {
        page: page.toString(),
        size: this.pageSize.toString()
      }
    }).subscribe({
      next: (res) => {
        this.pageResult.set(res);
        this.currentPage.set(res.currentPage);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  goToPage(page: number) {
    if (page >= 0 && (!this.pageResult() || page < this.pageResult()!.totalPages)) {
      this.fetchHistory(page);
    }
  }
}
