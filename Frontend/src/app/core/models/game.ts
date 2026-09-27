/** One quiz round from `GET /api/game/flags`. The name is not included; the API reveals it on answer. */
export interface FlagRound {
  alpha3Code: string;
  flagUrl: string;
}

/** Verdict from `POST /api/game/answer`. */
export interface AnswerResult {
  alpha3Code: string;
  commonName: string;
  flagUrl: string;
  correct: boolean;
}
