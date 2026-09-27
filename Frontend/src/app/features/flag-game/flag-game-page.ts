import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { forkJoin, map, Observable, tap } from 'rxjs';

import { ApiError } from '@/core/models/country';
import { FlagRound } from '@/core/models/game';
import { CountriesService } from '@/core/services/countries.service';
import { GameService } from '@/core/services/game.service';
import { ZardAlertComponent } from '@/shared/components/alert';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardImports } from '@/shared/components/card/card.imports';
import { ZardInputComponent } from '@/shared/components/input';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';

export const ROUNDS_PER_GAME = 10;
const MAX_SUGGESTIONS = 8;

export interface Suggestion {
  name: string;
  /** The part of the name that matches what was typed, for highlighting. */
  matchStart: number;
  matchLength: number;
}

export interface Round extends FlagRound {
  /** What the player typed; empty when skipped. */
  answer: string;
  /** null until the API has checked the round. */
  correct: boolean | null;
  /** Revealed by the API together with the verdict. */
  commonName: string | null;
}

type Phase = 'loading' | 'error' | 'playing' | 'finished';

/** Lower-case, strip diacritics and punctuation, collapse whitespace: "Côte d’Ivoire" → "cote d ivoire". */
export function normaliseAnswer(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, ' ')
    .trim();
}

/**
 * Maps a match found in the normalised name back onto the original string so the matching
 * characters can be highlighted. Normalisation drops combining marks and merges punctuation
 * runs into single spaces, so we walk both strings in step.
 */
function matchRange(name: string, normalisedStart: number, normalisedLength: number): Pick<Suggestion, 'matchStart' | 'matchLength'> {
  let start = -1;
  let end = -1;
  let seen = 0;
  for (let i = 0; i < name.length; i++) {
    const piece = normaliseAnswer(name.slice(0, i + 1));
    const produced = piece.length;
    if (produced > seen) {
      if (start === -1 && produced > normalisedStart) {
        start = i;
      }
      if (produced >= normalisedStart + normalisedLength) {
        end = i + 1;
        break;
      }
      seen = produced;
    }
  }
  if (start === -1 || end === -1) {
    return { matchStart: 0, matchLength: 0 };
  }
  return { matchStart: start, matchLength: end - start };
}

@Component({
  selector: 'app-flag-game-page',
  imports: [
    NgIcon,
    ZardAlertComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
    ZardCardImports,
    ZardInputComponent,
    ZardSkeletonComponent,
  ],
  templateUrl: './flag-game-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FlagGamePage {
  private readonly service = inject(GameService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly answerInput = viewChild('answerInput', { read: ElementRef<HTMLInputElement> });

  /** Every country name from `/api/game/names`, loaded once per visit. */
  private readonly allNames = signal<string[]>([]);
  protected readonly phase = signal<Phase>('loading');
  protected readonly error = signal<ApiError | null>(null);

  protected readonly rounds = signal<Round[]>([]);
  protected readonly index = signal(0);
  protected readonly answer = signal('');
  /** True while `POST /api/game/answer` is in flight. */
  protected readonly checking = signal(false);
  protected readonly checkError = signal<ApiError | null>(null);

  protected readonly totalRounds = computed(() => this.rounds().length);
  protected readonly current = computed<Round | undefined>(() => this.rounds()[this.index()]);
  protected readonly revealed = computed(() => this.current()?.correct !== null);
  protected readonly score = computed(() => this.rounds().filter(r => r.correct === true).length);
  protected readonly answered = computed(() => this.rounds().filter(r => r.correct !== null).length);
  protected readonly progress = computed(() =>
    this.totalRounds() === 0 ? 0 : Math.round((this.answered() / this.totalRounds()) * 100),
  );
  protected readonly isLastRound = computed(() => this.index() >= this.totalRounds() - 1);
  protected readonly canSubmit = computed(() => !this.revealed() && !this.checking() && this.answer().trim() !== '');

  /** All country names with their normalised form for matching. */
  private readonly names = computed(() => this.allNames().map(name => ({ name, key: normaliseAnswer(name) })));

  /** Names matching the typed text: prefix matches first, then anywhere in the name; narrows as you type. */
  protected readonly suggestions = computed<Suggestion[]>(() => {
    const query = normaliseAnswer(this.answer());
    if (!query || this.revealed() || this.checking()) {
      return [];
    }
    const starts: Suggestion[] = [];
    const contains: Suggestion[] = [];
    for (const { name, key } of this.names()) {
      if (key === query) {
        continue; // exact match already typed; nothing to suggest
      }
      const at = key.indexOf(query);
      if (at === -1) {
        continue;
      }
      const suggestion = { name, ...matchRange(name, at, query.length) };
      (at === 0 ? starts : contains).push(suggestion);
      if (starts.length >= MAX_SUGGESTIONS) {
        break;
      }
    }
    return [...starts, ...contains].slice(0, MAX_SUGGESTIONS);
  });

  /** Closed by Escape or after choosing; reopens on the next keystroke. */
  protected readonly suggestionsOpen = signal(true);
  protected readonly showSuggestions = computed(() => this.suggestionsOpen() && this.suggestions().length > 0);
  /** Index of the keyboard-highlighted suggestion; resets whenever the list changes. */
  protected readonly highlighted = linkedSignal<Suggestion[], number>({
    source: this.suggestions,
    computation: () => -1,
  });

  constructor() {
    this.load();
  }

  /** First load: the names for the autocomplete and the flags for the first game together. */
  protected load(): void {
    this.begin(
      forkJoin({ names: this.service.getNames(), flags: this.service.getFlags(ROUNDS_PER_GAME) }).pipe(
        tap(({ names }) => this.allNames.set(names)),
        map(({ flags }) => flags),
      ),
    );
  }

  /** Restart / Play again: a fresh draw of flags; the names are already here. */
  protected startGame(): void {
    this.begin(this.service.getFlags(ROUNDS_PER_GAME));
  }

  private begin(flags$: Observable<FlagRound[]>): void {
    this.phase.set('loading');
    this.error.set(null);
    this.checkError.set(null);
    flags$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: flags => {
        if (flags.length === 0) {
          this.error.set({
            status: -1,
            title: 'No flags available',
            detail: 'The API returned no countries with a flag image.',
          });
          this.phase.set('error');
          return;
        }
        this.rounds.set(flags.map(flag => ({ ...flag, answer: '', correct: null, commonName: null })));
        this.index.set(0);
        this.answer.set('');
        this.phase.set('playing');
        this.focusInput();
      },
      error: err => {
        this.error.set(CountriesService.toApiError(err));
        this.phase.set('error');
      },
    });
  }

  protected onAnswerInput(event: Event): void {
    this.answer.set((event.target as HTMLInputElement).value);
    this.suggestionsOpen.set(true);
  }

  protected onAnswerKeydown(event: KeyboardEvent): void {
    if (this.checking()) {
      event.preventDefault();
      return;
    }
    if (this.revealed()) {
      if (event.key === 'Enter') {
        event.preventDefault();
        this.next();
      }
      return;
    }
    const open = this.showSuggestions();
    const count = this.suggestions().length;
    switch (event.key) {
      case 'ArrowDown':
        if (open) {
          event.preventDefault();
          this.highlighted.update(i => (i + 1) % count);
        }
        break;
      case 'ArrowUp':
        if (open) {
          event.preventDefault();
          this.highlighted.update(i => (i <= 0 ? count - 1 : i - 1));
        }
        break;
      case 'Escape':
        if (open) {
          event.preventDefault();
          this.suggestionsOpen.set(false);
        }
        break;
      case 'Enter': {
        event.preventDefault();
        const pick = open ? this.suggestions()[this.highlighted()] : undefined;
        if (pick) {
          this.choose(pick.name);
        } else {
          this.submit();
        }
        break;
      }
    }
  }

  /** Picking a suggestion fills the answer and checks it straight away. */
  protected choose(name: string): void {
    this.answer.set(name);
    this.suggestionsOpen.set(false);
    this.submit();
  }

  protected submit(): void {
    const round = this.current();
    if (!round || this.revealed() || !this.canSubmit()) {
      return;
    }
    this.checkRound(round, this.answer());
  }

  protected skip(): void {
    const round = this.current();
    if (!round || this.revealed() || this.checking()) {
      return;
    }
    this.checkRound(round, '');
  }

  protected next(): void {
    if (!this.revealed()) {
      return;
    }
    if (this.isLastRound()) {
      this.phase.set('finished');
      return;
    }
    this.index.update(i => i + 1);
    this.answer.set('');
    this.checkError.set(null);
    this.focusInput();
  }

  /** The API holds the answer key: it checks the guess and reveals the name. */
  private checkRound(round: Round, answer: string): void {
    this.checking.set(true);
    this.checkError.set(null);
    this.service
      .checkAnswer(round.alpha3Code, answer)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.rounds.update(rounds =>
            rounds.map(r =>
              r === round ? { ...r, answer: answer.trim(), correct: result.correct, commonName: result.commonName } : r,
            ),
          );
          this.checking.set(false);
          // Keep focus on the (now read-only) input so Enter advances to the next round.
          this.focusInput();
        },
        error: err => {
          this.checkError.set(CountriesService.toApiError(err));
          this.checking.set(false);
        },
      });
  }

  private focusInput(): void {
    queueMicrotask(() => this.answerInput()?.nativeElement.focus());
  }
}
