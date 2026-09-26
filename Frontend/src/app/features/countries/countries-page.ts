import { DecimalPipe, JsonPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { catchError, distinctUntilChanged, map, merge, of, Subject, switchMap, tap, timer } from 'rxjs';

import { ApiError, Country, ListCriteria, PagedResponse, REGIONS, SortDirection } from '@/core/models/country';
import { CountriesService } from '@/core/services/countries.service';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardEmptyComponent } from '@/shared/components/empty';
import { ZardInputComponent } from '@/shared/components/input';
import { ZardInputGroupImports } from '@/shared/components/input-group';
import { ZardPaginationImports } from '@/shared/components/pagination';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardTableImports } from '@/shared/components/table';
import { ZardToggleGroupComponent, ZardToggleGroupItem } from '@/shared/components/toggle-group';

const PAGE_SIZE = 10;
const ALL_REGIONS = 'All';
/** Wait for typing to pause before hitting `/names`, so not every keystroke is a request. */
const SEARCH_DEBOUNCE_MS = 300;

interface PageRequest {
  criteria: ListCriteria;
  page: number;
}

@Component({
  selector: 'app-countries-page',
  imports: [
    DecimalPipe,
    JsonPipe,
    RouterLink,
    NgIcon,
    ZardAlertComponent,
    ZardButtonComponent,
    ZardEmptyComponent,
    ZardInputComponent,
    ZardInputGroupImports,
    ZardPaginationImports,
    ZardSkeletonComponent,
    ZardTableImports,
    ZardToggleGroupComponent,
  ],
  templateUrl: './countries-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountriesPage {
  private readonly service = inject(CountriesService);
  private readonly router = inject(Router);
  private readonly regionGroup = viewChild(ZardToggleGroupComponent);
  private readonly retry$ = new Subject<void>();

  protected readonly regionItems: ZardToggleGroupItem[] = [
    { value: ALL_REGIONS, label: 'All' },
    ...REGIONS.map(region => ({ value: region, label: region })),
  ];
  protected readonly skeletonRows = Array.from({ length: PAGE_SIZE }, (_, i) => i);

  /** The page the backend last returned; `null` until the first request resolves. */
  protected readonly pageData = signal<PagedResponse<Country> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly showErrorDetails = signal(false);

  protected readonly query = signal('');
  protected readonly region = signal<string>(ALL_REGIONS);
  protected readonly sortDir = signal<SortDirection>('desc');

  /**
   * The backend has one endpoint per criterion, so exactly one applies: a search wins over a
   * region, and the population sort only orders the plain list. Setting one control clears
   * the others (see `onSearch`/`onRegion`) so what you see matches what was requested.
   */
  protected readonly criteria = computed<ListCriteria>(() => {
    const query = this.query().trim();
    if (query !== '') {
      return { kind: 'name', query };
    }
    const region = this.region();
    if (region !== ALL_REGIONS) {
      return { kind: 'region', region };
    }
    return { kind: 'population', direction: this.sortDir() };
  });

  /** Resets to page 1 whenever the criteria change. */
  protected readonly page = linkedSignal<number>(() => {
    this.criteria();
    return 1;
  });

  private readonly request = computed<PageRequest>(() => ({ criteria: this.criteria(), page: this.page() }));

  protected readonly items = computed(() => this.pageData()?.items ?? []);
  protected readonly total = computed(() => this.pageData()?.totalItems ?? 0);
  protected readonly pageCount = computed(() => this.pageData()?.totalPages ?? 1);

  protected readonly sortEnabled = computed(() => this.criteria().kind === 'population');
  protected readonly hasFilters = computed(() => this.criteria().kind !== 'population');

  protected readonly rangeLabel = computed(() => {
    const data = this.pageData();
    if (!data || data.totalItems === 0) {
      return 'Showing 0 results';
    }
    const start = (data.page - 1) * data.size + 1;
    const end = start + data.items.length - 1;
    return `Showing ${start}–${end} of ${data.totalItems}${this.scopeLabel()}`;
  });

  protected readonly emptyTitle = computed(() => {
    const query = this.query().trim();
    return query ? `No countries match "${query}"` : `No countries${this.scopeLabel()}`;
  });

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
    // Typing pauses for a moment before a search request goes out; page and region changes go straight away.
    const request$ = toObservable(this.request).pipe(
      switchMap(req => (req.criteria.kind === 'name' ? timer(SEARCH_DEBOUNCE_MS).pipe(map(() => req)) : of(req))),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
    );

    merge(request$, this.retry$.pipe(map(() => this.request())))
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
          this.showErrorDetails.set(false);
        }),
        switchMap(req =>
          this.service.getPage(req.criteria, req.page, PAGE_SIZE).pipe(
            map(data => ({ data, error: null as ApiError | null })),
            catchError(err => of({ data: null, error: CountriesService.toApiError(err) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(result => {
        if (result.data) {
          this.pageData.set(result.data);
        }
        this.error.set(result.error);
        this.loading.set(false);
      });
  }

  private scopeLabel(): string {
    const criteria = this.criteria();
    return criteria.kind === 'region' ? ` in ${criteria.region}` : '';
  }

  protected retry(): void {
    this.retry$.next();
  }

  protected onSearch(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
    if (this.query().trim() !== '' && this.region() !== ALL_REGIONS) {
      this.setRegion(ALL_REGIONS);
    }
  }

  protected onRegion(value: string | string[]): void {
    const next = typeof value === 'string' && value !== '' ? value : ALL_REGIONS;
    this.region.set(next);
    if (value === '') {
      // Deselecting the active item in single mode leaves nothing pressed; keep "All" lit.
      this.regionGroup()?.writeValue(ALL_REGIONS);
    }
    if (next !== ALL_REGIONS) {
      this.query.set('');
    }
  }

  protected toggleSort(): void {
    this.sortDir.update(dir => (dir === 'desc' ? 'asc' : 'desc'));
  }

  protected clearFilters(): void {
    this.query.set('');
    this.setRegion(ALL_REGIONS);
  }

  private setRegion(region: string): void {
    this.region.set(region);
    this.regionGroup()?.writeValue(region);
  }

  protected previousPage(): void {
    this.page.update(p => Math.max(1, p - 1));
  }

  protected nextPage(): void {
    this.page.update(p => Math.min(this.pageCount(), p + 1));
  }

  protected toggleErrorDetails(): void {
    this.showErrorDetails.update(v => !v);
  }

  protected open(country: Country): void {
    void this.router.navigate(['/countries', country.alpha3Code]);
  }
}
