import { DecimalPipe, JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgIcon } from '@ng-icons/core';

import { ApiError, Country } from '@/core/models/country';
import { CountriesService } from '@/core/services/countries.service';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardTableImports } from '@/shared/components/table';

@Component({
  selector: 'app-countries-page',
  imports: [
    DecimalPipe,
    JsonPipe,
    NgIcon,
    ZardAlertComponent,
    ZardButtonComponent,
    ZardSkeletonComponent,
    ZardTableImports,
  ],
  templateUrl: './countries-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountriesPage {
  private readonly service = inject(CountriesService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly skeletonRows = Array.from({ length: 10 }, (_, i) => i);

  /** Full dataset from `/api/countries`; `null` until the first request resolves. */
  protected readonly countries = signal<Country[] | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly showErrorDetails = signal(false);

  protected readonly errorDescription = computed(() => {
    const err = this.error();
    if (!err) {
      return '';
    }
    if (err.status === 502 || err.status === 503) {
      return "The country data service isn't responding right now. Retry in a moment.";
    }
    return err.detail;
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.showErrorDetails.set(false);

    this.service
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: countries => {
          this.countries.set(countries);
          this.loading.set(false);
        },
        error: err => {
          this.error.set(CountriesService.toApiError(err));
          this.loading.set(false);
        },
      });
  }

  protected toggleErrorDetails(): void {
    this.showErrorDetails.update(v => !v);
  }
}
