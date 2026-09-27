import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
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
  /** Neighbours from `/api/countries/{code}/borders`; empty until they arrive or when the request fails. */
  private readonly neighbours = signal<Country[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

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

  /** Facts not already shown in the hero or the stat tiles. */
  protected readonly facts = computed<FactRow[]>(() => {
    const c = this.country();
    if (!c) {
      return [];
    }
    const list = (values: string[]) => (values.length ? values.join(', ') : '—');
    const yesNo = (value: boolean | null) => (value === null ? '—' : value ? 'Yes' : 'No');
    const capitalise = (value: string | null) => (value ? value.charAt(0).toUpperCase() + value.slice(1) : '—');
    return [
      { label: 'Languages', value: list(c.languages) },
      { label: 'Currency', value: list(c.currencies) },
      { label: 'Calling code', value: list(c.callingCodes) },
      { label: 'Top-level domain', value: list(c.topLevelDomains) },
      { label: 'Drives on', value: capitalise(c.drivingSide) },
      { label: 'UN member', value: yesNo(c.unMember) },
      { label: 'EU member', value: yesNo(c.euMember) },
      { label: 'Landlocked', value: yesNo(c.landlocked) },
      { label: 'Government', value: c.governmentType || '—' },
      { label: 'Demonym', value: c.demonym || '—' },
      { label: 'Week starts on', value: capitalise(c.startOfWeek) },
    ];
  });

  protected readonly borders = computed<BorderChip[]>(() =>
    this.neighbours()
      .map(n => ({
        code: n.alpha3Code,
        name: n.commonName,
        officialName: n.officialName,
        flagUrl: n.flagUrl,
      }))
      .sort((a, b) => a.name.localeCompare(b.name)),
  );

  constructor() {
    const code$ = toObservable(this.code);
    const reload$ = this.retry$.pipe(map(() => this.code()));

    merge(code$, reload$)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
          this.neighbours.set([]);
        }),
        switchMap(code =>
          this.service.getByCode(code).pipe(
            // The neighbours are a second request; if it fails the page still shows the country.
            switchMap(country =>
              this.service.getBorders(code).pipe(
                catchError(() => of([] as Country[])),
                map(neighbours => ({ country, neighbours, error: null as ApiError | null })),
              ),
            ),
            catchError(err => of({ country: null, neighbours: [] as Country[], error: CountriesService.toApiError(err) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(result => {
        this.country.set(result.country);
        this.neighbours.set(result.neighbours);
        this.error.set(result.error);
        this.loading.set(false);
      });
  }

  protected retry(): void {
    this.retry$.next();
  }
}
