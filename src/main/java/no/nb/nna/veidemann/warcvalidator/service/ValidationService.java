package no.nb.nna.veidemann.warcvalidator.service;

import no.nb.nna.veidemann.warcvalidator.validator.JhoveWarcFileValidator;
import org.apache.commons.codec.digest.DigestUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ValidationService {
    private final static String COMPRESSED_AND_OPEN_WARC_SUFFIX = ".warc.gz.open";
    private final static String OPEN_WARC_SUFFIX = ".warc.open";
    private final static String WARC_SUFFIX = ".warc";
    private final static String COMPRESSED_WARC_SUFFIX = ".warc.gz";
    private final static String REPORT_SUFFIX = ".xml";
    private final static String VALID_STATUS = "Well-Formed and valid";

    private final JhoveWarcFileValidator validator;

    public ValidationService(JhoveWarcFileValidator validator) {
        this.validator = validator;
    }

    /**
     * Validate warc file
     *
     * @param warcPath path of file to validate
     * @return path of generated report
     */
    public Path validateWarcFile(Path warcPath) {
        final Path reportPath = warcPath.resolveSibling(warcPath.getFileName() + REPORT_SUFFIX);
        validator.validate(warcPath, reportPath);
        return reportPath;
    }

    /**
     * Parse validation report and determine if warc is valid or not
     *
     * @param reportPath path of report
     * @return warc status
     */
    public boolean isWarcValid(Path reportPath) throws IOException, XMLStreamException {

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = factory.createXMLStreamReader(Files.newBufferedReader(reportPath));
        try {
            while (streamReader.hasNext()) {
                if (streamReader.next() == XMLStreamReader.START_ELEMENT) {
                    String elementName = streamReader.getLocalName();
                    if ("status".equals(elementName)) {
                        return streamReader.getElementText().equals(VALID_STATUS);
                    }
                }
            }
            return false;
        } finally {
            streamReader.close();
        }
    }

    /**
     * Find all warc files in a directory which is ready to be processed.
     *
     * @param directory File directory
     * @return Stream of warc paths
     */

    public DirectoryStream<Path> findAllWarcs(Path directory) throws IOException {
        return Files.newDirectoryStream(directory, path -> {
            boolean isOpen = path.toString().endsWith(COMPRESSED_AND_OPEN_WARC_SUFFIX) ||
                    path.toString().endsWith(OPEN_WARC_SUFFIX);
            boolean isWarc = path.toString().endsWith(WARC_SUFFIX) ||
                    path.toString().endsWith(COMPRESSED_WARC_SUFFIX);

            return isWarc && !isOpen;
        });
    }

    public Path generateChecksumFile(Path path) throws IOException {
        final String sep = "  ";
        Path sumPath = path.resolveSibling(path.getFileName() + ".md5");
        String sum = md5sum(path);
        Files.writeString(sumPath, sum + sep + path.getFileName().toString() + System.lineSeparator());
        return sumPath;
    }

    /**
     * Generates md5sum
     *
     * @param path path of file to generate checksumFilename from
     * @return checksumFilename
     */
    protected String md5sum(Path path) throws IOException {
        try (InputStream fis = Files.newInputStream(path)) {
            return DigestUtils.md5Hex(fis);
        }
    }
}
