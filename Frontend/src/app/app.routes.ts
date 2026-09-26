import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Countries · Country Explorer',
    loadComponent: () => import('./features/countries/countries-page').then(m => m.CountriesPage),
  },
  { path: '**', redirectTo: '' }, 
];
