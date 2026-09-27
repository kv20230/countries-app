package com.github.kv20230.backend.service;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.game.AnswerResult;
import com.github.kv20230.backend.model.dto.game.FlagRound;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for the core logic of the flag guessing game.
 * Handles generating randomized game rounds, providing valid answer pools for autocomplete,
 * and evaluating user guesses using tolerant string normalization.
 */
@Service
public class GameService {

    public static final int DEFAULT_ROUNDS = 10;
    public static final int MAX_ROUNDS = 50;

    private final CountryService countryService;

    public GameService(CountryService countryService) {
        this.countryService = countryService;
    }

    /**
     * Generates a randomized set of game rounds by picking {@code count} distinct countries
     * that possess a valid flag image. The available country pool is shuffled once,
     * ensuring that rounds are random and no empty images are presented to the user.
     * If the requested count exceeds the number of valid countries available (e.g., in a limited
     * demo environment), it will return as many valid rounds as possible.
     *
     * @param countries the pool of available countries to pick from
     * @param count     the requested number of rounds (must be between 1 and {@value MAX_ROUNDS})
     * @return a list of {@link FlagRound} objects representing the selected game rounds
     * @throws ResponseStatusException with HTTP 400 if the requested count is out of bounds
     */
    public List<FlagRound> pickFlags(List<Country> countries, int count) {
        if (count < 1 || count > MAX_ROUNDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "count must be between 1 and " + MAX_ROUNDS + ", was " + count);
        }

        List<Country> shuffled = new ArrayList<>(countries);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());

        Set<Country> picked = new LinkedHashSet<>();
        for (Country country : shuffled) {
            if (picked.size() >= count) {
                break;
            }
            if (hasFlagImage(country)) {
                picked.add(country);
            }
        }

        return picked.stream()
                .map(c -> new FlagRound(c.alpha3Code(), c.flagUrl()))
                .toList();
    }


    /**
     * Extracts and sorts all distinct common country names from the provided list.
     * This is primarily used to populate frontend autocomplete suggestions for user answers.
     *
     * @param countries the list of countries to extract names from
     * @return a sorted, deduplicated list of common country names (case-insensitive alphabetical order)
     */
    public List<String> names(List<Country> countries) {
        return countries.stream()
                .map(Country::commonName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Evaluates a user's guess against a specific country's common and official names.
     * <p>
     * The comparison is highly tolerant: it ignores case, strips diacritics (accents),
     * removes punctuation, and collapses whitespace. A blank or null answer is considered a skip
     * and will always evaluate to incorrect.
     *
     * @param countries  the pool of available countries
     * @param alpha3Code the 3-letter ISO code of the target country being guessed
     * @param answer     the user's raw text guess
     * @return an {@link AnswerResult} containing the correct data and a boolean indicating if the guess was correct
     * @throws ResponseStatusException with HTTP 404 if the provided code does not match any known country
     */
    public AnswerResult checkAnswer(List<Country> countries, String alpha3Code, String answer) {
        Country country = countryService.getCountryByCode(countries, alpha3Code);
        String given = normalise(answer);
        boolean correct = !given.isEmpty()
                && (given.equals(normalise(country.commonName())) || given.equals(normalise(country.officialName())));
        return new AnswerResult(country.alpha3Code(), country.commonName(), country.flagUrl(), correct);
    }

    /**
     * Helper method to determine if a country has a valid flag image URL.
     *
     * @param country the country to check
     * @return {@code true} if the flag URL is present and not blank, {@code false} otherwise
     */
    static boolean hasFlagImage(Country country) {
        return country.flagUrl() != null && !country.flagUrl().isBlank();
    }

    /**
     * Normalizes a string for lenient comparison.
     * Processes the input by:
     * <ul>
     *     <li>Decomposing Unicode characters (NFD) and stripping diacritical marks (e.g., accents).</li>
     *     <li>Converting to lowercase using the root locale.</li>
     *     <li>Replacing all non-alphanumeric characters (punctuation, special symbols) with spaces.</li>
     *     <li>Trimming leading and trailing whitespace.</li>
     * </ul>
     * Example: {@code "Côte d’Ivoire"} becomes {@code "cote d ivoire"}.
     *
     * @param value the raw input string to normalize
     * @return the heavily normalized string, or an empty string if the input was null
     */
    static String normalise(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }
}
