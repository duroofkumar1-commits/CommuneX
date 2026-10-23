package com.communex.common;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("db-inspect")
public class DatabaseInspector implements CommandLineRunner {
    private final DataSource dataSource;
    public DatabaseInspector(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void run(String... args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            System.out.println("\nCommuneX database metadata (read-only; no rows or schema modified)");
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), "public", "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String table = tables.getString("TABLE_NAME");
                    System.out.println("\n========== TABLE: " + table + " ==========");
                    try (ResultSet columns = metadata.getColumns(connection.getCatalog(), "public", table, null)) {
                        while (columns.next()) System.out.printf("%s | Type: %s | Nullable: %s%n", columns.getString("COLUMN_NAME"), columns.getString("TYPE_NAME"), columns.getString("IS_NULLABLE"));
                    }
                    try (ResultSet keys = metadata.getPrimaryKeys(connection.getCatalog(), "public", table)) {
                        while (keys.next()) System.out.printf("PRIMARY KEY: %s%n", keys.getString("COLUMN_NAME"));
                    }
                    try (ResultSet keys = metadata.getImportedKeys(connection.getCatalog(), "public", table)) {
                        while (keys.next()) System.out.printf("FOREIGN KEY: %s.%s -> %s.%s%n", table, keys.getString("FKCOLUMN_NAME"), keys.getString("PKTABLE_NAME"), keys.getString("PKCOLUMN_NAME"));
                    }
                }
            }
        }
    }
}
