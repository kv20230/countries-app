package com.github.kv20230.backend.controller;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.game.AnswerRequest;
import com.github.kv20230.backend.model.dto.game.AnswerResult;
import com.github.kv20230.backend.model.dto.game.FlagRound;
import com.github.kv20230.backend.service.CountryService;
import com.github.kv20230.backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@Tag(name = "Flag quiz", description = "Endpoints for the 'which country does this flag belong to?' minigame")
public class GameController {

    private final CountryService countryService;
    private final GameService gameService;

    public GameController(CountryService countryService, GameService gameService) {
        this.countryService = countryService;
        this.gameService = gameService;
    }

    @Operation(
            summary = "Draws the flags for a new game.",
            description = "Returns random, distinct countries that have a flag image. Only the code and the image URL are sent; "
                    + "the name is revealed by POST /api/game/answer. Fewer rounds come back when fewer countries qualify."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The rounds for one game"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid count",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Upstream REST Country API failure",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/flags")
    public List<FlagRound> getFlags(
            @Parameter(description = "Number of rounds (1-50)", example = "10")
            @RequestParam(defaultValue = "" + GameService.DEFAULT_ROUNDS) int count) {
        List<Country> cachedCountries = countryService.getAllCountries();
        return gameService.pickFlags(cachedCountries, count);
    }

    @Operation(
            summary = "Lists every country name.",
            description = "Sorted distinct common names, for the answer autocomplete."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All common names"),
            @ApiResponse(
                    responseCode = "502",
                    description = "Upstream REST Country API failure",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/names")
    public List<String> getNames() {
        return gameService.names(countryService.getAllCountries());
    }

    @Operation(
            summary = "Checks an answer.",
            description = "Compares the answer with the country's common and official name, ignoring case, accents and punctuation. "
                    + "A blank answer counts as a skip. The response reveals the correct name either way."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The verdict and the correct name"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing alpha3Code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No country with that code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping("/answer")
    public AnswerResult checkAnswer(@RequestBody AnswerRequest request) {
        if (request == null || request.alpha3Code() == null || request.alpha3Code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alpha3Code is required");
        }
        List<Country> cachedCountries = countryService.getAllCountries();
        return gameService.checkAnswer(cachedCountries, request.alpha3Code(), request.answer());
    }
}
