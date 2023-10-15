package ninja.foxyv.vsync.utils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ReactiveFileUtils {
    public static Flux<ByteBuffer> streamFile(File aFile) {
        String absolutePath = aFile.getAbsolutePath();
        return Flux.using(() -> new FileInputStream(aFile), d -> nextBytes(d, absolutePath), is -> {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("FOXE-4170023500584451508 - Could not close file input stream for file: " + absolutePath, e);
            }
        });
    }

    private static Mono<ByteBuffer> nextBytes(InputStream d, String absolutePath) {
        return Mono.just(256).map(byte[]::new).map(buffer -> {
            try {
                int numRead = d.read(buffer);
                return ByteBuffer.wrap(buffer, 0, numRead);
            } catch (IOException e) {
                throw new RuntimeException("FOXE-9092525343245726548 - Could not read from file input stream for file: " + absolutePath, e);
            }
        });

    }
}
