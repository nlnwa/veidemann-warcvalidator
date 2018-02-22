package no.nb.warcvalidator;

import no.nb.warcvalidator.validator.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.ArrayList;

@SpringBootApplication
public class WarcValidationApplication {

    private static final Logger logger = LoggerFactory.getLogger(WarcValidationApplication.class);

    public static void main(String[] args) throws Exception {

        int sleepBetweenChecks;

        try {
            if (args.length != 1) {
                System.out.println("Application must have one input parameter");
                System.out.println("\t <Time to sleep between checks (seconds)");
                return;
            }

            sleepBetweenChecks = Integer.parseInt(args[0]);

            // Sett til persistentVolume som inneholder warc på cluster
            File contentDirectory = new File("/warcs");
            String validWarcDirectory = "/validwarcs";
            String reportName;
            File[] files = contentDirectory.listFiles();

            ValidationService service = new ValidationService();

            // Filområde for warc (contentWriter) er ikke tomt
            while (files != null) {
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
