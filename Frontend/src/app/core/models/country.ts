/** Mirrors the backend `Country` DTO returned by `/api/countries`. */
export interface Country {
  commonName: string;
  officialName: string;
  alpha2Code: string;
  alpha3Code: string;
  region: string | null;
  subregion: string | null;
  population: number | null;
  area: number | null;
  capitals: string[];
  languages: string[];
  borders: string[];
  currencies: string[];
  flagEmoji: string | null;
  flagUrl: string | null;
  timezones: string[];
  /** Dialling prefixes, already prefixed with "+". */
  callingCodes: string[];
  topLevelDomains: string[];
  drivingSide: string | null;
  unMember: boolean | null;
  euMember: boolean | null;
  landlocked: boolean | null;
  governmentType: string | null;
  demonym: string | null;
  startOfWeek: string | null;
  wikipediaUrl: string | null;
  description: string | null;
}

/** One page of a list endpoint, as the backend's `PagedResponse` returns it. */
export interface PagedResponse<T> {
  items: T[];
  /** 1-based. */
  page: number;
  size: number;
  totalItems: number;
  /** At least 1, even for an empty result. */
  totalPages: number;
}

export const REGIONS = ['Africa', 'Americas', 'Asia', 'Europe', 'Oceania'] as const;
export type Region = (typeof REGIONS)[number];

export type SortDirection = 'asc' | 'desc';

/**
 * Which list endpoint to call. The backend offers one criterion per endpoint
 * (`/names`, `/regions`, `/population`), so a view is exactly one of these.
 */
export type ListCriteria =
  | { kind: 'all' }
  | { kind: 'name'; query: string }
  | { kind: 'region'; region: string }
  | { kind: 'population'; direction: SortDirection };

/** Shape of the RFC 9457 ProblemDetail the backend's @ControllerAdvice returns. */
export interface ApiError {
  status: number;
  title: string;
  detail: string;
}
