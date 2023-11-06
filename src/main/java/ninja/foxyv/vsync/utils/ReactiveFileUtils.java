package ninja.foxyv.vsync.utils;

import ninja.foxyv.vsync.react.fs.InputStreamFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ReactiveFileUtils {

    public static Flux<File> listFiles(File directory, boolean recursive) {
        if (directory.isFile()) {
            return Flux.just(directory);
        }

        if (recursive) {
            Flux<File> fileFlux = Mono.justOrEmpty(directory).filter(File::isDirectory)
                    .flatMap(f -> Mono.justOrEmpty(f.listFiles()).switchIfEmpty(Mono.just(new File[0])))
                    .flatMapMany(Flux::fromArray).flatMap(file -> listFiles(file, true));
            return Flux.merge(fileFlux, Flux.just(directory));
        } else {
            Flux<File> fileFlux = Mono.justOrEmpty(directory).filter(File::isDirectory)
                    .flatMap(f -> Mono.justOrEmpty(f.listFiles()).switchIfEmpty(Mono.just(new File[0]))).flatMapMany(Flux::fromArray);

            return Flux.merge(fileFlux);
        }
    }

    public static Flux<ByteBuffer> streamFile(File aFile) {
        return streamFile(aFile, InputStreamFlux.DEFAULT_BUFFER_SIZE);
    }

    public static Flux<ByteBuffer> streamFile(File aFile, int bufferSize) {
        String absolutePath = aFile.getAbsolutePath();
        return Flux.using(() -> new FileInputStream(aFile), is -> InputStreamFlux.flux(is, bufferSize), is -> closeStream(absolutePath, is));
    }

    private static void closeStream(String absolutePath, FileInputStream is) {
        try {
            is.close();
        } catch (IOException e) {
            throw new RuntimeException("FOXE-4170023500584451508 - Could not close file input stream for file: " + absolutePath, e);
        }
    }

}
