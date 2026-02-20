package com.albion.market.cli;

import com.albion.market.config.AppConfig;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CliOverrides {
    private CliOverrides() {
    }

    public static AppConfig apply(AppConfig config, String[] args) {
        String cities = null;
        String qualities = null;
        String enchants = null;
        Integer concurrency = null;
        String out = null;
        String db = null;
        Integer sinceMinutes = config.sinceMinutes();

        for (String arg : args) {
            if (arg.startsWith("--cities=")) cities = arg.substring("--cities=".length());
            if (arg.startsWith("--qualities=")) qualities = arg.substring("--qualities=".length());
            if (arg.startsWith("--enchantments=")) enchants = arg.substring("--enchantments=".length());
            if (arg.startsWith("--concurrency=")) concurrency = Integer.parseInt(arg.substring("--concurrency=".length()));
            if (arg.startsWith("--out=")) out = arg.substring("--out=".length());
            if (arg.startsWith("--db=")) db = arg.substring("--db=".length());
            if (arg.startsWith("--sinceMinutes=")) sinceMinutes = Integer.parseInt(arg.substring("--sinceMinutes=".length()));
        }

        return new AppConfig(
                config.itemCatalogUrl(),
                config.pricesBaseUrl(),
                cities != null ? csvStrings(cities) : config.cities(),
                qualities != null ? csvInts(qualities) : config.qualities(),
                enchants != null ? csvInts(enchants).stream().collect(Collectors.toSet()) : config.enchantments(),
                concurrency != null ? concurrency : config.concurrency(),
                config.chunkSize(),
                config.maxRetries(),
                config.requestTimeout(),
                config.requestsPerSecond(),
                out != null ? Path.of(out) : config.outputCsvPath(),
                db != null ? Path.of(db) : config.sqliteDbPath(),
                sinceMinutes
        );
    }

    private static List<String> csvStrings(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private static List<Integer> csvInts(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).map(Integer::parseInt).toList();
    }
}
