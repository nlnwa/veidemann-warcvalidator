package no.nb.nna.veidemann.warcvalidator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import no.nb.nna.veidemann.warcvalidator.service.ValidationService;
import no.nb.nna.veidemann.warcvalidator.settings.Settings;
import no.nb.nna.veidemann.warcvalidator.validator.JhoveWarcFileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.*;

public class WarcValidator {

    private static final Logger logger = LoggerFactory.getLogger(WarcValidator.class);
    private static final Settings SETTINGS;

    private final static int sleepTime;

    private final static Path warcsDirectory;
    private final static Path validWarcsDirectory;
    private final static Path invalidWarcsDirectory;
    private final static boolean deleteReportIfValid;
    private final static boolean skipMove;
    private boolean isRunning;

    static {
        Config config = ConfigFactory.load();
        config.checkValid(ConfigFactory.defaultReference());
        SETTINGS = ConfigBeanFactory.create(config, Settings.class);

        sleepTime = SETTINGS.getSleepTime();
        skipMove = SETTINGS.isSkipMove();
        deleteReportIfValid = SETTINGS.isDeleteReportIfValid();
        warcsDirectory = Paths.get(SETTINGS.getWarcDir()); // New warcs is placed here
        validWarcsDirectory = Paths.get(SETTINGS.getValidWarcDir()); // Well-formed and valid warcs  is placed here
        invalidWarcsDirectory = Paths.get(SETTINGS.getInvalidWarcDir()); // Warcs this isn't Well-formed and valid is placed here
    }

    public WarcValidator() {
        isRunning = true;
    }

    public void start() {
        logger.info("Veidemann warcvalidator (v. {}) started", WarcValidator.class.getPackage().getImplementationVersion());

        try {
            final JhoveWarcFileValidator warcFileValidator = new JhoveWarcFileValidator(SETTINGS.getJhoveConfigPath());
            final ValidationService validationService = new ValidationService(warcFileValidator);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> isRunning = false));

            runValidation(validationService);
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Run validation loop:
     * 1. List warcs in path
     * 2. For each warc generate report
     * a. If report says warc is valid, then delete report (if configured such) and move warc to valid directory
     * b. Else move report and warc to invalid directory
     *
     * @param service Validation service that can validate warc files and determine validity
     */
    public void runValidation(ValidationService service) throws IOException {
        while (isRunning) {
            try (DirectoryStream<Path> warcPaths = service.findAllWarcs(warcsDirectory)) {
                for (Path warcPath : warcPaths) {
                    if (!isRunning) {
                        return;
                    }
                    logger.debug("Validating warc: {}", warcPath.toString());
                    final Path reportPath = service.validateWarcFile(warcPath);
                    final boolean isValid;

                    try {
                        isValid = service.isWarcValid(reportPath);
                    } catch (XMLStreamException ex) {
                        logger.warn(ex.getLocalizedMessage(), ex);
                        continue;
                    }

                    if (isValid) {
                        logger.debug(warcPath + " is valid");

                        if (!skipMove) {
                            // move warc to validwarcs
                            Files.move(warcPath, validWarcsDirectory.resolve(warcPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                        }
                        if (!deleteReportIfValid) {
                            // delete report
                            Files.delete(reportPath);
                        }
                    } else {
                        logger.warn(warcPath + " is invalid");

                        if (!skipMove) {
                            // move warc file and report to invalidwarcs
                            Files.move(warcPath, invalidWarcsDirectory.resolve(warcPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                            Files.move(reportPath, invalidWarcsDirectory.resolve(reportPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                }
            }
            try {
                Thread.sleep(sleepTime * 1000);
            } catch (InterruptedException ex) {
                // noop
            }
        }
    }
}
