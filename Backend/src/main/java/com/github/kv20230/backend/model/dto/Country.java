package com.github.kv20230.backend.model.dto;

import java.util.List;

public record Country(
        String commonName,
        String officialName,
        String alpha2Code,
        String alpha3Code,
        String region,
        String subregion,
        Long population,
        Double area,
        List<String> capitals,
        List<String> languages,
        List<String> borders,
        List<String> currencies,
        String flagEmoji,
        String flagUrl,
        List<String> timezones
) {}