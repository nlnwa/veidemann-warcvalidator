package no.nb.nna.veidemann.warcvalidator.service;

import edu.harvard.hul.ois.jhove.JhoveException;
import no.nb.nna.veidemann.warcvalidator.model.WarcStatus;
import no.nb.nna.veidemann.warcvalidator.repo.RethinkRepository;
import no.nb.nna.veidemann.warcvalidator.validator.JhoveWarcFileValidator;
import org.apache.commons.codec.digest.DigestUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ValidationService {
    private final static String COMPRESSED_AND_OPEN_WARC_SUFFIX = ".warc.gz.open";
    private final static String OPEN_WARC_SUFFIX = ".warc.open";
    private final static String WARC_SUFFIX = ".warc";
    private final static String COMPRESSED_WARC_SUFFIX = ".warc.gz";
    private final static String MD5_CHECKSUM_PREFIX = "-md5_";
    private final static String REPORT_SUFFIX = ".xml";

    private JhoveWarcFileValidator validator;
    private RethinkRepository db;

    public ValidationService(RethinkRepository db, JhoveWarcFileValidator validator) {
        this.db = db;
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
     * Calculate checksum of file and insert checksum into path
     *
     * @param path path to file to checksum
     * @return path of file with checksum
     */
    public Path checksumFilename(Path path) throws IOException {
        return insertChecksumIntoPath(path, md5sum(path)).getFileName();
    }

    /**
     * Generates md5sum
     *
     * @param path path of file to generate checksumFilename from
     * @return checksumFilename
     */
    private String md5sum(Path path) throws IOException {
        try (InputStream fis = Files.newInputStream(path)) {
            return DigestUtils.md5Hex(fis);
        }
    }

    /**
     * Insert a checksum into given path
     *
     * @param path     path of checksummed file
     * @param checksum checksum
     * @return path with checksum
     */
    private Path insertChecksumIntoPath(Path path, String checksum) {
        String[] parts = path.toString().split("(?=.warc)");
        String name = parts[0];
        String ending = parts[1];
        return Paths.get(name + MD5_CHECKSUM_PREFIX + checksum + ending);
    }

    /**
     * Parse validation report
     *
     * @param reportPath path of report
     * @return warc status
     */
    public WarcStatus inspectReport(Path reportPath) throws IOException, XMLStreamException {
        final String warcRecordIDHeaderKey = "Warc-Record-ID header value.";
        final String isNonCompliantKey = "isNonCompliant value.";
        final String validStatus = "Well-Formed and valid";

        ArrayList<HashMap<String, String>> messages = new ArrayList<>();
        ArrayList<String> nonCompliantIds = new ArrayList<>();
        String status = "";
        String warcRecordIDHeader = "";

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = factory.createXMLStreamReader(Files.newBufferedReader(reportPath));
        try {
            while (streamReader.hasNext()) {
                if (streamReader.next() == XMLStreamReader.START_ELEMENT) {
                    String elementName = streamReader.getLocalName();

                    if ("status".equals(elementName)) {
                        status = streamReader.getElementText();
                        if (status.equals(validStatus)) {
                            break;
                        }
                    } else if ("value".equals(elementName)) {
                        String key = streamReader.getAttributeValue(null, "key");
                        if (key.equals(warcRecordIDHeaderKey)) {
                            warcRecordIDHeader = streamReader.getElementText();
                        } else if (key.equals(isNonCompliantKey)) {
                            boolean isNonCompliant = streamReader.getElementText().equals("true");
                            if (isNonCompliant) {
                                nonCompliantIds.add(warcRecordIDHeader);
                            }
                        }
                    } else if ("message".equals(elementName)) {
                        HashMap<String, String> messageMap = new HashMap<>();
                        for (int i = 0; i < streamReader.getAttributeCount(); i++) {
                            String key = streamReader.getAttributeLocalName(i);
                            String value = streamReader.getAttributeValue(i);
                            messageMap.put(key, value);
                        }
                        String message = streamReader.getElementText();
                        messageMap.put("text", message);
                        messages.add(messageMap);
                    }
                }
            }
            return new WarcStatus(reportPath.toString(), status, messages, nonCompliantIds, OffsetDateTime.now());
        } finally {
            streamReader.close();
        }
    }

    /**
     * Set file permissions on file
     *
     * @param path    path of file
     * @param groupID group ID of file
     */
    public void setFileGroupId(Path path, String groupID) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        GroupPrincipal group = lookupService.lookupPrincipalByGroupName(groupID);
        Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
    }

    /**
     * Set permissions
     *
     * @param path        path of file to set permissions on
     * @param permissions file permissions (e.g. "rwxrw-r--")
     */
    public void setFilePermissions(Path path, String permissions) throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissions);
        Files.setPosixFilePermissions(path, perms);
    }

    /**
     * Used to find all warc files in a directory which is ready to be processed.
     *
     * @param directory File directory
     * @return Array of warc files
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

    /**
     * Save warc status to database
     *
     * @param warcStatus validation status
     */
    public void saveWarcStatus(WarcStatus warcStatus) {
        if (warcStatus.isValidAndWellFormed()) {
            db.insertValidWarc(warcStatus);
        } else {
            db.insertFailedWarcInfo(warcStatus);
        }
    }
}
