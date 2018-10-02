package no.nb.nna.veidemann.warcvalidator;

/**
 * Main class for launching the service.
 */
public final class Main {

    /**
     * Private constructor to avoid instantiation.
     */
    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        new WarcValidator().start();
    }
}
