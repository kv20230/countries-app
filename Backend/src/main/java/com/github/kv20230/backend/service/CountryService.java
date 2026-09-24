package com.github.kv20230.backend.service;

import com.github.kv20230.backend.client.RestCountryClient;
import com.github.kv20230.backend.mapper.CountryMapper;
import com.github.kv20230.backend.model.dto.Country;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class CountryService {

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
}