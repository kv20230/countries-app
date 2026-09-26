package com.github.kv20230.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kv20230.backend.client.RestCountryClient;
import com.github.kv20230.backend.mapper.CountryMapper;
import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.game.AnswerResult;
import com.github.kv20230.backend.model.dto.game.FlagRound;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceTest {

    private final GameService service = new GameService(new CountryService(
            new RestCountryClient(new ObjectMapper(), "http://localhost", "test"),
            new CountryMapper()));

    private static Country country(String code, String common, String official, String flagUrl) {
        return new Country(common, official, code.substring(0, 2), code, "Europe", null, 1L, null,
                List.of(), List.of(), List.of(), List.of(), null, flagUrl, List.of(),
                List.of(), List.of(), null, null, null, null, null, null, null, null, null);
    }

    /** n countries; only the odd-numbered ones have a flag image (the even ones have null or blank). */
    private static List<Country> mixed(int n) {
        return IntStream.rangeClosed(1, n)
                .mapToObj(i -> country("C" + i, "Country " + i, "Republic of " + i,
                        i % 2 == 1 ? "https://flags/" + i + ".png" : (i % 4 == 0 ? "" : null)))
                .toList();
    }

    @Test
    void picksOnlyCountriesWithAFlagImage() {
        for (int attempt = 0; attempt < 20; attempt++) {
            List<FlagRound> rounds = service.pickFlags(mixed(40), 10);

            assertEquals(10, rounds.size());
            assertTrue(rounds.stream().allMatch(r -> r.flagUrl() != null && !r.flagUrl().isBlank()));
        }
    }

    @Test
    void neverRepeatsACountry() {
        for (int attempt = 0; attempt < 20; attempt++) {
            List<FlagRound> rounds = service.pickFlags(mixed(40), 10);
            Set<String> codes = new HashSet<>(rounds.stream().map(FlagRound::alpha3Code).toList());

            assertEquals(rounds.size(), codes.size());
        }
    }

    @Test
    void returnsFewerRoundsWhenFewCountriesQualify() {
        List<FlagRound> rounds = service.pickFlags(mixed(6), 10); // only 3 have an image

        assertEquals(3, rounds.size());
    }

    @Test
    void rejectsCountOutsideBounds() {
        assertThrows(ResponseStatusException.class, () -> service.pickFlags(mixed(4), 0));
        assertThrows(ResponseStatusException.class, () -> service.pickFlags(mixed(4), GameService.MAX_ROUNDS + 1));
    }

    @Test
    void namesAreDistinctAndSorted() {
        List<Country> all = List.of(
                country("SVN", "Slovenia", "Republic of Slovenia", null),
                country("AUT", "Austria", "Republic of Austria", null),
                country("XXX", "Austria", "Duplicate", null),
                country("ZZZ", null, "No name", null));

        assertEquals(List.of("Austria", "Slovenia"), service.names(all));
    }

    @Test
    void acceptsCommonNameIgnoringCaseAccentsAndPunctuation() {
        List<Country> all = List.of(country("CIV", "Côte d’Ivoire", "Republic of Côte d'Ivoire", "f.png"));

        AnswerResult result = service.checkAnswer(all, "civ", "  cote D'IVOIRE ");

        assertTrue(result.correct());
        assertEquals("Côte d’Ivoire", result.commonName());
        assertEquals("CIV", result.alpha3Code());
        assertEquals("f.png", result.flagUrl());
    }

    @Test
    void acceptsOfficialName() {
        List<Country> all = List.of(country("SVN", "Slovenia", "Republic of Slovenia", "f.png"));

        assertTrue(service.checkAnswer(all, "SVN", "republic of slovenia").correct());
    }

    @Test
    void wrongOrBlankAnswerIsIncorrectButStillRevealsTheName() {
        List<Country> all = List.of(country("SVN", "Slovenia", "Republic of Slovenia", "f.png"));

        AnswerResult wrong = service.checkAnswer(all, "SVN", "Slovakia");
        AnswerResult skipped = service.checkAnswer(all, "SVN", "   ");
        AnswerResult missing = service.checkAnswer(all, "SVN", null);

        assertFalse(wrong.correct());
        assertFalse(skipped.correct());
        assertFalse(missing.correct());
        assertEquals("Slovenia", skipped.commonName());
    }

    @Test
    void unknownCodeIs404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.checkAnswer(mixed(2), "ZZZ", "x"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
