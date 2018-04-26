package no.nb.nna.veidemann.warcvalidator.repo;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlDriverError;
import com.rethinkdb.net.Connection;
import no.nb.nna.veidemann.warcvalidator.model.WarcError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class RethinkRepository implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(RethinkRepository.class);

    private static final String VERSION = "V1";
    private String DATABASE_NAME = "report";
    private final String UNVALID_WARCS_TABLE = "invalid_warcs";
    private final String VALID_WARCS_TABLE = "valid_warcs";

    private static final RethinkDB r = RethinkDB.r;
    private Connection connection;


    public RethinkRepository(String host, int port, String user, String password) {

        logger.info(RethinkRepository.VERSION + ": Starting up RethinkRepository. " + VERSION);

        try {
            connection = r.connection().hostname(host).port(port).user(user, password).connect();
            if (!connection.isOpen()) {
                throw new ReqlDriverError("Unable to connect to server at: " + host + " : " + port);
            }

            // check that we have the database
            boolean existsDatabase = r.dbList().contains(DATABASE_NAME).run(connection);
            if (!existsDatabase) {
                logger.info("Database doesn't exist, will try to create it.");
                r.dbCreate(DATABASE_NAME).run(connection);
            }

            // check that we have the tables
            boolean unvalidTableExists = r.db(DATABASE_NAME).tableList().contains(UNVALID_WARCS_TABLE).run(connection);
            if (!unvalidTableExists) {
                logger.info("invalid_warcs table doesn't exist, will try to create it");
                r.db(DATABASE_NAME).tableCreate(UNVALID_WARCS_TABLE).optArg("primary_key", "filename").run(connection);
            }

            boolean validTableExists = r.db(DATABASE_NAME).tableList().contains(VALID_WARCS_TABLE).run(connection);
            if(!validTableExists) {
                logger.info("valid_warcs table doesn't exist, will try to create it");
                r.db(DATABASE_NAME).tableCreate(VALID_WARCS_TABLE).optArg("primary_key", "filename").run(connection);
            }
        } catch (ReqlDriverError error) {
            logger.info("Unable to connect to server: " + host + " on port " + port);
            throw error;
        }
    }

    public void insertFailedWarcInfo(WarcError error) {
        r.db(DATABASE_NAME).table(UNVALID_WARCS_TABLE).insert(error).run(connection);
    }

    public void insertValidWarc(String filename) {
        OffsetDateTime nowDateTime = OffsetDateTime.now();

        r.db(DATABASE_NAME).table(VALID_WARCS_TABLE).insert(
                r.hashMap("filename", filename)
                .with("timestamp", nowDateTime)
        ).run(connection);
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            if (connection.isOpen()) {
                connection.close();
            }
        }
    }
}
