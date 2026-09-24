package com.github.kv20230.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestCountryResponse(
        @JsonProperty("names") Name names,
        @JsonProperty("codes") Code codes,
        String region,
        String subregion,
        Long population,
        @JsonProperty("area") Area area,
        @JsonProperty("flag") Flag flag,
        List<String> borders,
        List<Capital> capitals,
        List<Language> languages,
        List<Currency> currencies,
        List<String> timezones
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Name(
            String common,
            String official
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Code(
            @JsonProperty("alpha_2") String alpha2,
            @JsonProperty("alpha_3") String alpha3
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Area(
            Double kilometers
            //Double miles
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Flag(
            String emoji,
            @JsonProperty("url_png") String urlPng,
            @JsonProperty("url_svg") String urlSvg,
            String description
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Capital(
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Currency(
            String code,
            String name,
            String symbol
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Language(
            String name,
            @JsonProperty("native_name") String nativeName
    ) {}
}

