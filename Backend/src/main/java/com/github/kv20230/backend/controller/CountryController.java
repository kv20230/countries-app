package com.github.kv20230.backend.controller;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.RestCountryResponse;
import com.github.kv20230.backend.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@Tag(name = "Country API", description = "Endpoints for fetching, searching and sorting country information")
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @Operation(
            summary = "Fetch all countries",
            description = "Retrieves a list of all countries mapped from REST Countries v5 and caches results in memory."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved country list"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error or external API failure",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Upstream REST Country API failure (e.g., invalid/missing API key, upstream 4xx/5xx error, or network failure)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping
    //returns a list of all countries
    public List<Country> getAllCountries() {
        return countryService.getAllCountries();
    }

    @Operation(
            summary = "Sort countries by a chosen region.",
            description = "Retrieves a list of all countries in a certain region."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully sorted countries by region"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Region not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/regions")
    public List<Country> getCountriesByRegion (
            @Parameter(description = "Name of the region (e.g. Europe, Americas, Asia, Africa, Oceania)", example = "Europe")
            @RequestParam String region) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.getCountriesByRegion(cachedCountries, region);
    }

    @Operation(
            summary = "Sorts countries by population.",
            description = "Retrieves a list that is sorted by population ascending or descending."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully sorted countries by population"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/population")
    public List<Country> sortCountriesByPopulation(
            @Parameter(description = "The countries can either be sorted population ascending or descending", example = "ascending")
            @RequestParam String direction) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.sortCountriesByPopulation(cachedCountries, direction);
    }

    @Operation(
            summary = "Retrieves a country.",
            description = "Retrieves a country that matches the given 3 letter code."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieves the country."
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/{alpha3Code}")
    public Country getCountryByCode(
            @Parameter(description = "Unique 3 letter code for the country", example = "SVN")
            @PathVariable String alpha3Code) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.getCountryByCode(cachedCountries, alpha3Code);
    }

    @Operation(
            summary = "Retrieves a country.",
            description = "Retrieves a country that matches the given name."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieves the country."
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/names")
    public List<Country> getCountriesByName(
            @Parameter(description = "Name of the country, either official or common", example = "Slovenia")
            @RequestParam String name) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.getCountriesByName(cachedCountries, name);
    }


}