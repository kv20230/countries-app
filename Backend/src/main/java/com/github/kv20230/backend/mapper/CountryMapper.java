package com.github.kv20230.backend.mapper;

import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.RestCountryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

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
                external.currencies() != null ? external.currencies().stream().map(RestCountryResponse.Currency::name).toList() : List.of(),
                external.flag() != null ? external.flag().emoji() : null,
                external.flag() != null ? external.flag().urlPng() : null,
                external.timezones() != null ? external.timezones() : List.of()
        );
    }
}