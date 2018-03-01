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
     *
     * Application will look for .xml reports with the same name as the .warc file it's currently processing.
     * If the report doesn't exist, the jhove application (open preservation foundation) will generate a new one.
     *
     * If the report does exist then the 'status' field of the .xml file will be checked.
     * If status = Well formed and valid, then the .warc file is moved to the /validwarcs directory.
     *
     * After all .warc files in directory is checked, the thread will sleep for a given amount of time.
     *
     * @param args Seconds to sleep between loops
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext run = SpringApplication.run(WarcValidationApplication.class, args);

        AppConfig appConfig = run.getBean(AppConfig.class);

        try {

            if (args.length != 1) {
                System.out.println("Application must have one input parameter");
                System.out.println("\t <Time to sleep between checks (seconds)");
                return;
            }

            int sleepBetweenChecks = Integer.parseInt(args[0]);

            File contentDirectory = new File(appConfig.getWarcsLocation());
            String validWarcDirectory = appConfig.getValidWarcsLocation();

            String reportName;
            System.out.println("Listing files for folder: " + contentDirectory);
            System.out.println("Moving files to folder: " + validWarcDirectory);
            System.out.println("Config filen: " +appConfig.getJhoveConfigFilePath());
            File[] files = contentDirectory.listFiles();

            ValidationService service = new ValidationService(appConfig);

            // Filområde for warc (contentWriter) er ikke tomt
            while (true) {

                if (files != null && files.length > 0) {
                    logger.info("Will validate and move WARC files");
                    ArrayList<File> warcs = service.findAllWarcs(files);
                    ArrayList<File> reports = service.findAllReports(files);

                    // ser gjennom alle  warc-filer
                    for (File warc : warcs) {
                        String warcFilename = warc.getName();
                        String warcFilePath = warc.getAbsolutePath();

                        // Sjekker om warc-fil allerede ligger i valid katalog
                        if (service.warcMovedToValid(validWarcDirectory, warcFilename)) {
                            logger.info(warcFilename + " already validated and moved to final directory");
                        } else {

                            // Sjekker om warc-fil har en rapport
                            File validationReport = service.reportForWarcExist(reports, warcFilename);
                            if (validationReport != null) {

                                // har rapport fra før, sjekker status i rapport
                                if (service.warcStatusIsValidAndWellFormed(validationReport)) {
                                    logger.info(warcFilename +
                                            " , status: Well-formed and valid. Moving WARC to final directory");
                                    service.copyWarcToValidWarcsFolder(warc,
                                            new File(validWarcDirectory + warcFilename));
                                } else {
                                    logger.info("WARC: " + warcFilename + " contains errors, will not be moved");
                                }

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
                } else {
                    logger.info("No files in directory to check.");
                }
                try {
                    logger.info("Thread will sleep for: " + sleepBetweenChecks + " seconds");
                    int sleepTime = sleepBetweenChecks * 1000;
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {

        }
    }
}
