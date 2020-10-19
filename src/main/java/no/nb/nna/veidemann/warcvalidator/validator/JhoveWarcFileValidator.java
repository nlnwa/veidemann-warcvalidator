package no.nb.nna.veidemann.warcvalidator.validator;

import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.*;
import edu.harvard.hul.ois.jhove.handler.XmlHandler;
import edu.harvard.hul.ois.jhove.module.WarcModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Objects;


public class JhoveWarcFileValidator {
    private static final Logger logger = LoggerFactory.getLogger(JhoveWarcFileValidator.class);
    private final String release;
    private final String name = "Veidemann WARC validator";
    private final JhoveBase je;

    /**
     * @param configPath Path of Jhove config file
     */
    public JhoveWarcFileValidator(String configPath) {
        release = Objects.requireNonNullElse(
                JhoveWarcFileValidator.class.getPackage().getImplementationVersion(),
                "undefined");

        try {
            je = new JhoveBase();
            je.setLogLevel("info");
            je.setChecksumFlag(false);
            je.setShowRawFlag(false);
            je.setSignatureFlag(false);
            je.init(configPath, null);
        } catch (JhoveException e) {
            logger.error("Failed to initialise JHOVE");
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates a file using Open Preservation Foundation - Jhove, with the external WarcModule.
     *
     * @param warcPath   which .warc file to test
     * @param reportPath name of genereted validation report
     * @see <a href="https://github.com/openpreserve/jhove">JHOVE</a>
     */
    public void validate(Path warcPath, Path reportPath) {
        final OffsetDateTime utcDate = OffsetDateTime.now(ZoneOffset.UTC);
        final int[] date = {utcDate.getYear(), utcDate.getMonthValue(), utcDate.getDayOfMonth()};

        App app = new App(name, release, date, "", "");
        Module module = new WarcModule();
        module.setDefaultParams(new ArrayList<>());
        OutputHandler handler = new XmlHandler();

        try {
            je.dispatch(app, module, null, handler, reportPath.toString(), new String[]{warcPath.toString()});
        } catch (Exception e) {
            // je.dispatch throws Exception (which no library should)
            throw new RuntimeException(e);
        } finally {
            try {
                handler.close();
            } catch (Exception ex) {
                // noop
            }
        }
    }
}

