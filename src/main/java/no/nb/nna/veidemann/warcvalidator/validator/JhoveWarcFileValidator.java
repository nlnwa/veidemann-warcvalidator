package no.nb.nna.veidemann.warcvalidator.validator;

import edu.harvard.hul.ois.jhove.*;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.handler.XmlHandler;
import edu.harvard.hul.ois.jhove.module.WarcModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;


public class JhoveWarcFileValidator {
    private static final Logger logger = LoggerFactory.getLogger(JhoveWarcFileValidator.class);
    private final String release = getClass().getPackage().getImplementationVersion();

    private Path jhoveConfigFile;

    /**
     * @param configPath Path of Jhove config file
     */
    public JhoveWarcFileValidator(String configPath) {
        jhoveConfigFile = Paths.get(configPath);

        logger.debug("Using Jhove config file: " + jhoveConfigFile);
    }

    /**
     * Validates a file using Open Preservation Foundation - Jhove, with the external WarcModule.
     *
     * @param warcPath   which .warc file to test
     * @param reportPath name of genereted validation report
     * @see <a href="https://github.com/openpreserve/jhove">JHOVE</a>
     */
    public void validate(Path warcPath, Path reportPath) throws JhoveException {
        OffsetDateTime timestamp = OffsetDateTime.now();
        int[] date = {timestamp.getYear(), timestamp.getDayOfMonth(), timestamp.getMonthValue()};

        App app = new App("Veidemann WARC validator", release, date, "", "");

        Module module = new WarcModule();
        module.setDefaultParams(new ArrayList<>());

        OutputHandler handler = new XmlHandler();

        JhoveBase je = new JhoveBase();
        je.setLogLevel("info");
        je.setChecksumFlag(false);
        je.setShowRawFlag(false);
        je.setSignatureFlag(false);
        je.init(jhoveConfigFile.toString(), null);

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

