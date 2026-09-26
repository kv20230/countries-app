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
}

export const REGIONS = ['Africa', 'Americas', 'Asia', 'Europe', 'Oceania'] as const;
export type Region = (typeof REGIONS)[number];

export type SortDirection = 'asc' | 'desc';

/** Shape of the RFC 9457 ProblemDetail the backend's @ControllerAdvice returns. */
export interface ApiError {
  status: number;
  title: string;
  detail: string;
}
