import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Countries · Country Explorer',
    loadComponent: () => import('./features/countries/countries-page').then(m => m.CountriesPage),
  },
  {
    path: 'countries/:code',
    title: 'Country · Country Explorer',
    loadComponent: () =>
      import('./features/country-detail/country-detail-page').then(m => m.CountryDetailPage),
  },
  { path: '**', redirectTo: '' },
];
