package ninja.foxyv.vsync.utils;

import ninja.foxyv.vsync.entities.DirectoryFingerprint;
import ninja.foxyv.vsync.entities.FileFingerprint;
import ninja.foxyv.vsync.react.fs.FingerprintMono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DirectorySyncUtils {
    public static Mono<DirectoryFingerprint> fingerprintDirectory(File aDirectory) {

        List<File> children = Arrays.asList(Objects.requireNonNull(aDirectory.listFiles(), "FOXE-7448228588131654119 - Could not list files for directory: " + aDirectory.getAbsolutePath()));

        Mono<List<DirectoryFingerprint>> subdirectoryFingerprints = Flux.fromIterable(children)
                .filter(File::isDirectory)
                .flatMap(DirectorySyncUtils::fingerprintDirectory)
                .collect(Collectors.toList());

        Mono<List<FileFingerprint>> fileFingerprints = Flux.fromIterable(children)
                .filter(File::isFile)
                .flatMap(DirectorySyncUtils::fingerprintFile)
                .collectList();

        return Mono.zip(subdirectoryFingerprints, fileFingerprints).map(t2 -> new DirectoryFingerprint(aDirectory.toPath(), t2.getT1(), t2.getT2()));
    }

    public static Mono<FileFingerprint> fingerprintFile(File aFile) {
        if (aFile == null) {
            return Mono.empty();
        }

        return FingerprintMono.fromFileBytesFlux(ReactiveFileUtils.streamFile(aFile));
    }

    public static Mono<FileFingerprint> fingerprintFile(File aFile, int bufferSize) {
        if (aFile == null) {
            return Mono.empty();
        }

        return FingerprintMono.fromFileBytesFlux(ReactiveFileUtils.streamFile(aFile, bufferSize));
    }

}
