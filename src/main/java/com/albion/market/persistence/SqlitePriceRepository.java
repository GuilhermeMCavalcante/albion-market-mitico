package com.albion.market.persistence;

import com.albion.market.model.PriceRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SqlitePriceRepository {
    public void save(Path dbPath, List<PriceRecord> rows) throws Exception {
        if (dbPath.getParent() != null) Files.createDirectories(dbPath.getParent());
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url)) {
            createTable(conn);
            upsertBatch(conn, rows);
        }
    }

    private void createTable(Connection conn) throws SQLException {
        String ddl = """
                CREATE TABLE IF NOT EXISTS prices (
                  item_id TEXT NOT NULL,
                  item_name TEXT,
                  tier INTEGER,
                  enchantment INTEGER,
                  city TEXT NOT NULL,
                  quality INTEGER NOT NULL,
                  sell_price_min INTEGER,
                  sell_price_min_date TEXT,
                  buy_price_max INTEGER,
                  buy_price_max_date TEXT,
                  retrieved_at TEXT NOT NULL,
                  PRIMARY KEY (item_id, city, quality, retrieved_at)
                );
                """;
        try (Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }

    private void upsertBatch(Connection conn, List<PriceRecord> rows) throws SQLException {
        conn.setAutoCommit(false);
        String sql = """
                INSERT INTO prices(item_id,item_name,tier,enchantment,city,quality,sell_price_min,sell_price_min_date,buy_price_max,buy_price_max_date,retrieved_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(item_id,city,quality,retrieved_at) DO UPDATE SET
                  item_name=excluded.item_name,
                  tier=excluded.tier,
                  enchantment=excluded.enchantment,
                  sell_price_min=excluded.sell_price_min,
                  sell_price_min_date=excluded.sell_price_min_date,
                  buy_price_max=excluded.buy_price_max,
                  buy_price_max_date=excluded.buy_price_max_date;
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PriceRecord row : rows) {
                ps.setString(1, row.itemId());
                ps.setString(2, row.itemName());
                if (row.tier() == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, row.tier());
                if (row.enchantment() == null) ps.setNull(4, java.sql.Types.INTEGER); else ps.setInt(4, row.enchantment());
                ps.setString(5, row.city());
                ps.setInt(6, row.quality());
                ps.setLong(7, row.sellPriceMin());
                ps.setString(8, row.sellPriceMinDate());
                ps.setLong(9, row.buyPriceMax());
                ps.setString(10, row.buyPriceMaxDate());
                ps.setString(11, row.retrievedAt());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
