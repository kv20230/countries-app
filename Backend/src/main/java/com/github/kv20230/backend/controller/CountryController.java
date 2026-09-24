package com.github.kv20230.backend.controller;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.RestCountryResponse;
import com.github.kv20230.backend.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
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


}