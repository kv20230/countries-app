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
        List<String> timezones,
        @JsonProperty("calling_codes") List<String> callingCodes,
        List<String> tlds,
        Cars cars,
        Classification classification,
        Boolean landlocked,
        @JsonProperty("government_type") String governmentType,
        Demonyms demonyms,
        @JsonProperty("date") DateInfo date,
        Memberships memberships,
        Links links,
        Descriptions descriptions
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cars(
            @JsonProperty("driving_side") String drivingSide
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Classification(
            @JsonProperty("un_member") Boolean unMember,
            Boolean sovereign
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Demonyms(
            Demonym eng
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Demonym(
            String m,
            String f
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DateInfo(
            @JsonProperty("start_of_week") String startOfWeek
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Memberships(
            Boolean eu,
            Boolean nato,
            Boolean schengen,
            Boolean eurozone
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Links(
            String wikipedia,
            String official
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Descriptions(
            @JsonProperty("short") String shortDescription,
            @JsonProperty("long") String longDescription
    ) {}
}
