package no.nb.nna.veidemann.warcvalidator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChecksumTest {

    @TempDir
    public File temporaryFolder;

    @Test
    public void createChecksumFile() throws IOException {
        ValidationService validationService = new ValidationService(null);
        Path tmp = temporaryFolder.toPath();

        // Generate files
        Files.writeString(tmp.resolve("file.txt"), "This is a text");

        // Generate checksum files, and assert
        try (DirectoryStream<Path> warcPaths = Files.newDirectoryStream(tmp)) {
            for (Path path : warcPaths) {
                Path sum = validationService.generateChecksumFile(path);

                String expected = validationService.md5sum(path);
                String actual = Files.readString(sum);
                String[] parts = actual.split("  ");

                assertEquals(2, parts.length);
                assertEquals(expected, parts[0]);
                assertEquals(path.getFileName().toString(), parts[1].stripTrailing());
            }
        } catch (NoSuchFileException e) {
            // warcs directory empty and non existant in remote git repo
        }
    }
}
