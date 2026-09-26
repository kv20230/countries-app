package com.github.kv20230.backend.model.dto.game;

/**
 * @param alpha3Code the code from the {@link FlagRound}
 * @param answer     what the player typed; blank or null counts as a skip
 */
public record AnswerRequest(
        String alpha3Code,
        String answer
) {
}
