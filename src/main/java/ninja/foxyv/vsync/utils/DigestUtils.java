package ninja.foxyv.vsync.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {
    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("FOXE-3064138332423778388 - SHA-256 hash algorithm is not available.", e);
        }
    }
}
