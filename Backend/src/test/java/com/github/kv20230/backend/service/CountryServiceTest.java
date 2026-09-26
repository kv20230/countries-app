package com.github.kv20230.backend.service;

import com.github.kv20230.backend.client.RestCountryClient;
import com.github.kv20230.backend.mapper.CountryMapper;
import com.github.kv20230.backend.model.dto.Country;
import com.github.kv20230.backend.model.dto.PagedResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountryServiceTest {

    private final CountryService service = new CountryService(
            new RestCountryClient(new com.fasterxml.jackson.databind.ObjectMapper(), "http://localhost", "test"),
            new CountryMapper());

    private static Country country(int i) {
        return country("XX" + i, List.of());
    }

    private static Country country(String code, List<String> borders) {
        return new Country("C" + code, "Country " + code, code.substring(0, 2), code, "Europe", null, 1L, null,
                List.of(), List.of(), borders, List.of(), null, null, List.of(),
                List.of(), List.of(), null, null, null, null, null, null, null, null, null);
    }

    private static List<Country> countries(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(CountryServiceTest::country).toList();
    }

    @Test
    void bordersResolveCodesInOrderAndSkipUnknown() {
        List<Country> all = List.of(
                country("SVN", List.of("AUT", "HRV", "XXX", "ITA")),
                country("ITA", List.of("SVN")),
                country("HRV", List.of("SVN")),
                country("AUT", List.of("SVN")));

        List<String> borders = service.getBorders(all, "svn").stream().map(Country::alpha3Code).toList();

        assertEquals(List.of("AUT", "HRV", "ITA"), borders);
    }

    @Test
    void bordersOfUnknownCountryIs404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getBorders(countries(2), "ZZZ"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void firstPageHoldsSizeItemsAndReportsTotals() {
        PagedResponse<Country> page = service.paginate(countries(25), 1, 10);

        assertEquals(10, page.items().size());
        assertEquals("XX1", page.items().get(0).alpha3Code());
        assertEquals(1, page.page());
        assertEquals(10, page.size());
        assertEquals(25, page.totalItems());
        assertEquals(3, page.totalPages());
    }

    @Test
    void lastPageIsShorter() {
        PagedResponse<Country> page = service.paginate(countries(25), 3, 10);

        assertEquals(5, page.items().size());
        assertEquals("XX21", page.items().get(0).alpha3Code());
    }

    @Test
    void pagePastTheEndIsEmptyButKeepsTotals() {
        PagedResponse<Country> page = service.paginate(countries(25), 9, 10);

        assertTrue(page.items().isEmpty());
        assertEquals(25, page.totalItems());
        assertEquals(3, page.totalPages());
    }

    @Test
    void emptyListStillHasOnePage() {
        PagedResponse<Country> page = service.paginate(List.of(), 1, 10);

        assertTrue(page.items().isEmpty());
        assertEquals(0, page.totalItems());
        assertEquals(1, page.totalPages());
    }

    @Test
    void rejectsPageBelowOne() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.paginate(countries(3), 0, 10));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsSizeOutsideBounds() {
        assertThrows(ResponseStatusException.class, () -> service.paginate(countries(3), 1, 0));
        assertThrows(ResponseStatusException.class, () -> service.paginate(countries(3), 1, CountryService.MAX_PAGE_SIZE + 1));
    }
}
