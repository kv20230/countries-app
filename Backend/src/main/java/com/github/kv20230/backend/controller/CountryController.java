package com.github.kv20230.backend.controller;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.RestCountryResponse;
import com.github.kv20230.backend.service.CountryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    public List<Country> getAllCountries() {
        return countryService.getAllCountries();
    }
}