package ninja.foxyv.vsync.utils;

import ninja.foxyv.vsync.entities.DirectoryFingerprint;
import ninja.foxyv.vsync.entities.FileFingerprint;
import ninja.foxyv.vsync.entities.SHA256Hash;
import ninja.foxyv.vsync.react.fs.FingerprintMono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DirectorySyncUtils {
    /**
     * Calculate fingerprints for the passed directory and it's subdirectories. Returning ONLY the passed directory fingerprint.
     *
     * @param aDirectory The directory to calculate the fingerprint for.
     * @return A mono that, when subscribed to, will provide the fingerprint for the directory passed.
     */
    public static Mono<DirectoryFingerprint> fingerprintDirectory(File aDirectory) {

        List<File> children = Arrays.asList(Objects.requireNonNull(aDirectory.listFiles(), "FOXE-7448228588131654119 - Could not list files for directory: " + aDirectory.getAbsolutePath()));

        // Collect the subdirectory fingerprints of the subdirectories
        Mono<List<DirectoryFingerprint>> subdirectoryFingerprints = Flux.fromIterable(children)
                .filter(File::isDirectory)
                .flatMap(DirectorySyncUtils::fingerprintDirectory)
                .collect(Collectors.toList());

        // Collect the file fingerprints for the files in the directory
        Mono<List<FileFingerprint>> fileFingerprints = Flux.fromIterable(children)
                .filter(File::isFile)
                .flatMap(DirectorySyncUtils::fingerprintFile)
                .collectList();

        return Mono.zip(subdirectoryFingerprints, fileFingerprints).map(t2 -> calculateFingerprint(aDirectory.getName(), t2.getT1(), t2.getT2()));
    }

    /**
     * Take the subdirectory fingerprints and the file fingerprints, then calculate a fingerprint for the directory.
     *
     * @param subdirectoryFingerprints The fingerprints of subdirectories.
     * @param fileFingerprints         The fingerprints of files in this directory.
     * @return The calculated fingerprint.
     */
    private static DirectoryFingerprint calculateFingerprint(String name, List<DirectoryFingerprint> subdirectoryFingerprints, List<FileFingerprint> fileFingerprints) {
        MessageDigest sha256 = DigestUtils.sha256();

        // Digest the subdirectory fingerprints after sorting by their names.
        subdirectoryFingerprints.stream()
                .sorted(Comparator.comparing(DirectoryFingerprint::directoryName))
                .forEach(directoryFingerprint -> {
                    sha256.update(directoryFingerprint.directoryName().getBytes(StandardCharsets.UTF_16));
                    sha256.update(directoryFingerprint.hash().value());
                });
        fileFingerprints.stream()
                .sorted(Comparator.comparing(FileFingerprint::filename))
                .forEach(fingerprint -> {
                    sha256.update(fingerprint.filename().getBytes(StandardCharsets.UTF_16));
                    sha256.update(fingerprint.sha256().value());
                });

        return new DirectoryFingerprint(name, new SHA256Hash(sha256.digest()));
    }


    public static Mono<FileFingerprint> fingerprintFile(File aFile) {
        if (aFile == null) {
            return Mono.empty();
        }

        return FingerprintMono.fromFileBytesFlux(aFile.getName(), ReactiveFileUtils.streamFile(aFile));
    }

    public static Mono<FileFingerprint> fingerprintFile(File aFile, int bufferSize) {
        if (aFile == null) {
            return Mono.empty();
        }

        return FingerprintMono.fromFileBytesFlux(aFile.getName(), ReactiveFileUtils.streamFile(aFile, bufferSize));
    }

}
