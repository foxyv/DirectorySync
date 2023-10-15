package ninja.foxyv.vsync.entities;

import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;

public record FileFingerprint(CRC32 crc32, CRC32C crc32C, Adler32 adler32, byte[] sha1, long length) {

}
