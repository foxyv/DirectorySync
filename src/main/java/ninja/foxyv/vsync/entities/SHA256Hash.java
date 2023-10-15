package ninja.foxyv.vsync.entities;

public record SHA256Hash(byte[] value) {
    public SHA256Hash {
        if (value.length != 256 / 8) {
            throw new RuntimeException("FOXE-148245531121979067 - Hash value is not 256 bits!");
        }
    }
}
