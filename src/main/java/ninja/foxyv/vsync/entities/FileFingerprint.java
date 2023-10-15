package ninja.foxyv.vsync.entities;

import java.nio.file.Path;
import java.util.zip.Checksum;

public record FileFingerprint(Path file, FileChecksum checksum) {
    public record FileChecksum(Checksum checksum, long length) {

    }
}
