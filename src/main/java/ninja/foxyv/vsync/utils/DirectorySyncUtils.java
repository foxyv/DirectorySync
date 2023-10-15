package ninja.foxyv.vsync.utils;

import ninja.foxyv.vsync.entities.DirectoryFingerprint;
import ninja.foxyv.vsync.entities.FileFingerprint;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class DirectorySyncUtils {
    public static Mono<DirectoryFingerprint> fingerprintDirectory(File aDirectory) {
        throw new RuntimeException("Not Yet Implemented");
    }

    public static Mono<FileFingerprint> fingerprintFile(File aFile) {
        if (aFile == null) {
            return Mono.empty();
        } else {
            return Mono.just(aFile)
                    .filter(File::exists)
                    .flatMap(DirectorySyncUtils::checksum)
                    .map(checksum -> new FileFingerprint(aFile.toPath(), checksum));
        }
    }

    private static Mono<FileFingerprint.FileChecksum> checksum(File file) {
        CRC32 checksum = new CRC32();
        long length = 0;


        return ReactiveFileUtils.streamFile(file).reduceWith(() -> new FileFingerprint.FileChecksum(new CRC32(), 0L), DirectorySyncUtils::updateChecksum);
    }

    private static FileFingerprint.FileChecksum updateChecksum(FileFingerprint.FileChecksum fingerprint, ByteBuffer byteBuffer) {
        long length = fingerprint.length() + (long) byteBuffer.remaining();
        fingerprint.checksum().update(byteBuffer);
        return new FileFingerprint.FileChecksum(fingerprint.checksum(), length);
    }
}
