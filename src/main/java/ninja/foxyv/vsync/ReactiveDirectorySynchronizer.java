package ninja.foxyv.vsync;

import ninja.foxyv.vsync.entities.CopyCounters;
import ninja.foxyv.vsync.entities.FileFingerprint;
import ninja.foxyv.vsync.utils.DirectorySyncUtils;
import ninja.foxyv.vsync.utils.ReactiveFileUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ReactiveDirectorySynchronizer {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveDirectorySynchronizer.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Scheduler progressScheduler = Schedulers.single();
        Scheduler fileScheduler = Schedulers.newParallel("FileCountScheduler", 100);

        File zDrive = new File("V:\\");
        File dDrive = new File("D:\\");

        File[] zDriveFiles = zDrive.listFiles();
        Arrays.stream(Objects.requireNonNull(zDriveFiles, "Could not list files in: " + zDrive.getAbsolutePath()))
                .filter(File::isDirectory)
                .forEach(file -> System.out.println(file.getAbsolutePath()));

        final long totalFiles = Optional.ofNullable(countFiles(zDrive, true, fileScheduler).block()).orElseThrow(() -> new RuntimeException("Could not count files. (NULL)"));
        AtomicReference<File> currentFile = new AtomicReference<>();
        CopyCounters copyCounter = new CopyCounters();
        try {
            progressScheduler.schedulePeriodically(() -> {
                        long remainingFiles = totalFiles - copyCounter.fileCounter.get() - copyCounter.filesHashed.get();
                        LOG.info("Current directory: " + currentFile.get().getName() + " - Files copied: " + copyCounter.fileCounter.get() + " - Bytes copied: " + FileUtils.byteCountToDisplaySize(copyCounter.byteCounter.get())
                                + " - Files hashed: " + copyCounter.filesHashed.get() + " - Remaining: " + remainingFiles);
                    },
                    1, 1, TimeUnit.SECONDS);

            for (File zDriveDirectory : zDriveFiles) {
                currentFile.set(zDriveDirectory);
                if (zDriveDirectory.getName().startsWith("$")) {
                    LOG.info("Skipping: " + zDriveDirectory.getAbsolutePath());
                    continue;
                }

                LOG.info("Copying directory: " + zDriveDirectory.getAbsolutePath());
                // Convert the path to its relative path from the source directory
                Path relativize = zDrive.toPath().relativize(zDriveDirectory.toPath());

                // Use the relative path to create a File location in the target directory
                File targetDirectory = new File(dDrive, relativize.toString());

                Long aLong = countFiles(zDriveDirectory, true, fileScheduler).block();

                LOG.debug("Total file(s) in " + zDriveDirectory.getAbsolutePath() + ": " + aLong);

                CompletableFuture<String> completableFuture = new CompletableFuture<>();

                copyDirectory(zDriveDirectory, targetDirectory, true, fileScheduler, copyCounter)
                        .doOnNext(file -> LOG.debug("Copied file: " + file.getAbsolutePath()))
                        .doOnComplete(() -> completableFuture.complete("Done"))
                        .doOnError(t -> LOG.error("FOXE-7137204502349216775 - Error copying file.", t))
                        .doOnTerminate(() -> completableFuture.completeExceptionally(new RuntimeException("FOXE-2058797770128287305 - Terminated.")))
                        .subscribe();

                completableFuture.get();

            }
        } finally {
            fileScheduler.disposeGracefully().block();
            progressScheduler.disposeGracefully().block();
        }

    }

    public static Mono<Long> countFiles(File directory, boolean recursive, Scheduler scheduler) {
        return filesInDirectory(directory, recursive, scheduler)
                // Turn this into a flux of grouped fluxes that we can do stuff with
                .groups()
                // Get a count from each group
                .flatMap(gf -> gf.collect(Collectors.counting()))
                // Sum up all the counts
                .collect(Collectors.summingLong(l -> l));
    }


    public static ParallelFlux<File> filesInDirectory(File directory, boolean recursive, Scheduler fileScheduler) {
        if (directory.isDirectory()) {
            if (!recursive) {
                // Emit all the files from the directory
                return Mono.just(directory)
                        .flatMapMany(
                                dir -> Optional.ofNullable(dir.listFiles())
                                        .map(Flux::fromArray)
                                        .orElse(Flux.empty())
                        )
                        .filter(File::isFile)
                        .subscribeOn(fileScheduler)
                        .parallel();
            } else {
                // Emit all the files and directories from the directory then recursively apply it
                return Mono.just(directory)
                        .flatMapMany(
                                dir -> Optional.ofNullable(dir.listFiles())
                                        .map(Flux::fromArray)
                                        .orElse(Flux.empty())
                        )
                        .subscribeOn(fileScheduler)
                        .parallel()
                        .flatMap(file -> filesInDirectory(file, true, fileScheduler))
                        .filter(File::isFile);
            }
        } else {
            // If a file is passed in then just return it.
            return Flux.just(directory).parallel();
        }

    }

    public static ParallelFlux<File> copyDirectory(File src, File target, boolean recursive, Scheduler fileScheduler, CopyCounters copyCounter) {
        return filesInDirectory(src, recursive, fileScheduler)
                .flatMap(file -> {
                    // Convert the path to its relative path from the source directory
                    Path relativize = src.toPath().relativize(file.toPath());

                    // Use the relative path to create a File location in the target directory
                    File targetFile = new File(target, relativize.toString());
                    return copyFile(file, targetFile, copyCounter);
                });
    }

    private static Mono<File> copyFile(File sourceFile, File targetFile, CopyCounters copyCounter) {
        if (targetFile.exists()) {
            return synchronizeFile(sourceFile, targetFile, copyCounter);
        }

        return Mono.fromCallable(() -> {
                    File parentDirectory = targetFile.getParentFile();
                    if (!parentDirectory.exists() && parentDirectory.mkdirs()) {
                        LOG.debug("Creating directory: " + parentDirectory.getAbsolutePath());
                    }
                    return targetFile;
                })
                .doOnNext(file -> LOG.debug("Copying file: " + sourceFile.getAbsolutePath() + " to: " + targetFile.getAbsolutePath()))
                .flatMap(file ->
                        Mono.using(
                            // Open the file output stream
                            () -> new FileOutputStream(targetFile),
                            // Write bytes from the input stream to the output stream
                            fos -> ReactiveFileUtils.streamFile(sourceFile).doOnNext(buffer -> {
                                try {
                                    copyCounter.byteCounter.addAndGet(buffer.remaining());
                                    fos.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
                                } catch (IOException e) {
                                    throw new RuntimeException("FOXE-9056604565732576339 - Could not save block to the file: " + targetFile.getAbsolutePath(), e);
                                }
                            }).last().doOnNext(bb -> copyCounter.fileCounter.getAndAdd(1)),
                            // Close the file output stream
                            fos -> {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    throw new RuntimeException("FOXE-9056604565732576339 - Could not close file output stream for file: " + targetFile.getAbsolutePath(), e);
                                }
                            })
                            .map(lastBytes -> targetFile));
    }

    private static Mono<File> synchronizeFile(File sourceFile, File targetFile, CopyCounters copyCounter) {
        Mono<FileFingerprint> sourceHashMono = DirectorySyncUtils.fingerprintFile(sourceFile);
        Mono<FileFingerprint> targetHashMono = DirectorySyncUtils.fingerprintFile(targetFile);


        return Mono.zip(sourceHashMono, targetHashMono)
                .doOnNext(tuple2 -> copyCounter.filesHashed.getAndIncrement())
                .filter(tuple2 -> tuple2.getT1().checksum().getValue() != tuple2.getT2().checksum().getValue())
                .flatMap(tuple2 -> {
                    LOG.debug("Synchronizing file: " + sourceFile.getAbsolutePath() + " to: " + targetFile.getAbsolutePath());
                    // Copy source file to target file
                    if(targetFile.delete()) {
                        LOG.info("Deleted old file: " + targetFile.getAbsolutePath());
                    }

                    // Copy the file now that the old one has been deleted.
                    return copyFile(sourceFile, targetFile, copyCounter);
                })
                .switchIfEmpty(Mono.just(targetFile));
    }

}
