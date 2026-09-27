package com.github.kv20230.backend.model.dto.game;

/** The verdict for one round, with the solution (name of the country) revealed. */
public record AnswerResult(
        String alpha3Code,
        String commonName,
        String flagUrl,
        boolean correct
) {
}
