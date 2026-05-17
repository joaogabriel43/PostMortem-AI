import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="container hero-section">
      <div class="hero-content">
        <h1 class="hero-title">Inteligência Artificial Ativa para <span class="accent-glow">Resiliência de Sistemas</span></h1>
        <p class="hero-subtitle">
          Automatize a geração de relatórios de Post-Mortem a partir de logs crus de produção, 
          mitigue falhas futuras e blinde seu ecossistema contra incidentes recorrentes utilizando SRE cognitivo.
        </p>
        <div class="hero-actions">
          <a routerLink="/incidents/new" class="btn btn-primary">Registrar Incidente</a>
          <div class="search-box">
            <input 
              type="text" 
              placeholder="Digite o nome de um projeto (ex: PaymentGateway)" 
              [(ngModel)]="projectName" 
              (keyup.enter)="searchProject()"
            />
            <button (click)="searchProject()" class="btn btn-secondary">Buscar Histórico</button>
          </div>
        </div>
      </div>

      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon">🔍</div>
          <h3>Parser Multiformato</h3>
          <p>Strategy Pattern inteligente para detecção e parsing nativo de Logs JSON, Plain Text e Java Stack Traces.</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🤖</div>
          <h3>Engenharia de Prompt</h3>
          <p>Mitigação ativa de erro de atribuição superficial (Surface Attribution Error) com busca profunda por causa raiz.</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🛡️</div>
          <h3>Privacidade & Segurança</h3>
          <p>Sanitização nativa e supressão de injeções de script (OWASP) via rendering seguro Flexmark nos relatórios.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .hero-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 60px;
      padding-top: 60px;
    }
    .hero-content {
      max-width: 800px;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .hero-title {
      font-size: 3.5rem;
      font-weight: 800;
      line-height: 1.15;
      letter-spacing: -1.5px;
    }
    .accent-glow {
      background: linear-gradient(135deg, #45f3ff 0%, #a020f0 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .hero-subtitle {
      color: var(--text-secondary);
      font-size: 1.25rem;
      line-height: 1.6;
      font-weight: 400;
    }
    .hero-actions {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
      margin-top: 16px;
    }
    .btn {
      padding: 14px 28px;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s, background 0.2s;
      border: none;
    }
    .btn-primary {
      background: var(--primary-glow);
      color: #fff;
      box-shadow: 0 4px 20px rgba(111, 66, 193, 0.4);
    }
    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(111, 66, 193, 0.6);
    }
    .search-box {
      display: flex;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-light);
      padding: 6px;
      border-radius: 12px;
      width: 100%;
      max-width: 500px;
      backdrop-filter: blur(10px);
    }
    .search-box input {
      flex-grow: 1;
      background: transparent;
      border: none;
      padding: 12px 16px;
      color: #fff;
      font-size: 0.95rem;
      outline: none;
      min-width: 0;
    }
    .search-box input::placeholder {
      color: rgba(255, 255, 255, 0.4);
    }
    .btn-secondary {
      background: rgba(255, 255, 255, 0.08);
      color: #fff;
      padding: 10px 20px;
    }
    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.15);
    }
    .features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 32px;
      width: 100%;
    }
    .feature-card {
      background: rgba(31, 40, 51, 0.4);
      border: 1px solid var(--border-light);
      border-radius: 16px;
      padding: 32px;
      text-align: left;
      backdrop-filter: blur(10px);
      transition: transform 0.2s, border-color 0.2s;
    }
    .feature-card:hover {
      transform: translateY(-4px);
      border-color: rgba(255, 255, 255, 0.15);
    }
    .feature-icon {
      font-size: 2.5rem;
      margin-bottom: 16px;
    }
    .feature-card h3 {
      font-size: 1.3rem;
      font-weight: 700;
      margin-bottom: 12px;
      color: #fff;
    }
    .feature-card p {
      color: var(--text-secondary);
      line-height: 1.6;
      font-size: 0.95rem;
    }
  `]
})
export class HomeComponent {
  projectName = '';

  constructor(private router: Router) {}

  searchProject() {
    if (this.projectName && this.projectName.trim()) {
      this.router.navigate(['/projects', this.projectName.trim()]);
    }
  }
}
