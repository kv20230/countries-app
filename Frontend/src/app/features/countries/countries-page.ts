import { DecimalPipe, JsonPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';

import { ApiError, Country, REGIONS, SortDirection } from '@/core/models/country';
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
  private readonly destroyRef = inject(DestroyRef);
  private readonly regionGroup = viewChild(ZardToggleGroupComponent);

  protected readonly regionItems: ZardToggleGroupItem[] = [
    { value: ALL_REGIONS, label: 'All' },
    ...REGIONS.map(region => ({ value: region, label: region })),
  ];
  protected readonly skeletonRows = Array.from({ length: PAGE_SIZE }, (_, i) => i);

  /** Full dataset from `/api/countries`; `null` until the first request resolves. */
  protected readonly countries = signal<Country[] | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly showErrorDetails = signal(false);

  protected readonly query = signal('');
  protected readonly region = signal<string>(ALL_REGIONS);
  protected readonly sortDir = signal<SortDirection>('desc');

  /** Resets to page 1 whenever the search, region or sort changes. */
  protected readonly page = linkedSignal<number>(() => {
    this.query();
    this.region();
    this.sortDir();
    return 1;
  });

  protected readonly filtered = computed<Country[]>(() => {
    const all = this.countries() ?? [];
    const query = this.query().trim().toLowerCase();
    const region = this.region();
    const direction = this.sortDir() === 'desc' ? -1 : 1;

    return all
      .filter(c => region === ALL_REGIONS || (c.region ?? '').toLowerCase() === region.toLowerCase())
      .filter(
        c =>
          query === '' ||
          c.commonName.toLowerCase().includes(query) ||
          c.officialName.toLowerCase().includes(query),
      )
      .sort((a, b) => direction * ((a.population ?? 0) - (b.population ?? 0)));
  });

  protected readonly total = computed(() => this.filtered().length);
  protected readonly pageCount = computed(() => Math.max(1, Math.ceil(this.total() / PAGE_SIZE)));
  protected readonly pageItems = computed(() => {
    const start = (this.page() - 1) * PAGE_SIZE;
    return this.filtered().slice(start, start + PAGE_SIZE);
  });

  protected readonly hasFilters = computed(() => this.query().trim() !== '' || this.region() !== ALL_REGIONS);

  protected readonly rangeLabel = computed(() => {
    const total = this.total();
    if (total === 0) {
      return 'Showing 0 results';
    }
    const start = (this.page() - 1) * PAGE_SIZE + 1;
    const end = Math.min(total, start + PAGE_SIZE - 1);
    const scope = this.region() === ALL_REGIONS ? '' : ` in ${this.region()}`;
    return `Showing ${start}–${end} of ${total}${scope}`;
  });

  protected readonly emptyTitle = computed(() => {
    const query = this.query().trim();
    const scope = this.region() === ALL_REGIONS ? '' : ` in ${this.region()}`;
    return query ? `No countries match "${query}"${scope}` : `No countries${scope}`;
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

  protected onSearch(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }

  protected onRegion(value: string | string[]): void {
    const next = typeof value === 'string' && value !== '' ? value : ALL_REGIONS;
    this.region.set(next);
    if (value === '') {
      // Deselecting the active item in single mode leaves nothing pressed; keep "All" lit.
      this.regionGroup()?.writeValue(ALL_REGIONS);
    }
  }

  protected toggleSort(): void {
    this.sortDir.update(dir => (dir === 'desc' ? 'asc' : 'desc'));
  }

  protected clearFilters(): void {
    this.query.set('');
    this.region.set(ALL_REGIONS);
    this.regionGroup()?.writeValue(ALL_REGIONS);
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
