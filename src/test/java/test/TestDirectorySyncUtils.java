package test;

import ninja.foxyv.vsync.entities.DirectoryFingerprint;
import ninja.foxyv.vsync.entities.FileFingerprint;
import ninja.foxyv.vsync.utils.DirectorySyncUtils;
import ninja.foxyv.vsync.utils.ReactiveFileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Timed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

import static test.utils.TmpDirectoryUtils.prepareTMPDirectory;

public class TestDirectorySyncUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TestDirectorySyncUtils.class);

    @Test
    public void testFileList() throws IOException {
        File tmp = new File("tmp");
        prepareTMPDirectory(tmp);
        File testDir = new File(tmp, "test");
        testDir.mkdirs();
        File testFile = new File(testDir, "test.txt");
        Files.writeString(testFile.toPath(), "Rawr");

        List<File> files = ReactiveFileUtils.listFiles(tmp, true).collectList().switchIfEmpty(Mono.just(Collections.emptyList())).block();
        Assertions.assertNotNull(files);
        Assertions.assertFalse(files.isEmpty());
        files.stream().map(File::getAbsolutePath).forEach(System.out::println);
        Assertions.assertTrue(files.contains(testFile));
        List<File> filesNonRecursive = ReactiveFileUtils.listFiles(tmp, false).collectList().switchIfEmpty(Mono.just(Collections.emptyList())).block();
        Assertions.assertFalse(filesNonRecursive.contains(testFile));

    }

    @Test
    public void testFingerprintDirectory() {
        File tmp = new File("tmp");
        prepareTMPDirectory(tmp);

        DirectoryFingerprint directoryFingerprint = DirectorySyncUtils.fingerprintDirectory(tmp).block();
        Assertions.assertNotNull(directoryFingerprint);
        Assertions.assertEquals("tmp", directoryFingerprint.directoryName());
        Assertions.assertNotNull(directoryFingerprint.hash());
        Assertions.assertEquals(256 / 8, directoryFingerprint.hash().value().length);

        // Check for idempotence
        DirectoryFingerprint directoryFingerprint2 = DirectorySyncUtils.fingerprintDirectory(tmp).block();
        Assertions.assertNotNull(directoryFingerprint2);
        Assertions.assertNotNull(directoryFingerprint2.hash());
        Assertions.assertArrayEquals(directoryFingerprint.hash().value(), directoryFingerprint2.hash().value(), "Directory hash changed after 2nd invocation.");

    }

    @Test
    public void test() throws NoSuchAlgorithmException {
        File tmp = new File("tmp");
        prepareTMPDirectory(tmp);

        File mobyDick = new File(tmp, "MobyDick.txt");

        Timed<FileFingerprint> result = DirectorySyncUtils.fingerprintFile(mobyDick, 200000).timed().block();
        Assertions.assertNotNull(result);

        FileFingerprint fingerprint = result.get();

        Checksum crc32c = crc32c(mobyDick);

        Assertions.assertNotNull(fingerprint);
        Assertions.assertEquals(mobyDick.length(), fingerprint.length());
        Assertions.assertEquals(crc32c.getValue(), fingerprint.checksum().getValue());

        LOG.info("Fingerprinted " + result.get().length() + " bytes in " + result.elapsed().toMillis() + " milliseconds and " + result.elapsed().toNanosPart() + " nanos.");

        FileFingerprint fingerprint2 = DirectorySyncUtils.fingerprintFile(mobyDick, 1024).block();
        Assertions.assertNotNull(fingerprint2);
        Assertions.assertEquals(fingerprint.length(), fingerprint2.length());
        Assertions.assertEquals(fingerprint.checksum().getValue(), fingerprint2.checksum().getValue());
    }

    private Checksum crc32c(File mobyDick) throws NoSuchAlgorithmException {
        Checksum crc32c = new CRC32C();
        try(FileInputStream fis = new FileInputStream(mobyDick)) {
            int numRead;
            byte[] buffer = new byte[1024];
            while((numRead = fis.read(buffer)) != -1) {
                crc32c.update(buffer, 0, numRead);
            }
            return crc32c;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
