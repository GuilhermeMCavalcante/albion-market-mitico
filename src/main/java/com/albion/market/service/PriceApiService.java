package com.albion.market.service;

import com.albion.market.http.HttpJsonClient;
import com.albion.market.model.PriceApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

public class PriceApiService {
    private final HttpJsonClient client;
    private final String pricesBaseUrl;

    public PriceApiService(HttpJsonClient client, String pricesBaseUrl) {
        this.client = client;
        this.pricesBaseUrl = pricesBaseUrl;
    }

    public List<PriceApiResponse> fetchPrices(List<String> itemIds, List<String> cities, List<Integer> qualities) {
        String itemsPath = String.join(",", itemIds);
        String locations = encode(csv(cities));
        String quality = encode(csv(qualities));
        String url = "%s/%s.json?locations=%s&qualities=%s".formatted(pricesBaseUrl, itemsPath, locations, quality);
        return client.getJson(url, new TypeReference<>() {});
    }

    private String csv(List<?> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (Object value : values) {
            joiner.add(String.valueOf(value));
        }
        return joiner.toString();
    }

    private String encode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }
}
