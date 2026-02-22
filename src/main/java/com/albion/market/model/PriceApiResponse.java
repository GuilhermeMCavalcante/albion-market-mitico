package com.albion.market.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceApiResponse(
        @JsonProperty("item_id") String itemId,
        String city,
        int quality,
        @JsonProperty("sell_price_min") long sellPriceMin,
        @JsonProperty("sell_price_min_date") String sellPriceMinDate,
        @JsonProperty("buy_price_max") long buyPriceMax,
        @JsonProperty("buy_price_max_date") String buyPriceMaxDate
) {
}
