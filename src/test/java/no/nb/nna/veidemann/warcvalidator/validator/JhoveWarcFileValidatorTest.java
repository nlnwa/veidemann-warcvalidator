package no.nb.nna.veidemann.warcvalidator.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class JhoveWarcFileValidatorTest {

    @TempDir
    public File temporaryFolder;

    @Test
    public void validate() throws IOException {
        Path resourceDirectory = Paths.get("src", "test", "resources");
        JhoveWarcFileValidator jwv = new JhoveWarcFileValidator(resourceDirectory.resolve("jhove.conf").toString());

        try (DirectoryStream<Path> warcPaths =
                     Files.newDirectoryStream(resourceDirectory.resolve("warcs"), path -> !path.toString().endsWith(".open") &&
                             (path.toString().endsWith(".warc") ||
                                     path.toString().endsWith(".warc.gz")))) {
            for (Path warcPath : warcPaths) {
                Path reportPath = temporaryFolder.toPath().resolve(warcPath.getFileName().toString() + ".xml");
                jwv.validate(warcPath, reportPath);
            }
        } catch (NoSuchFileException e) {
            // warcs directory empty and non existant in remote git repo
        }
    }
}
