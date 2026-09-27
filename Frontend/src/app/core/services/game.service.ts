import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AnswerResult, FlagRound } from '@/core/models/game';

@Injectable({ providedIn: 'root' })
export class GameService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/game';

  /** Random, distinct countries that have a flag image. May return fewer than asked for. */
  getFlags(count: number): Observable<FlagRound[]> {
    return this.http.get<FlagRound[]>(`${this.baseUrl}/flags`, { params: { count } });
  }

  /** Sorted distinct common names, for the autocomplete. */
  getNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/names`);
  }

  /** A blank answer counts as a skip; the result reveals the correct name either way. */
  checkAnswer(alpha3Code: string, answer: string): Observable<AnswerResult> {
    return this.http.post<AnswerResult>(`${this.baseUrl}/answer`, { alpha3Code, answer });
  }
}
