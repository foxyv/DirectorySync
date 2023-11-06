package test;

import ninja.foxyv.vsync.entities.CopyCounters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import test.utils.TmpDirectoryUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static ninja.foxyv.vsync.ReactiveDirectorySynchronizer.*;

public class TestDirectorySynchro {

    private static final Logger LOG = LoggerFactory.getLogger(TestDirectorySynchro.class);
    Scheduler scheduler = Schedulers.newParallel("Test", 10);
    @Test
    public void test() {
        File tmp = new File("tmp");

        String uuid = UUID.randomUUID().toString();

        // Create example source and target directories with files in them.
        File src = createTestSourceDirectory("source-" + uuid, tmp);
        File target = createTestTargetDirectory("target-" + uuid, tmp);

        try {
            Assertions.assertEquals(4, countFiles(src, true, scheduler).block());
            Assertions.assertEquals(3, countFiles(src, false, scheduler).block());
            Assertions.assertEquals(0, countFiles(target, true, scheduler).block());

            List<File> filesInTestDirectory = filesInDirectory(src, true, scheduler).sequential().collectList().block();
            Assertions.assertNotNull(filesInTestDirectory);
            Assertions.assertEquals(4, filesInTestDirectory.size());

            // Copy src directory to target directory
            CopyCounters copyCounter = new CopyCounters();
            copyDirectory(src, target, true, scheduler, copyCounter)
                    .subscribe(file -> LOG.info("Copying file: " + file.getAbsolutePath())
                    );

            Long fileCountNonRecursive = countFiles(target, false, scheduler).block();
            Assertions.assertNotNull(fileCountNonRecursive);
            Assertions.assertTrue(fileCountNonRecursive > 0, "No files were copied...");
            Assertions.assertEquals(3, fileCountNonRecursive);
            Assertions.assertEquals(4, countFiles(target, true, scheduler).block());

        } finally {
            deleteRecursive(src);
            deleteRecursive(target);
        }
    }

    private void deleteRecursive(File target) {
        if (target.isDirectory()) {
            for (File file : Objects.requireNonNull(target.listFiles())) {
                deleteRecursive(file);
            }
        }

        if (!target.delete()) {
            throw new RuntimeException("Could not delete file: " + target.getAbsolutePath());
        }
    }

    private File createTestTargetDirectory(String name, File tmp) {
        File testTargetDirectory = new File(tmp, name);
        if (testTargetDirectory.exists()) {
            throw new RuntimeException("Somehow the test target directory already exists?");
        }

        if (!testTargetDirectory.mkdirs()) {
            throw new RuntimeException("Unable to create test target directory.");
        }

        return testTargetDirectory;
    }

    private File createTestSourceDirectory(String name, File tmp) {
        File testSourceDirectory = new File(tmp, name);
        if (testSourceDirectory.exists()) {
            throw new RuntimeException("Somehow the test target directory already exists?");
        }

        if (!testSourceDirectory.mkdirs()) {
            throw new RuntimeException("Unable to create test target directory.");
        }

        TmpDirectoryUtils.copyClasspathFileTo("files/MobyDick.txt", testSourceDirectory);
        TmpDirectoryUtils.copyClasspathFileTo("files/PrideAndPrejudice.txt", testSourceDirectory);
        TmpDirectoryUtils.copyClasspathFileTo("files/RomeoAndJuliet.txt", testSourceDirectory);

        File imagesDirectory = new File(testSourceDirectory, "images");
        if (!imagesDirectory.mkdirs()) {
            throw new RuntimeException("Could not create images directory.");
        }

        TmpDirectoryUtils.copyClasspathFileTo("files/ThreeRectangles.png", imagesDirectory);


        return testSourceDirectory;
    }
}
