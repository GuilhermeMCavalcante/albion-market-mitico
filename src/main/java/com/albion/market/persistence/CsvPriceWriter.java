package com.albion.market.persistence;

import com.albion.market.model.PriceRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvPriceWriter {
    public void write(Path path, List<PriceRecord> rows) throws IOException {
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        try (var writer = Files.newBufferedWriter(path)) {
            writer.write("item_id,item_name,tier,enchantment,city,quality,sell_price_min,sell_price_min_date,buy_price_max,buy_price_max_date,retrieved_at\n");
            for (PriceRecord row : rows) {
                writer.write(csv(row.itemId()) + "," + csv(row.itemName()) + "," + csv(row.tier()) + "," + csv(row.enchantment()) + ","
                        + csv(row.city()) + "," + row.quality() + "," + row.sellPriceMin() + "," + csv(row.sellPriceMinDate()) + ","
                        + row.buyPriceMax() + "," + csv(row.buyPriceMaxDate()) + "," + csv(row.retrievedAt()) + "\n");
            }
        }
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
