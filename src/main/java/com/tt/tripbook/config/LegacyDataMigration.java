package com.tt.tripbook.config;

import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.repository.UserRepository;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// One-time upgrade of a database created before accounts owned data. Safe to run on every start:
// once there is nothing left to adopt or drop, it does nothing.
//
// 1. Passengers without an owner (and, through them, their trips) are given to the first account.
// 2. MySQL only: Hibernate's ddl-auto=update never removes constraints, so the old global unique index
//    on passenger.document_number is dropped here. The per-account unique constraint replaces it.
@Component
@Order(1)
public class LegacyDataMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataMigration.class);

    private final UserRepository users;
    private final PassengerRepository passengers;
    private final DataSource dataSource;

    public LegacyDataMigration(UserRepository users, PassengerRepository passengers, DataSource dataSource) {
        this.users = users;
        this.passengers = passengers;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        adoptOrphanPassengers();
        dropOldDocumentNumberIndex();
    }

    private void adoptOrphanPassengers() {
        users.findAll(Sort.by("id")).stream().findFirst().ifPresent((AppUser first) -> {
            int adopted = passengers.assignOrphansTo(first);
            if (adopted > 0) {
                log.info("Assigned {} passengers created before accounts to {}", adopted, first.getEmail());
            }
        });
    }

    private void dropOldDocumentNumberIndex() {
        try {
            if (!isMySql()) {
                return;
            }
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            // Unique indexes made only of document_number (the composite one has two columns)
            List<String> indexes = jdbc.queryForList(
                    "SELECT INDEX_NAME FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'passenger' AND NON_UNIQUE = 0 "
                            + "GROUP BY INDEX_NAME HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME) = 'document_number'",
                    String.class);
            for (String index : indexes) {
                jdbc.execute("ALTER TABLE passenger DROP INDEX `" + index + "`");
                log.info("Dropped the old global unique index {} on passenger.document_number", index);
            }
        } catch (Exception ex) {
            // Not fatal: the app works, but two accounts cannot register the same document number
            log.warn("Could not drop the old unique index on passenger.document_number: {}", ex.getMessage());
        }
    }

    private boolean isMySql() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return "MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }
}
