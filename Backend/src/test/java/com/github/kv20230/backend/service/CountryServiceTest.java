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
import static org.junit.jupiter.api.Assertions.assertSame;
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

    private static Country country(String code, String commonName, String officialName, String region, Long population) {
        return new Country(commonName, officialName, code.substring(0, 2), code, region, null, population, null,
                List.of(), List.of(), List.of(), List.of(), null, null, List.of(),
                List.of(), List.of(), null, null, null, null, null, null, null, null, null);
    }

    private static List<Country> sample() {
        return List.of(
                country("SVN", "Slovenia", "Republic of Slovenia", "Europe", 2_100_000L),
                country("JPN", "Japan", "Japan", "Asia", 124_000_000L),
                country("AUT", "Austria", "Republic of Austria", "Europe", 9_100_000L),
                country("ATA", "Antarctica", "Antarctica", null, null));
    }

    private static List<String> codes(List<Country> countries) {
        return countries.stream().map(Country::alpha3Code).toList();
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

    @Test
    void regionFilterIgnoresCaseAndSurroundingSpaces() {
        assertEquals(List.of("SVN", "AUT"), codes(service.getCountriesByRegion(sample(), "  europe ")));
    }

    @Test
    void unknownRegionIsEmpty() {
        assertTrue(service.getCountriesByRegion(sample(), "Atlantis").isEmpty());
    }

    @Test
    void blankRegionReturnsEveryCountry() {
        List<Country> all = sample();
        assertSame(all, service.getCountriesByRegion(all, " "));
        assertSame(all, service.getCountriesByRegion(all, null));
    }

    @Test
    void populationAscendingPutsMissingPopulationFirst() {
        assertEquals(List.of("ATA", "SVN", "AUT", "JPN"), codes(service.sortCountriesByPopulation(sample(), "asc")));
    }

    @Test
    void populationDescendingIgnoresCase() {
        assertEquals(List.of("JPN", "AUT", "SVN", "ATA"), codes(service.sortCountriesByPopulation(sample(), "DESC")));
    }

    @Test
    void anyOtherDirectionSortsAscending() {
        assertEquals(List.of("ATA", "SVN", "AUT", "JPN"), codes(service.sortCountriesByPopulation(sample(), "sideways")));
    }

    @Test
    void blankDirectionKeepsTheOriginalOrder() {
        List<Country> all = sample();
        assertSame(all, service.sortCountriesByPopulation(all, ""));
        assertSame(all, service.sortCountriesByPopulation(all, null));
    }

    @Test
    void codeLookupIgnoresCaseAndSurroundingSpaces() {
        assertEquals("Slovenia", service.getCountryByCode(sample(), " svn ").commonName());
    }

    @Test
    void unknownOrBlankCodeIs404() {
        for (String code : new String[]{"ZZZ", " ", null}) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getCountryByCode(sample(), code));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Test
    void nameSearchMatchesPartOfTheCommonName() {
        assertEquals(List.of("SVN"), codes(service.getCountriesByName(sample(), "  SLOV ")));
    }

    @Test
    void nameSearchAlsoMatchesTheOfficialName() {
        assertEquals(List.of("SVN", "AUT"), codes(service.getCountriesByName(sample(), "republic")));
    }

    @Test
    void nameSearchWithoutMatchIsEmpty() {
        assertTrue(service.getCountriesByName(sample(), "Narnia").isEmpty());
    }

    @Test
    void blankNameReturnsEveryCountry() {
        List<Country> all = sample();
        assertSame(all, service.getCountriesByName(all, ""));
        assertSame(all, service.getCountriesByName(all, null));
    }
}
