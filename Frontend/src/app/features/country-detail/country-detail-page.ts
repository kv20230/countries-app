import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { catchError, map, merge, of, Subject, switchMap, tap } from 'rxjs';

import { ApiError, Country } from '@/core/models/country';
import { CountriesService } from '@/core/services/countries.service';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardImports } from '@/shared/components/card/card.imports';
import { ZardSeparatorComponent } from '@/shared/components/separator';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardTooltipImports } from '@/shared/components/tooltip';

interface BorderChip {
  code: string;
  name: string;
  officialName: string | null;
  flagUrl: string | null;
}

interface FactRow {
  label: string;
  value: string;
}

@Component({
  selector: 'app-country-detail-page',
  imports: [
    DecimalPipe,
    RouterLink,
    NgIcon,
    ZardAlertComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
    ZardCardImports,
    ZardSeparatorComponent,
    ZardSkeletonComponent,
    ZardTooltipImports,
  ],
  templateUrl: './country-detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountryDetailPage {
  /** Alpha-3 code from the route (`/countries/:code`), bound via withComponentInputBinding. */
  readonly code = input.required<string>();

  private readonly service = inject(CountriesService);
  private readonly retry$ = new Subject<void>();

  protected readonly country = signal<Country | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

  /** The cached list, used to resolve border codes to names and flags. Empty until it arrives. */
  private readonly all = toSignal(this.service.getAll().pipe(catchError(() => of([] as Country[]))), {
    initialValue: [] as Country[],
  });
  private readonly byCode = computed(() => new Map(this.all().map(c => [c.alpha3Code, c])));

  protected readonly eyebrow = computed(() => {
    const c = this.country();
    return c ? [c.region, c.subregion, c.alpha3Code].filter(Boolean).join(' · ') : '';
  });

  protected readonly density = computed<number | null>(() => {
    const c = this.country();
    if (!c || c.population === null || !c.area) {
      return null;
    }
    return Math.round(c.population / c.area);
  });

  protected readonly primaryTimezone = computed(() => this.country()?.timezones[0] ?? '—');
  protected readonly extraTimezones = computed(() => Math.max(0, (this.country()?.timezones.length ?? 0) - 1));
  protected readonly allTimezones = computed(() => this.country()?.timezones.join(', ') ?? '');

  protected readonly facts = computed<FactRow[]>(() => {
    const c = this.country();
    if (!c) {
      return [];
    }
    const list = (values: string[]) => (values.length ? values.join(', ') : '—');
    return [
      { label: 'ISO code', value: `${c.alpha2Code} / ${c.alpha3Code}` },
      { label: 'Capital', value: list(c.capitals) },
      { label: 'Subregion', value: c.subregion || '—' },
      { label: 'Timezones', value: list(c.timezones) },
      { label: 'Languages', value: list(c.languages) },
      { label: 'Currency', value: list(c.currencies) },
    ];
  });

  protected readonly borders = computed<BorderChip[]>(() => {
    const codes = this.country()?.borders ?? [];
    const lookup = this.byCode();
    return codes
      .map(code => {
        const match = lookup.get(code);
        return {
          code,
          name: match?.commonName ?? code,
          officialName: match?.officialName ?? null,
          flagUrl: match?.flagUrl ?? null,
        };
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  });

  constructor() {
    const code$ = toObservable(this.code);
    const reload$ = this.retry$.pipe(map(() => this.code()));

    merge(code$, reload$)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap(code =>
          this.service.getByCode(code).pipe(
            map(country => ({ country, error: null as ApiError | null })),
            catchError(err => of({ country: null, error: CountriesService.toApiError(err) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(result => {
        this.country.set(result.country);
        this.error.set(result.error);
        this.loading.set(false);
      });
  }

  protected retry(): void {
    this.retry$.next();
  }
}
