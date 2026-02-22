package com.albion.market.http;

import com.albion.market.model.PriceApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceApiResponseMappingTest {
    @Test
    void mapsAlbionDataResponse() throws Exception {
        String json = """
                [
                  {
                    "item_id": "T4_BAG",
                    "city": "Bridgewatch",
                    "quality": 1,
                    "sell_price_min": 1234,
                    "sell_price_min_date": "2025-01-01T00:00:00Z",
                    "buy_price_max": 1000,
                    "buy_price_max_date": "2025-01-01T00:01:00Z"
                  }
                ]
                """;

        ObjectMapper mapper = new ObjectMapper();
        List<PriceApiResponse> rows = mapper.readValue(json, new TypeReference<>() {});
        assertEquals(1, rows.size());
        assertEquals("T4_BAG", rows.get(0).itemId());
        assertEquals("Bridgewatch", rows.get(0).city());
        assertEquals(1234, rows.get(0).sellPriceMin());
    }
}
