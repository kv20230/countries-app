package com.github.kv20230.backend.controller;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.PagedResponse;
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

    private static final String PAGE_DESCRIPTION = "1-based page number";
    private static final String SIZE_DESCRIPTION = "Entries per page (1-100)";

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @Operation(
            summary = "Fetch all countries",
            description = "Retrieves one page of all countries mapped from REST Countries v5. The full dataset is cached in memory."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved country list"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page or size",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
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
    public PagedResponse<Country> getAllCountries(
            @Parameter(description = PAGE_DESCRIPTION, example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = SIZE_DESCRIPTION, example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return countryService.paginate(countryService.getAllCountries(), page, size);
    }

    @Operation(
            summary = "Sort countries by a chosen region.",
            description = "Retrieves one page of the countries in a certain region."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully sorted countries by region"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page or size",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
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
    public PagedResponse<Country> getCountriesByRegion (
            @Parameter(description = "Name of the region (e.g. Europe, Americas, Asia, Africa, Oceania)", example = "Europe")
            @RequestParam String region,
            @Parameter(description = PAGE_DESCRIPTION, example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = SIZE_DESCRIPTION, example = "10")
            @RequestParam(defaultValue = "10") int size) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.paginate(countryService.getCountriesByRegion(cachedCountries, region), page, size);
    }

    @Operation(
            summary = "Sorts countries by population.",
            description = "Retrieves one page of the list sorted by population ascending or descending."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully sorted countries by population"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page or size",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/population")
    public PagedResponse<Country> sortCountriesByPopulation(
            @Parameter(description = "The countries can either be sorted population ascending or descending", example = "ascending")
            @RequestParam String direction,
            @Parameter(description = PAGE_DESCRIPTION, example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = SIZE_DESCRIPTION, example = "10")
            @RequestParam(defaultValue = "10") int size) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.paginate(countryService.sortCountriesByPopulation(cachedCountries, direction), page, size);
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
                    responseCode = "404",
                    description = "No country with that code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
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
            summary = "Retrieves the bordering countries.",
            description = "Retrieves the full country objects for every border code of the given country, in the API's order."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieves the neighbours (empty for island countries)."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No country with that code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/{alpha3Code}/borders")
    public List<Country> getBorders(
            @Parameter(description = "Unique 3 letter code for the country", example = "SVN")
            @PathVariable String alpha3Code) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.getBorders(cachedCountries, alpha3Code);
    }

    @Operation(
            summary = "Retrieves a country.",
            description = "Retrieves one page of the countries whose common or official name contains the given text."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieves the country."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page or size",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/names")
    public PagedResponse<Country> getCountriesByName(
            @Parameter(description = "Name of the country, either official or common", example = "Slovenia")
            @RequestParam String name,
            @Parameter(description = PAGE_DESCRIPTION, example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = SIZE_DESCRIPTION, example = "10")
            @RequestParam(defaultValue = "10") int size) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return countryService.paginate(countryService.getCountriesByName(cachedCountries, name), page, size);
    }
}
