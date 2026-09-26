package com.github.kv20230.backend.model.dto.game;

/** The verdict for one round, with the solution revealed. */
public record AnswerResult(
        String alpha3Code,
        String commonName,
        String flagUrl,
        boolean correct
) {
}
