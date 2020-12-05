package com.epam.catgenome.util;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Creates an embedded Postgres database and extracts datasource from it, to be used in tests.
 * Used in test-applicationContext-database.xml for Postgres database
 */
public class EmbeddedPostgresDataSourceFactory {
    public DataSource getDataSource() throws IOException {
        EmbeddedPostgres db = EmbeddedPostgres.start();
        return db.getPostgresDatabase();
    }
}