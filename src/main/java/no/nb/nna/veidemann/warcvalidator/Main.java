package no.nb.nna.veidemann.warcvalidator;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

/**
 * Main class for launching the service.
 */
public final class Main {

    /**
     * Private constructor to avoid instantiation.
     */
    private Main() {
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        new WarcValidator().start();
    }
}
