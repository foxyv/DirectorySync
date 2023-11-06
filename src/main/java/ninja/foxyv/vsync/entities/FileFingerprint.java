package ninja.foxyv.vsync.entities;

import java.util.zip.Checksum;

public record FileFingerprint(String filename, Checksum checksum, long length) {

}
