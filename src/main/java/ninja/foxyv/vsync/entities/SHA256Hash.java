package ninja.foxyv.vsync.entities;

import java.util.Arrays;

public record SHA256Hash(byte[] value) {
    public SHA256Hash {
        if (value.length != 256 / 8) {
            throw new RuntimeException("FOXE-148245531121979067 - Hash value is not 256 bits!");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SHA256Hash that = (SHA256Hash) o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
