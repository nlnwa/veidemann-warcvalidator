package no.nb.nna.veidemann.warcvalidator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import edu.harvard.hul.ois.jhove.JhoveException;
import no.nb.nna.veidemann.warcvalidator.model.WarcStatus;
import no.nb.nna.veidemann.warcvalidator.repo.RethinkRepository;
import no.nb.nna.veidemann.warcvalidator.service.ValidationService;
import no.nb.nna.veidemann.warcvalidator.settings.Settings;
import no.nb.nna.veidemann.warcvalidator.validator.JhoveWarcFileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WarcValidator {

    private static final Logger logger = LoggerFactory.getLogger(WarcValidator.class);
    private static final Settings SETTINGS;

    private final static int sleepTime;

    private final static String deliveryPermissions;
    private final static String deliveryGroupId;

    private final static Path warcsDirectory;
    private final static Path validWarcsDirectory;
    private final static Path invalidWarcsDirectory;
    private final static Path deliveryWarcsDirectory;

    static {
        Config config = ConfigFactory.load();
        config.checkValid(ConfigFactory.defaultReference());
        SETTINGS = ConfigBeanFactory.create(config, Settings.class);

        sleepTime = SETTINGS.getSleepTime();

        deliveryPermissions = SETTINGS.getDeliveryPermissions();
        deliveryGroupId = SETTINGS.getDeliveryGroupId();

        warcsDirectory = Paths.get(SETTINGS.getWarcDir()); // New warcs is placed here
        validWarcsDirectory = Paths.get(SETTINGS.getValidWarcDir()); // Well-formed and valid warcs  is placed here
        invalidWarcsDirectory = Paths.get(SETTINGS.getInvalidWarcDir()); // Warcs this isn't Well-formed and valid is placed here
        deliveryWarcsDirectory = Paths.get(SETTINGS.getDeliveryWarcDir()); // Valid warcs get copied here for further storing
    }

    public void start() {
        logger.info("Veidemann warcvalidator (v. {}) started", WarcValidator.class.getPackage().getImplementationVersion());

        try (RethinkRepository database = new RethinkRepository(SETTINGS.getDbHost(), SETTINGS.getDbPort(),
                SETTINGS.getDbUser(), SETTINGS.getDbPassword())) {

            final JhoveWarcFileValidator warcFileValidator = new JhoveWarcFileValidator(SETTINGS.getJhoveConfigPath());
            final ValidationService validationService = new ValidationService(database, warcFileValidator);

            startValidation(validationService);

        } catch (InterruptedException ex) {
            logger.info(ex.getLocalizedMessage(), ex);
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void startValidation(ValidationService service) throws InterruptedException {
        while (true) try {
            for (Path warcPath : service.findAllWarcs(warcsDirectory)) {
                // validate
                final Path reportPath = service.validateWarcFile(warcPath);
                // inspect report
                final WarcStatus warcStatus = service.inspectReport(reportPath);

                if (warcStatus.isValidAndWellFormed()) {
                    logger.debug(warcPath + " is well-formed and valid.");

                    final Path deliveryPath = deliveryWarcsDirectory.resolve(service.checksumFilename(warcPath));

                    // copy warc to delivery
                    Files.copy(warcPath, deliveryPath);

                    // set permissions/group on warc in delivery
                    service.setFileGroupId(deliveryPath, deliveryGroupId);
                    service.setFilePermissions(deliveryPath, deliveryPermissions);

                    // move warc and report to validwarcs
                    Files.move(warcPath, validWarcsDirectory.resolve(warcPath.getFileName()));
                    Files.move(reportPath, validWarcsDirectory.resolve(reportPath.getFileName()));
                } else {
                    logger.debug(warcPath + " contains errors");

                    // move warc and report to invalidwarcs
                    Files.move(warcPath, invalidWarcsDirectory.resolve(warcPath.getFileName()));
                    Files.move(reportPath, invalidWarcsDirectory.resolve(reportPath.getFileName()));
                }
                service.saveWarcStatus(warcStatus);
            }

            Thread.sleep(sleepTime * 1000);

        } catch (JhoveException | XMLStreamException | IOException ex) {
            logger.warn(ex.getLocalizedMessage(), ex);
        }
    }
}
