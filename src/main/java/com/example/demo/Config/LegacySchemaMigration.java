package com.example.demo.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class LegacySchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.database-platform:}")
    private String databasePlatform;

    public LegacySchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!databasePlatform.toLowerCase().contains("h2")) {
            return;
        }
        addColumnIfMissing("products", "stock", "INTEGER DEFAULT 100");
        addColumnIfMissing("products", "branch_id", "BIGINT");
        addColumnIfMissing("orders", "source", "VARCHAR(50) DEFAULT 'ONLINE'");
        addColumnIfMissing("orders", "payment_status", "VARCHAR(50) DEFAULT 'PENDING'");
        addColumnIfMissing("orders", "payment_method", "VARCHAR(50) DEFAULT 'PAY_AT_PICKUP'");
        addColumnIfMissing("orders", "payment_reference", "VARCHAR(255)");
        addColumnIfMissing("orders", "branch_id", "BIGINT");
        addColumnIfMissing("orders", "telegram_chat_id", "BIGINT");
        addColumnIfMissing("users", "telegram_user_id", "BIGINT");
        addColumnIfMissing("users", "telegram_username", "VARCHAR(255)");

        safeUpdate("UPDATE products SET stock = 100 WHERE stock IS NULL");
        safeUpdate("UPDATE orders SET source = 'ONLINE' WHERE source IS NULL");
        safeUpdate("UPDATE orders SET payment_status = 'PENDING' WHERE payment_status IS NULL");
        safeUpdate("UPDATE orders SET payment_method = 'PAY_AT_PICKUP' WHERE payment_method IS NULL");
        safeUpdate("UPDATE cafe_tables SET qr_code = table_number || '-LYHOUR' WHERE qr_code IS NULL");
    }

    private void safeUpdate(String sql) {
        try {
            jdbcTemplate.update(sql);
        } catch (Exception ignored) {
            // Table doesn't exist yet or already up to date
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + definition);
        } catch (Exception ignored) {
            // Column already exists or table not created yet
        }
    }
}
