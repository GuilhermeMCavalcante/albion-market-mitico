package com.albion.market.service;

import com.albion.market.config.AppConfig;
import com.albion.market.model.ItemDefinition;
import com.albion.market.model.PriceApiResponse;
import com.albion.market.model.PriceRecord;
import com.albion.market.util.Chunker;
import com.albion.market.util.ItemIdParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketCollectorService {
    private static final Logger log = LoggerFactory.getLogger(MarketCollectorService.class);

    private final PriceApiService priceApiService;

    public MarketCollectorService(PriceApiService priceApiService) {
        this.priceApiService = priceApiService;
    }

    public List<PriceRecord> collect(AppConfig config, List<ItemDefinition> catalog) {
        Map<String, String> nameByItemId = new HashMap<>();
        Set<String> queryItemIds = new LinkedHashSet<>();

        for (ItemDefinition item : catalog) {
            String base = ItemIdParser.stripEnchantment(item.itemId());
            nameByItemId.putIfAbsent(base, item.itemName());
            for (Integer enchant : config.enchantments()) {
                queryItemIds.add(enchant == 0 ? base : base + "@" + enchant);
            }
        }

        List<List<String>> chunks = Chunker.chunk(new ArrayList<>(queryItemIds), config.chunkSize());
        log.info("Fetching prices for {} item ids in {} chunks", queryItemIds.size(), chunks.size());

        ExecutorService executor = Executors.newFixedThreadPool(config.concurrency());
        AtomicInteger processed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        String retrievedAt = OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<CompletableFuture<List<PriceRecord>>> futures = chunks.stream().map(chunk ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        List<PriceApiResponse> responses = priceApiService.fetchPrices(chunk, config.cities(), config.qualities());
                        List<PriceRecord> rows = new ArrayList<>();
                        for (PriceApiResponse response : responses) {
                            if (shouldSkipBySince(config.sinceMinutes(), response)) {
                                continue;
                            }
                            ItemIdParser.ParsedItemId parsed = ItemIdParser.parse(response.itemId());
                            String baseId = ItemIdParser.stripEnchantment(response.itemId());
                            rows.add(new PriceRecord(
                                    response.itemId(),
                                    nameByItemId.get(baseId),
                                    parsed.tier(),
                                    parsed.enchantment(),
                                    response.city(),
                                    response.quality(),
                                    response.sellPriceMin(),
                                    response.sellPriceMinDate(),
                                    response.buyPriceMax(),
                                    response.buyPriceMaxDate(),
                                    retrievedAt
                            ));
                        }
                        int done = processed.addAndGet(chunk.size());
                        log.info("Progress: {}/{} items", done, queryItemIds.size());
                        return rows;
                    } catch (Exception e) {
                        failed.addAndGet(chunk.size());
                        log.error("Failed chunk with {} items", chunk.size(), e);
                        return List.<PriceRecord>of();
                    }
                }, executor)
        ).toList();

        List<PriceRecord> allRows = futures.stream().map(CompletableFuture::join).flatMap(List::stream).toList();
        shutdown(executor);

        log.info("Finished collection. Processed={}, failed={}, outputRows={}", processed.get(), failed.get(), allRows.size());
        return allRows;
    }

    private boolean shouldSkipBySince(Integer sinceMinutes, PriceApiResponse response) {
        if (sinceMinutes == null) {
            return false;
        }
        Instant cutoff = Instant.now().minusSeconds(sinceMinutes * 60L);
        Instant sell = parse(response.sellPriceMinDate());
        Instant buy = parse(response.buyPriceMaxDate());
        Instant newest = sell != null && buy != null ? (sell.isAfter(buy) ? sell : buy) : (sell != null ? sell : buy);
        return newest != null && newest.isBefore(cutoff);
    }

    private Instant parse(String value) {
        try {
            return value == null || value.isBlank() ? null : Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
