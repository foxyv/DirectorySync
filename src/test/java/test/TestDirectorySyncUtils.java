package test;

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
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

import static test.utils.TmpDirectoryUtils.prepareTMPDirectory;

public class TestDirectorySyncUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TestDirectorySyncUtils.class);

    @Test
    public void test() throws NoSuchAlgorithmException {
        File tmp = new File("tmp");
        prepareTMPDirectory(tmp);

        File mobyDick = new File(tmp, "MobyDick.txt");

        Timed<FileFingerprint> result = DirectorySyncUtils.fingerprintFile(mobyDick, 200000).timed().block();
        Assertions.assertNotNull(result);

        FileFingerprint fingerprint = result.get();

        CRC32 expectedcrc32 = checksum(mobyDick, new CRC32());
        CRC32C expectedCRC32C = checksum(mobyDick, new CRC32C());
        Adler32 expectedAdler32 = checksum(mobyDick, new Adler32());
        byte[] sha1 = sha1(mobyDick);

        Assertions.assertNotNull(fingerprint);
        Assertions.assertNotNull(fingerprint.crc32());
        Assertions.assertEquals(mobyDick.length(), fingerprint.length());
        Assertions.assertEquals(expectedcrc32.getValue(), fingerprint.crc32().getValue());
        Assertions.assertEquals(expectedCRC32C.getValue(), fingerprint.crc32C().getValue());
        Assertions.assertEquals(expectedAdler32.getValue(), fingerprint.adler32().getValue());
        Assertions.assertArrayEquals(sha1, fingerprint.sha1());

        LOG.info("Fingerprinted " + result.get().length() + " bytes in " + result.elapsed().toMillis() + " milliseconds and " + result.elapsed().toNanosPart() + " nanos.");
    }

    private byte[] sha1(File mobyDick) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
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


    private static <T extends Checksum> T checksum(File aFile, T checksum) {
        if (aFile == null || !aFile.isFile()) {
            throw new RuntimeException("FOXE-4262080697766779427 - File is not a file: " + Optional.ofNullable(aFile).map(File::getAbsolutePath).orElse("null"));
        }

        try(FileInputStream fis = new FileInputStream(aFile)) {
            int numRead;
            byte[] buffer = new byte[256];
            while((numRead = fis.read(buffer)) != -1) {
                ByteBuffer bb = ByteBuffer.wrap(buffer, 0, numRead);
                checksum.update(bb);
            }
            return checksum;
        } catch (IOException e) {
            throw new RuntimeException("FOXE-1479317106517931203 - Could not read from test file: " + aFile.getAbsolutePath(), e);
        }
    }

}
