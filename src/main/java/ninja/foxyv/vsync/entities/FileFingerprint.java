package ninja.foxyv.vsync.entities;

public record FileFingerprint(String filename, SHA256Hash sha256, long length) {

}
