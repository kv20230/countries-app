package com.github.kv20230.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kv20230.backend.model.dto.RestCountryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class RestCountryClient {

    private static final int PAGE_LIMIT = 100;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    //receives baseUrl and apiKey from application.yaml (properties)
    public RestCountryClient(ObjectMapper objectMapper,
                             @Value("${restcountries.api.url}") String baseUrl,
                             @Value("${restcountries.api.key}") String apiKey) {

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Accept", "application/json")
                .build();

        this.objectMapper = objectMapper;
    }

    public List<RestCountryResponse> fetchAllCountries() {
        List<RestCountryResponse> allCountries = new ArrayList<>();
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            List<RestCountryResponse> page = fetchPage(PAGE_LIMIT, offset);

            if (!page.isEmpty()) {
                allCountries.addAll(page);
                offset += PAGE_LIMIT;

                if (page.size() < PAGE_LIMIT) {
                    hasMore = false;
                }
            } else {
                hasMore = false;
            }
        }

        return allCountries;
    }

    private List<RestCountryResponse> fetchPage(int limit, int offset) {
        try {
            String uri = String.format("/countries/v5?limit=%d&offset=%d", limit, offset);
            String rawResponse = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (rawResponse != null) {
                JsonNode rootNode = objectMapper.readTree(rawResponse);

                //field data.objects to access country list
                JsonNode objectsNode = rootNode.path("data").path("objects");

                if (objectsNode.isMissingNode() || !objectsNode.isArray()) {
                    objectsNode = rootNode.isArray() ? rootNode : rootNode.path("data");
                }

                if (objectsNode.isArray()) {
                    RestCountryResponse[] page = objectMapper.treeToValue(objectsNode, RestCountryResponse[].class);
                    return Arrays.asList(page);
                }
            }
        } catch (Exception e) {
            System.err.println("Napaka pri dekodiranju odgovora (offset=" + offset + "): " + e.getMessage());
            e.printStackTrace();
        }
        return List.of();
    }
}