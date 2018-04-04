package no.nb.warcvalidator;

import no.nb.warcvalidator.config.AppConfig;
import no.nb.warcvalidator.validator.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.ArrayList;

@SpringBootApplication
public class WarcValidationApplication {

    private static final Logger logger = LoggerFactory.getLogger(WarcValidationApplication.class);

    /**
     * Will in each loop look for any valid .warc files to copy to second location for further processing.
     * <p>
     * Application will look for .xml reports with the same name as the .warc file it's currently processing.
     * If the report doesn't exist, the jhove application (open preservation foundation) will generate a new one.
     * <p>
     * If the report does exist then the 'status' field of the .xml file will be checked.
     * If status = Well formed and valid, then the .warc file is moved to the /validwarcs directory.
     * <p>
     * After all .warc files in directory is checked, the thread will sleep for a given amount of time.
     *
     * @param args Seconds to sleep between loops. Can be set in pom.xml under docker build.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext run = SpringApplication.run(WarcValidationApplication.class, args);

        AppConfig appConfig = run.getBean(AppConfig.class);

        if (args.length != 1) {
            System.out.println("Application must have one input parameter");
            System.out.println("\t <Time to sleep between checks (seconds)");
            return;
        }

        int sleepBetweenChecks = Integer.parseInt(args[0]);

        File contentDirectory = new File(appConfig.getWarcsLocation());
        String validWarcDirectory = appConfig.getValidWarcsLocation();

        String reportName;
        File[] files = contentDirectory.listFiles();

        ValidationService service = new ValidationService(appConfig);

        // Check if warcs folder is empty
        while (true) {

            if (files != null && files.length > 0) {
                logger.info("Will validate and move WARC files from directory: " + contentDirectory);
                logger.trace("Using Jhove config: " + appConfig.getJhoveConfigFilePath());
                logger.info("And move valid warcs to: " + validWarcDirectory);
                ArrayList<File> warcs = service.findAllWarcs(files);
                ArrayList<File> reports = service.findAllReports(files);

                // For each .warc in directory
                for (File warc : warcs) {
                    String warcFilename = warc.getName();
                    String warcFilePath = warc.getAbsolutePath();

                    // Check if .warc already exists in /validwarcs
                    if (service.warcMovedToValid(validWarcDirectory, warcFilename)) {
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

                                File md5Warc = new File(validWarcDirectory + md5Checksum);

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
}
