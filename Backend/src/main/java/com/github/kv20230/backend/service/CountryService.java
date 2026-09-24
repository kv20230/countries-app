package com.github.kv20230.backend.service;

import com.github.kv20230.backend.client.RestCountryClient;
import com.github.kv20230.backend.mapper.CountryMapper;
import com.github.kv20230.backend.model.dto.Country;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Service
public class CountryService {

    private final RestCountryClient restCountryClient;
    private final CountryMapper countryMapper;

    public CountryService(RestCountryClient restCountryClient, CountryMapper countryMapper) {
        this.restCountryClient = restCountryClient;
        this.countryMapper = countryMapper;
    }

    //Metoda, ki pokliče zunanji API (REST Countries) nato pa podatke shrani v medpomnilnik
    @Cacheable("countries")
    public List<Country> getAllCountries() {
        return restCountryClient.fetchAllCountries().stream()
                .map(countryMapper::toResponseDto)
                .toList();
    }
}