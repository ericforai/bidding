package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V79__tender_trigram_search_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        String databaseProductName = context.getConnection()
                .getMetaData()
                .getDatabaseProductName()
                .toLowerCase(Locale.ROOT);
        if (!databaseProductName.contains("postgresql")) {
            return;
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("create extension if not exists pg_trgm");
            statement.execute("""
                    create index if not exists idx_tender_search_text_trgm
                        on tenders using gin (search_text_normalized gin_trgm_ops)
                    """);
            statement.execute("""
                    create index if not exists idx_tender_purchaser_name_trgm
                        on tenders using gin (purchaser_name_normalized gin_trgm_ops)
                    """);
        }
    }
}
