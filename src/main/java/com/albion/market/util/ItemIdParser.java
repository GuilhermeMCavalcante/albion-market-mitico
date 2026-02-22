package com.albion.market.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemIdParser {
    private static final Pattern TIER_PATTERN = Pattern.compile("^T(\\d+)_.*$");
    private static final Pattern ENCHANT_PATTERN = Pattern.compile(".*@(\\d+)$");

    private ItemIdParser() {
    }

    public static ParsedItemId parse(String itemId) {
        Integer tier = null;
        Integer enchant = 0;

        Matcher tierMatcher = TIER_PATTERN.matcher(itemId);
        if (tierMatcher.matches()) {
            tier = Integer.parseInt(tierMatcher.group(1));
        }

        Matcher enchantMatcher = ENCHANT_PATTERN.matcher(itemId);
        if (enchantMatcher.matches()) {
            enchant = Integer.parseInt(enchantMatcher.group(1));
        }

        return new ParsedItemId(tier, enchant);
    }

    public static String stripEnchantment(String itemId) {
        return itemId.replaceAll("@\\d+$", "");
    }

    public record ParsedItemId(Integer tier, Integer enchantment) {
    }
}
