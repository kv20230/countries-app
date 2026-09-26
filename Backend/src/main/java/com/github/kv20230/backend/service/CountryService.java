package com.github.kv20230.backend.service;

import com.github.kv20230.backend.client.RestCountryClient;
import com.github.kv20230.backend.mapper.CountryMapper;
import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.PagedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CountryService {

    public static final int MAX_PAGE_SIZE = 100;

    private final RestCountryClient restCountryClient;
    private final CountryMapper countryMapper;

    public CountryService(RestCountryClient restCountryClient, CountryMapper countryMapper) {
        this.restCountryClient = restCountryClient;
        this.countryMapper = countryMapper;
    }

    //Function that call external REST Country API until it gets all countries and caches the results.
    @Cacheable("countries")
    public List<Country> getAllCountries() {
        return restCountryClient.fetchAllCountries().stream()
                .map(countryMapper::toResponseDto)
                .toList();
    }

    public List<Country> getCountriesByRegion(List<Country> cachedCountries, String region) {
        if (region == null || region.isBlank()) {
            return cachedCountries;
        }
        return cachedCountries.stream()
                .filter(c -> c.region() != null && c.region().equalsIgnoreCase(region.trim()))
                .toList();
    }

    public List<Country> sortCountriesByPopulation(List<Country> cachedCountries, String direction) {
        if (direction == null || direction.isBlank()) {
            return cachedCountries;
        }

        //default: ascending
        Comparator<Country> comparator = Comparator.comparingLong(
                c -> c.population() != null ? c.population() : 0L
        );

        //descending
        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return  cachedCountries.stream()
                .sorted(comparator)
                .toList();
    }

    public Country getCountryByCode(List<Country> cachedCountries, String alpha3Code) {
        if (alpha3Code == null || alpha3Code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found with code: " + alpha3Code);
        }

        String normalizedCode = alpha3Code.trim();

        return cachedCountries.stream()
                .filter(c -> c.alpha3Code() != null && c.alpha3Code().equalsIgnoreCase(normalizedCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Country not found with code: " + alpha3Code));
    }

    /**
     * The countries a country borders, in the order REST Countries lists the border codes.
     * Codes with no matching country are skipped.
     *
     * @throws ResponseStatusException 404 when the country itself is unknown
     */
    public List<Country> getBorders(List<Country> cachedCountries, String alpha3Code) {
        Country country = getCountryByCode(cachedCountries, alpha3Code);
        Map<String, Country> byCode = new HashMap<>();
        for (Country c : cachedCountries) {
            if (c.alpha3Code() != null) {
                byCode.putIfAbsent(c.alpha3Code().toUpperCase(), c);
            }
        }
        return country.borders().stream()
                .filter(Objects::nonNull)
                .map(code -> byCode.get(code.toUpperCase()))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Country> getCountriesByName(List<Country> cachedCountries, String name) {
        if (name == null || name.isBlank()) {
            return cachedCountries;
        }

        String query = name.trim().toLowerCase();

        return cachedCountries.stream()
                .filter(c -> (c.commonName() != null && c.commonName().toLowerCase().contains(query))
                        || (c.officialName() != null && c.officialName().toLowerCase().contains(query)))
                .toList();
    }

    /**
     * Slices an already filtered/sorted list into one page.
     * A page past the end returns no items but still reports the totals, so the client can clamp.
     *
     * @throws ResponseStatusException 400 when page < 1 or size is outside 1..{@value MAX_PAGE_SIZE}
     */
    public PagedResponse<Country> paginate(List<Country> countries, int page, int size) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 1 or greater, was " + page);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size must be between 1 and " + MAX_PAGE_SIZE + ", was " + size);
        }

        int totalItems = countries.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) size));
        int from = (page - 1) * size;
        List<Country> items = from >= totalItems
                ? List.of()
                : countries.subList(from, Math.min(totalItems, from + size));

        return new PagedResponse<>(items, page, size, totalItems, totalPages);
    }
}
