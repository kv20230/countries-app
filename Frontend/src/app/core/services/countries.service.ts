import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { ApiError, Country } from '@/core/models/country';

@Injectable({ providedIn: 'root' })
export class CountriesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/countries';

  /**
   * The backend caches the full REST Countries dataset, so the list is fetched once
   * and replayed to every subscriber. On error the replay resets, so a retry re-fetches.
   */
  private readonly all$: Observable<Country[]> = this.http
    .get<Country[]>(this.baseUrl)
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));

  getAll(): Observable<Country[]> {
    return this.all$;
  }

  getByCode(alpha3Code: string): Observable<Country> {
    return this.http.get<Country>(`${this.baseUrl}/${encodeURIComponent(alpha3Code)}`);
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
