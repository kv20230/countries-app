package com.github.kv20230.backend.model.dto.game;

/**
 * One quiz round: the flag to identify and the code to send back with the answer.
 * The country name is deliberately absent so the client cannot read the solution.
 */
public record FlagRound(
        String alpha3Code,
        String flagUrl
) {
}
