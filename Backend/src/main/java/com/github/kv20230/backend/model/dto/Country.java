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
        /** "Euro (€)" when a symbol is known, otherwise just the name. */
        List<String> currencies,
        String flagEmoji,
        String flagUrl,
        List<String> timezones,
        /** International dialling prefixes, already prefixed with "+". */
        List<String> callingCodes,
        List<String> topLevelDomains,
        String drivingSide,
        Boolean unMember,
        Boolean euMember,
        Boolean landlocked,
        String governmentType,
        String demonym,
        String startOfWeek,
        String wikipediaUrl,
        String description
) {
}
