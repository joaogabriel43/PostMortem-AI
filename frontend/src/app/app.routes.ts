import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'incidents/new',
    loadComponent: () => import('./pages/incidents/incident-new/incident-new.component').then(m => m.IncidentNewComponent)
  },
  {
    path: 'incidents/:id',
    loadComponent: () => import('./pages/incidents/incident-detail/incident-detail.component').then(m => m.IncidentDetailComponent)
  },
  {
    path: 'projects/:name',
    loadComponent: () => import('./pages/projects/project-history/project-history.component').then(m => m.ProjectHistoryComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
