package no.nb.nna.veidemann.warcvalidator;

import edu.harvard.hul.ois.jhove.JhoveException;
import no.nb.nna.veidemann.warcvalidator.model.WarcError;
import no.nb.nna.veidemann.warcvalidator.repo.RethinkRepository;
import no.nb.nna.veidemann.warcvalidator.settings.Settings;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ValidationService {

    private RethinkRepository db;
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private Settings SETTINGS;

    public ValidationService(Settings settings, RethinkRepository rethinkRepository) {
        this.db = rethinkRepository;
        this.SETTINGS = settings;
    }

    /**
     * Generates md5sum for warcfile and includes it in filename
     *
     * @param warcfile
     * @return
     * @throws FileNotFoundException
     */
    public String generateMd5(File warcfile) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(warcfile);
        String filename = warcfile.getName();
        String[] parts = filename.split("(?=.warc)");
        String name = parts[0];
        String ending = parts[1];
        try {
            String md5 = DigestUtils.md5Hex(fis);
            fis.close();
            return name + "_md5_" + md5 + ending;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Helper method that generates a hashmap of a message field in the xml
     * so that all the attributes for the field is included in the message
     *
     * @param element
     * @return
     */
    private static HashMap<String, String> getMessageInXml(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        int numAttrs = attributes.getLength();
        HashMap<String, String> messageMap = new HashMap<>();
        String msg = element.getTextContent();
        messageMap.put("text", msg);
        for (int i = 0; i < numAttrs; i++) {
            Attr attr = (Attr) attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();
            messageMap.put(attrName, attrValue);
        }
        return messageMap;
    }


    public boolean warcStatusIsValidAndWellFormed(File xmlReportFile)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document xmlDocument = documentBuilder.parse(xmlReportFile);

        XPath xpath = XPathFactory.newInstance().newXPath();


        String fileName = xmlReportFile.getName();

        // Used to find the status field in  the xml
        String status = "/jhove/repInfo/status";
        Node statusNode = (Node) xpath.evaluate(status, xmlDocument, XPathConstants.NODE);


        // Lists for all the message fields in the xml
        NodeList messageList = xmlDocument.getElementsByTagName("message");

        // Find  Warc-Record-ID header value of Records with isNonCompliant value = true
        XPathExpression nonCompliant = xpath.compile("/jhove/repInfo/properties/property/values/property/values/value[@key='isNonCompliant value.'][text()='true']/preceding-sibling::value[@key='Warc-Record-ID header value.'][text()]");
        Object result = nonCompliant.evaluate(xmlDocument, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;

        // Checks if status field in xml is valid. If not create a WarcError Object and write it to DB.
        if (statusNode != null) {
            String validWarc = "Well-Formed and valid";
            if (statusNode.getTextContent().equalsIgnoreCase(validWarc)) {
                db.insertValidWarc(fileName);
                return true;
            } else {
                ArrayList<HashMap<String, String>> messages = new ArrayList();
                String statusText = statusNode.getTextContent();
                ArrayList<String> nonCompliantIds = new ArrayList();
                OffsetDateTime timestamp = OffsetDateTime.now();

                for (int i = 0; i < messageList.getLength(); i++) {
                    messages.add(getMessageInXml((Element) messageList.item(i)));
                }
                for (int j = 0; j < nodes.getLength(); j++) {
                    nonCompliantIds.add(nodes.item(j).getTextContent());
                }

                WarcError error = new WarcError(fileName, statusText, messages, nonCompliantIds, timestamp);
                db.insertFailedWarcInfo(error);
                return false;
            }
        }
        return false;
    }

    /**
     * Moves warc and xml reports to another directory
     *
     * @param source
     * @param target
     * @param file
     * @throws IOException
     */

    public void moveWarcToDirectory(String source, String target, String file) throws IOException {
        Path sourceDir = Paths.get(source);
        Path targetDir = Paths.get(target);

        Path sourceWarc = sourceDir.resolve(file);
        Path targetWarc = targetDir.resolve(file);

        Path sourceXml = sourceDir.resolve(file + ".xml");
        Path targetXml = targetDir.resolve(file + ".xml");
        try {
            Files.move(sourceWarc, targetWarc);
            Files.move(sourceXml, targetXml);
        } catch (FileAlreadyExistsException ex) {
            logger.warn("File already exists: " + ex);
        } catch (FileSystemException ex) {
            logger.warn("Error copying file to /validwarcs directory: " + ex);
        }
    }


    /**
     * Copies a valid warc to valid directory
     *
     * @param source
     * @param target
     * @throws IOException
     */

    public void copyToValid(String source, String target, String file, String md5Checksum) throws IOException {
        Path sourceFile = Paths.get(source).resolve(file);
        Path targetFile = Paths.get(target).resolve(md5Checksum);

        try {
            Files.copy(sourceFile, targetFile);
        } catch (FileAlreadyExistsException ex) {
            logger.warn("File already exists: " + ex);
        }

        setGroupOnFile(targetFile);
    }

    /**
     * Set file permissions on warcs
     * Used after a valid warc is moved to /validwarcs directory
     *
     * @param file
     * @throws IOException
     */
    public void setGroupOnFile(Path file) throws IOException { // before file
        String groupid = "10009";
        // Path path = file.toPath();
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-r--");

        // Set permissioin
        Files.setPosixFilePermissions(file, perms);

        // Get group principal
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();

        GroupPrincipal group = lookupService.lookupPrincipalByGroupName(groupid);

        // Change group attribute !REMOVE when running in debug
        Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
    }

    /**
     * Used to find all warc files in a directory which is ready to be processed.
     *
     * @param FileDirectory
     * @return
     */

    public ArrayList<File> findAllWarcs(File[] FileDirectory) {
        ArrayList<File> warcFiles = new ArrayList<>();
        for (File file : FileDirectory) {
            if (warcIsReady(file)) {
                warcFiles.add(file);
            }
        }
        return warcFiles;
    }

    /**
     * Used to define if a file is a ready .warc file
     * <p>
     * A file is considered a warc if the file ends with .warc or .warc.gz,
     * and ready if does not end with open.warc or open.warc.gz
     *
     * @param warc
     * @return
     */

    public boolean warcIsReady(File warc) {
        final String WARC_COMPRESSED_AND_OPEN = ".open.warc.gz";
        final String WARC_OPEN = ".open.warc";
        final String IS_WARC = ".warc";
        final String IS_COMPRESSED_WARC = ".warc.gz";

        boolean isOpen = warc.getName().endsWith(WARC_COMPRESSED_AND_OPEN) || warc.getName().endsWith(WARC_OPEN);
        boolean isReady = warc.getName().endsWith(IS_WARC) || warc.getName().endsWith(IS_COMPRESSED_WARC);

        if (isReady && !isOpen) {
            return true;
        }
        return false;
    }

    /**
     * Used to find warc validation reports in a directory
     * <p>
     * A file is considered a report if it ends with .warc.xml or warc.gz.xml
     *
     * @param FileDirectory
     * @return
     */
    public ArrayList<File> findAllReports(File[] FileDirectory) {
        ArrayList<File> reportFiles = new ArrayList<>();
        for (File file : FileDirectory) {
            if (file.getName().endsWith(".warc.gz.xml") || file.getName().endsWith(".warc.xml")) {
                reportFiles.add(file);
            }
        }
        return reportFiles;
    }

    /**
     * Search through list of reports to find a report that matches the name of a warc file.
     *
     * @param warcReports
     * @param warcFileName
     * @return
     */

    public File reportForWarcExist(ArrayList<File> warcReports, String warcFileName) {
        String warcValidationReportName = warcFileName + ".xml";
        for (File report : warcReports) {
            if (warcValidationReportName.equalsIgnoreCase(report.getName())) {
                return report;
            }
        }
        return null;
    }

    /**
     * Starts validation of .warc file using Jhove.
     *
     * @param warcFilePath
     * @param reportName
     */

    public void validateWarc(String warcFilePath, String reportName) throws JhoveException {
        JhoveWarcFileValidator validator = new JhoveWarcFileValidator(warcFilePath, reportName, SETTINGS);
        validator.validateFile();
    }

}
