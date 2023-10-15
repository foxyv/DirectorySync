package test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class TmpDirectoryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TmpDirectoryUtils.class);

    public static void prepareTMPDirectory(File tmp) {
        if (!tmp.exists() && tmp.mkdirs()) {
            LOG.info("Creating temporary directory: " + tmp.getAbsolutePath());
        }

        copyClasspathFileTo("TMP_README.md", tmp);
        copyClasspathFileTo("MobyDick.txt", tmp);
    }

    public static void copyClasspathFileTo(String classpath, File tmp) {
        File target = new File(tmp, classpath);
        if (target.exists()) {
            LOG.info("File has already been copied: " + classpath);
            return;
        }

        try (InputStream is = TmpDirectoryUtils.class.getClassLoader().getResourceAsStream(classpath); FileOutputStream fos = new FileOutputStream(target)) {
            Objects.requireNonNull(is, "FOXE-5348416516738332580 - Could not find classpath resource: " + classpath);

            byte[] buffer = new byte[256];
            int numRead;
            while ((numRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, numRead);
            }
        } catch (IOException e) {
            throw new RuntimeException("FOXE-5262996920082214375 - Could not copy test file to: " + target.getAbsolutePath(), e);
        }
    }
}
