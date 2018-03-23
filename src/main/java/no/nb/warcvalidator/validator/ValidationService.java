package no.nb.warcvalidator.validator;

import no.nb.warcvalidator.config.AppConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.ArrayList;
import java.util.Set;

public class ValidationService {


    private AppConfig appConfig;

    public ValidationService(AppConfig config) {
        this.appConfig = config;
    }

    public boolean warcMovedToValid(String directory, String warc) {

        String folder = directory;
        File[] listFiles = new File(folder).listFiles();

        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].isFile()) {
                String fileName = listFiles[i].getName();
                String[] parts = fileName.split("_md5_");
                if (warc.contains(parts[0])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Takes .xml generated by Jhove, and checks status field in document to decide if the .warc file is valid or not.
     *
     * @param xmlReportFile
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    public boolean warcStatusIsValidAndWellFormed(File xmlReportFile)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document xmlDocument = documentBuilder.parse(xmlReportFile);

        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/jhove/repInfo/status";
        Node widgetNode = (Node) xpath.evaluate(expression, xmlDocument, XPathConstants.NODE);

        if (widgetNode != null) {
            String validWarc = "Well-Formed and valid";
            if (widgetNode.getTextContent().equalsIgnoreCase(validWarc)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
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
     * Copies a valid warc to another directory
     *
     * @param source
     * @param destination
     * @throws IOException
     */

    public void copyWarcToValidWarcsFolder(File source, File destination) throws IOException {
        try (
                InputStream is = new FileInputStream(source);
                OutputStream os = new FileOutputStream(destination);
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    public void setGroupOnFile(File file) throws IOException {
        String groupid = "1000";
        Path path = file.toPath();
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-r--");

        // Set permissioin
        Files.setPosixFilePermissions(path, perms);

        // Get group principal
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();

        GroupPrincipal group = lookupService.lookupPrincipalByGroupName(groupid);

        // Change group attribute
        Files.getFileAttributeView(path,PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
        //Files.setAttribute(path, "posix:group", group, LinkOption.NOFOLLOW_LINKS);
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

    public void validateWarc(String warcFilePath, String reportName) {
        JhoveWarcFileValidator validator = new JhoveWarcFileValidator(warcFilePath, reportName, appConfig);
        try {
            validator.validateFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
