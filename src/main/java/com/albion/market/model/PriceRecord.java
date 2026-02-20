package com.albion.market.model;

public record PriceRecord(
        String itemId,
        String itemName,
        Integer tier,
        Integer enchantment,
        String city,
        int quality,
        long sellPriceMin,
        String sellPriceMinDate,
        long buyPriceMax,
        String buyPriceMaxDate,
        String retrievedAt
) {
}
