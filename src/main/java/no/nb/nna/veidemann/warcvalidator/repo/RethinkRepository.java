package no.nb.nna.veidemann.warcvalidator.repo;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlDriverError;
import com.rethinkdb.net.Connection;
import no.nb.nna.veidemann.warcvalidator.model.WarcStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class RethinkRepository implements AutoCloseable {
    private static final RethinkDB r = RethinkDB.r;
    private static final String DATABASE_NAME = "report";
    private static final String INVALID_WARCS_TABLE = "invalid_warcs";
    private static final String VALID_WARCS_TABLE = "valid_warcs";

    private Connection connection;

    public RethinkRepository(String host, int port, String user, String password) {
        Logger logger = LoggerFactory.getLogger(RethinkRepository.class);

        try {
            connection = r.connection().hostname(host).port(port).user(user, password).connect();
        } catch (ReqlDriverError ex) {
            logger.error(String.format("Unable to connect to server at %s:%s", host, port));
            throw ex;
        }

        // check that we have the database
        boolean existsDatabase = r.dbList().contains(DATABASE_NAME).run(connection);
        if (!existsDatabase) {
            logger.info("Database doesn't exist, will try to create it.");
            r.dbCreate(DATABASE_NAME).run(connection);
        }

        // check that we have the tables
        boolean invalidTableExists = r.db(DATABASE_NAME).tableList().contains(INVALID_WARCS_TABLE).run(connection);
        if (!invalidTableExists) {
            logger.info("invalid_warcs table doesn't exist, will try to create it");
            r.db(DATABASE_NAME).tableCreate(INVALID_WARCS_TABLE).optArg("primary_key", "filename").run(connection);
        }

        boolean validTableExists = r.db(DATABASE_NAME).tableList().contains(VALID_WARCS_TABLE).run(connection);
        if (!validTableExists) {
            logger.info("valid_warcs table doesn't exist, will try to create it");
            r.db(DATABASE_NAME).tableCreate(VALID_WARCS_TABLE).optArg("primary_key", "filename").run(connection);
        }
    }

    public void insertFailedWarcInfo(WarcStatus warcStatus) {
        r.db(DATABASE_NAME).table(INVALID_WARCS_TABLE).insert(warcStatus).run(connection);
    }

    public void insertValidWarc(WarcStatus warcStatus) {
        OffsetDateTime nowDateTime = OffsetDateTime.now();

        r.db(DATABASE_NAME).table(VALID_WARCS_TABLE).insert(
                r.hashMap("filename", warcStatus.getFilename()).with("timestamp", nowDateTime)
        ).run(connection);
    }

    @Override
    public void close() {
        if (connection != null) {
            if (connection.isOpen()) {
                connection.close();
            }
        }
    }
}
