package no.nb.nna.veidemann.warcvalidator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import edu.harvard.hul.ois.jhove.JhoveException;
import no.nb.nna.veidemann.warcvalidator.repo.RethinkRepository;
import no.nb.nna.veidemann.warcvalidator.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class WarcValidator {

    private static final Logger logger = LoggerFactory.getLogger(WarcValidator.class);
    private static final Settings SETTINGS;

    static {
        Config config = ConfigFactory.load();
        config.checkValid(ConfigFactory.defaultReference());
        SETTINGS = ConfigBeanFactory.create(config, Settings.class);
    }

    public WarcValidator() {
    }

    public void start() {
        try {
            startValidation();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void startValidation() {

        RethinkRepository db = null;
        try {
            db = new RethinkRepository(SETTINGS.getDbHost(), SETTINGS.getDbPort(),
                    SETTINGS.getDbUser(), SETTINGS.getDbPassword());
        } catch (Exception ex) {
            logger.error("Could not connect to DB");
        }

        int sleepBetweenChecks = SETTINGS.getSleepTime();

        File contentDirectory = new File(SETTINGS.getWarcDir());
        String warcsDirectory = SETTINGS.getWarcDir(); // New warcs is placed here
        String validWarcsDirectory = SETTINGS.getValidWarcDir(); // Well-formed and valid warcs  is placed here
        String invalidWarcsDirectory = SETTINGS.getInvalidWarcDir(); // Warcs this isn't Well-formed and valid is placed here
        String deliveryWarcsDirectory = SETTINGS.getDeliveryWarcDir(); // Walid warcs get copied here for further storing

        ValidationService service = new ValidationService(SETTINGS, db);

        while (true) {

            File[] files = contentDirectory.listFiles();

            if (files != null && files.length > 0) {
                logger.info("Will validate and move WARC files from directory: " + contentDirectory);
                logger.trace("Using Jhove config: " + SETTINGS.getJhoveConfigPath());

                ArrayList<File> warcs = service.findAllWarcs(files);
                ArrayList<File> reports = service.findAllReports(files);

                if (warcs.size() > 0) {

                    for (File warc : warcs) {
                        String warcFilename = warc.getName();
                        String warcFilePath = warc.getAbsolutePath();

                        // check if jhove validation report with same name exists
                        File validationReport = service.reportForWarcExist(reports, warcFilename);

                        try {
                            if (validationReport != null) {
                                // jhove report exists. Check validation status.
                                if (service.warcStatusIsValidAndWellFormed(validationReport)) {
                                    logger.info(warcFilename +
                                            " , status: Well-formed and valid. Moving WARC to valid and delivery directory");
                                    String md5Checksum = service.generateMd5(warc);
                                    service.copyToValid(warcsDirectory, deliveryWarcsDirectory, warcFilename, md5Checksum);
                                    service.moveWarcToDirectory(warcsDirectory, validWarcsDirectory, warcFilename);
                                } else {
                                    // Jhove report not valid
                                    logger.debug("WARC: " + warcFilename + " contains errors, will be moved to invalid directory");
                                    service.moveWarcToDirectory(warcsDirectory, invalidWarcsDirectory, warcFilename);

                                }

                                // Jhove report for .warc file doesn't exist. Generate one using Jhove.
                            } else {
                                logger.info("Can't find a validation report for: " + warcFilename);
                                String reportName = warcFilePath + ".xml";
                                logger.info("Will create report using Jhove, with name: " + reportName);
                                service.validateWarc(warcFilePath, reportName);
                                files = contentDirectory.listFiles();
                                reports = service.findAllReports(files);
                            }
                        } catch (JhoveException | SAXException | ParserConfigurationException |
                                XPathExpressionException | IOException ex) {
                            logger.warn(ex.getLocalizedMessage());
                        }
                    }
                    // Only .open files in /warcs
                } else {
                    logger.debug("Directory doesn't contain any closed files to check");
                }
                // /warcs directory is empty
            } else {
                logger.debug("No files in directory to check.");
            }
            try {
                logger.trace("Thread will sleep for: " + sleepBetweenChecks + " seconds");
                int sleepTime = sleepBetweenChecks * 1000;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Settings getSettings() {
        return SETTINGS;
    }
}
