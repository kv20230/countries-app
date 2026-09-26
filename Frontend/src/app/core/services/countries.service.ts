import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiError, Country, ListCriteria, PagedResponse } from '@/core/models/country';

@Injectable({ providedIn: 'root' })
export class CountriesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/countries';

  /**
   * One page of the list. The criteria picks the endpoint; every endpoint takes the same
   * `page` (1-based) and `size` query parameters and returns the same `PagedResponse`.
   */
  getPage(criteria: ListCriteria, page: number, size: number): Observable<PagedResponse<Country>> {
    let path = '';
    let params = new HttpParams().set('page', page).set('size', size);

    switch (criteria.kind) {
      case 'name':
        path = '/names';
        params = params.set('name', criteria.query);
        break;
      case 'region':
        path = '/regions';
        params = params.set('region', criteria.region);
        break;
      case 'population':
        path = '/population';
        params = params.set('direction', criteria.direction);
        break;
      case 'all':
        break;
    }

    return this.http.get<PagedResponse<Country>>(`${this.baseUrl}${path}`, { params });
  }

  getByCode(alpha3Code: string): Observable<Country> {
    return this.http.get<Country>(`${this.baseUrl}/${encodeURIComponent(alpha3Code)}`);
  }

  /** The countries behind a country's border codes, resolved by the backend. Empty for islands. */
  getBorders(alpha3Code: string): Observable<Country[]> {
    return this.http.get<Country[]>(`${this.baseUrl}/${encodeURIComponent(alpha3Code)}/borders`);
  }

  static toApiError(err: unknown): ApiError {
    if (err instanceof HttpErrorResponse) {
      const body = err.error as Partial<ApiError> | null;
      if (err.status === 0) {
        return {
          status: 0,
          title: 'Backend unreachable',
          detail: 'The Spring Boot API did not respond. Is it running on port 8080?',
        };
      }
      return {
        status: err.status,
        title: body?.title ?? err.statusText ?? 'Request failed',
        detail: body?.detail ?? err.message,
      };
    }
    return { status: -1, title: 'Unexpected error', detail: String(err) };
  }
}
