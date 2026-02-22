package com.albion.market.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemIdParserTest {

    @Test
    void parsesTierAndEnchantment() {
        ItemIdParser.ParsedItemId parsed = ItemIdParser.parse("T4_BAG@2");
        assertEquals(4, parsed.tier());
        assertEquals(2, parsed.enchantment());
    }

    @Test
    void parsesNoTierAndDefaultEnchant() {
        ItemIdParser.ParsedItemId parsed = ItemIdParser.parse("UNIQUE_SPECIAL_ITEM");
        assertNull(parsed.tier());
        assertEquals(0, parsed.enchantment());
    }

    @Test
    void stripsEnchantment() {
        assertEquals("T8_2H_BOW", ItemIdParser.stripEnchantment("T8_2H_BOW@3"));
    }
}
