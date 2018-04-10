package no.nb.nna.veidemann.warcvalidator;

import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.OutputHandler;
import edu.harvard.hul.ois.jhove.handler.XmlHandler;
import edu.harvard.hul.ois.jhove.module.WarcModule;
import no.nb.nna.veidemann.warcvalidator.settings.Settings;

import java.io.File;
import java.util.ArrayList;


public class JhoveWarcFileValidator {

    private File xmlOutFile;
    private File warcFilename;
    private Settings settings;

    /**
     * @param filename which .warc file to test
     * @param outFile  name of genereted validation report
     * @param settings which jhove config to use
     */

    public JhoveWarcFileValidator(String filename, String outFile, Settings settings) {

        this.settings = settings;

        this.warcFilename = new File(filename);

        if (!warcFilename.exists()) {
            throw new RuntimeException("Couldn't find .warc file: " + warcFilename.getAbsolutePath());
        }


        this.xmlOutFile = new File(outFile);
    }

    /**
     * Validates a file using Open Preservation Foundation - Jhove.
     *
     * @throws Exception
     * @https://github.com/openpreserve/jhove In this use case: .warc files through the external WarcModule.
     */

    public void validateFile() throws Exception {


        int time[] = {2018, 20, 02};

        App app = new App("Veidemann WARC validator", "1.0", time, "usage", "rights");


        JhoveBase je = new JhoveBase();
        OutputHandler handler = new XmlHandler();
        try {
            Module module = new WarcModule();
            module.setDefaultParams(new ArrayList<>());

            File conf = new File(settings.getJhoveConfigPath());

            if (!conf.exists()) {
                throw new RuntimeException("Couldn't find the file: " + conf.getAbsolutePath());
            } else {
                System.out.println("Using Jhove config file: " + settings.getJhoveConfigPath());
            }
            je.setLogLevel("info");
            je.init(conf.getAbsolutePath(), null);

            je.setEncoding("utf-8");
            je.setTempDirectory("src/test/resources/");
            je.setBufferSize(4096);
            je.setChecksumFlag(false);
            je.setShowRawFlag(false);
            je.setSignatureFlag(false);

            je.dispatch(app, module, null, handler, xmlOutFile.getAbsolutePath(), new String[]{warcFilename.getAbsolutePath()});
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
}

