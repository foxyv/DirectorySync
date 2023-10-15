package test;

import ninja.foxyv.vsync.entities.DirectoryFingerprint;
import ninja.foxyv.vsync.entities.FileFingerprint;
import ninja.foxyv.vsync.utils.DirectorySyncUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Timed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static test.utils.TmpDirectoryUtils.prepareTMPDirectory;

public class TestDirectorySyncUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TestDirectorySyncUtils.class);

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

        byte[] sha256 = sha256(mobyDick);

        Assertions.assertNotNull(fingerprint);
        Assertions.assertEquals(mobyDick.length(), fingerprint.length());
        Assertions.assertArrayEquals(sha256, fingerprint.sha256().value());

        LOG.info("Fingerprinted " + result.get().length() + " bytes in " + result.elapsed().toMillis() + " milliseconds and " + result.elapsed().toNanosPart() + " nanos.");

        FileFingerprint fingerprint2 = DirectorySyncUtils.fingerprintFile(mobyDick, 1024).block();
        Assertions.assertNotNull(fingerprint2);
        Assertions.assertEquals(fingerprint.length(), fingerprint2.length());
        Assertions.assertArrayEquals(fingerprint.sha256().value(), fingerprint2.sha256().value());
    }

    private byte[] sha256(File mobyDick) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
        try(FileInputStream fis = new FileInputStream(mobyDick)) {
            int numRead;
            byte[] buffer = new byte[1024];
            while((numRead = fis.read(buffer)) != -1) {
                sha1.update(buffer, 0, numRead);
            }
            return sha1.digest();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
