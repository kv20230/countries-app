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

@Service
public class GameService {

    public static final int DEFAULT_ROUNDS = 10;
    public static final int MAX_ROUNDS = 50;

    private final CountryService countryService;

    public GameService(CountryService countryService) {
        this.countryService = countryService;
    }

    /**
     * Picks {@code count} distinct countries that have a flag image.
     * The cached list is shuffled once and walked in order; a country is only added to the
     * result set when it carries a flag image, so a round can never show an empty picture.
     * Returns fewer rounds when fewer countries qualify (the demo API key returns one country).
     *
     * @throws ResponseStatusException 400 when count is outside 1..{@value MAX_ROUNDS}
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

    /** Every distinct common name, sorted, for the answer autocomplete. */
    public List<String> names(List<Country> countries) {
        return countries.stream()
                .map(Country::commonName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Compares the answer with the common and official name after normalising both:
     * lower-case, diacritics and punctuation stripped, whitespace collapsed.
     * A blank answer is a skip and is never correct.
     *
     * @throws ResponseStatusException 404 when the code matches no country
     */
    public AnswerResult checkAnswer(List<Country> countries, String alpha3Code, String answer) {
        Country country = countryService.getCountryByCode(countries, alpha3Code);
        String given = normalise(answer);
        boolean correct = !given.isEmpty()
                && (given.equals(normalise(country.commonName())) || given.equals(normalise(country.officialName())));
        return new AnswerResult(country.alpha3Code(), country.commonName(), country.flagUrl(), correct);
    }

    static boolean hasFlagImage(Country country) {
        return country.flagUrl() != null && !country.flagUrl().isBlank();
    }

    /** "Côte d’Ivoire" → "cote d ivoire". */
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
