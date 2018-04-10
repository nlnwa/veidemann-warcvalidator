package no.nb.nna.veidemann.warcvalidator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
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

    public WarcValidator start() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {

        RethinkRepository db = null;
        try {
            db = new RethinkRepository(SETTINGS.getDbHost(), SETTINGS.getDbPort(), SETTINGS.getDbName(),
                    SETTINGS.getDbUser(), SETTINGS.getDbPassword());
        } catch (Exception ex) {
            logger.error("Could not connect to DB");
        }

        int sleepBetweenChecks = SETTINGS.getSleepTime();

        File contentDirectory = new File(SETTINGS.getWarcDir());
        String validWarcsDirectory = SETTINGS.getValidWarcDir();

        String reportName;
        File[] files = contentDirectory.listFiles();

        ValidationService service = new ValidationService(SETTINGS, db);

        while (true) {

            if (files != null && files.length > 0) {
                logger.info("Will validate and move WARC files from directory: " + contentDirectory);
                logger.trace("Using Jhove config: " + SETTINGS.getJhoveConfigPath());
                logger.info("And move valid warcs to: " + validWarcsDirectory);

                ArrayList<File> warcs = service.findAllWarcs(files);
                ArrayList<File> reports = service.findAllReports(files);

                for (File warc : warcs) {
                    String warcFilename = warc.getName();
                    String warcFilePath = warc.getAbsolutePath();

                    // Check if .warc already exists in /validwarcs
                    if (service.warcMovedToValid(validWarcsDirectory, warcFilename)) {
                        logger.info(warcFilename + " already validated and moved to final directory");
                    } else {

                        // if not, check if jhove validation report with same name exists
                        File validationReport = service.reportForWarcExist(reports, warcFilename);
                        if (validationReport != null) {

                            // jhove report exists. Check validation status.
                            if (service.warcStatusIsValidAndWellFormed(validationReport)) {
                                logger.info(warcFilename +
                                        " , status: Well-formed and valid. Moving WARC to final directory");
                                // Report status is well formed and valid. Generate md5sum for file.
                                String md5Checksum = service.generateMd5(warc);

                                File md5Warc = new File(validWarcsDirectory + md5Checksum);

                                // Copy .warc file to /validwarcs with a filename including the md5sum
                                service.copyWarcToValidWarcsFolder(warc, md5Warc);

                                // Set file permissions
                                service.setGroupOnFile(md5Warc);
                            } else {
                                // Jhove report not valid
                                logger.debug("WARC: " + warcFilename + " contains errors, will not be moved");
                            }
                            // Jhove report for .warc file doesn't exist. Generate one using Jhove.
                        } else {
                            logger.info("Can't find a validation report for: " + warcFilename);
                            reportName = warcFilePath + ".xml";
                            logger.info("Will create report using Jhove, with name: " + reportName);
                            service.validateWarc(warcFilePath, reportName);
                            files = contentDirectory.listFiles();
                            reports = service.findAllReports(files);
                        }
                    }
                }
                // /warcs directory is empty
            } else {
                logger.debug("No files in directory to check.");
            }
            // set sleep time before next check
            try {
                logger.trace("Thread will sleep for: " + sleepBetweenChecks + " seconds");
                int sleepTime = sleepBetweenChecks * 1000;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Settings getSettings() { return SETTINGS; }
}
