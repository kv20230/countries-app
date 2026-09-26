package com.github.kv20230.backend.mapper;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.RestCountryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class CountryMapper {

    public Country toResponseDto(RestCountryResponse external) {
        return new Country(
                external.names() != null ? external.names().common() : null,
                external.names() != null ? external.names().official() : null,
                external.codes() != null ? external.codes().alpha2() : null,
                external.codes() != null ? external.codes().alpha3() : null,
                external.region(),
                external.subregion(),
                external.population(),
                external.area() != null ? external.area().kilometers() : null,
                external.capitals() != null ? external.capitals().stream().map(RestCountryResponse.Capital::name).toList() : List.of(),
                external.languages() != null ? external.languages().stream().map(RestCountryResponse.Language::name).toList() : List.of(),
                external.borders() != null ? external.borders() : List.of(),
                external.currencies() != null ? external.currencies().stream().map(CountryMapper::formatCurrency).toList() : List.of(),
                external.flag() != null ? external.flag().emoji() : null,
                external.flag() != null ? external.flag().urlPng() : null,
                external.timezones() != null ? external.timezones() : List.of(),
                external.callingCodes() != null ? external.callingCodes().stream().filter(Objects::nonNull).map(code -> code.startsWith("+") ? code : "+" + code).toList() : List.of(),
                external.tlds() != null ? external.tlds() : List.of(),
                external.cars() != null ? external.cars().drivingSide() : null,
                external.classification() != null ? external.classification().unMember() : null,
                external.memberships() != null ? external.memberships().eu() : null,
                external.landlocked(),
                external.governmentType(),
                external.demonyms() != null && external.demonyms().eng() != null ? external.demonyms().eng().m() : null,
                external.date() != null ? external.date().startOfWeek() : null,
                external.links() != null ? external.links().wikipedia() : null,
                external.descriptions() != null ? external.descriptions().shortDescription() : null
        );
    }

    /** "Euro (€)" when a symbol is known, otherwise just the name. */
    private static String formatCurrency(RestCountryResponse.Currency currency) {
        if (currency.symbol() == null || currency.symbol().isBlank() || currency.symbol().equals(currency.name())) {
            return currency.name();
        }
        return currency.name() + " (" + currency.symbol() + ")";
    }
}
