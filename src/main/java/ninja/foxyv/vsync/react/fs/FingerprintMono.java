package ninja.foxyv.vsync.react.fs;

import ninja.foxyv.vsync.entities.FileFingerprint;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;

/**
 * The classic reduce, collect, and other flat map methods of converting a flux to a mono are not really compatible with
 * some checksums. This Mono will subscribe to the flux and accumulate the checksums of data from the flux.
 */
public class FingerprintMono extends Mono<FileFingerprint> {

    CompletableFuture<FileFingerprint> future = new CompletableFuture<>();
    final Flux<ByteBuffer> fileBytesFlux;
    final AtomicLong totalLength = new AtomicLong();
    final CRC32 crc32 = new CRC32();
    final CRC32C crc32c = new CRC32C();
    final Adler32 adler32 = new Adler32();
    final MessageDigest sha1;

    public static FingerprintMono fromFileBytesFlux(Flux<ByteBuffer> fileBytesFlux) {
        return new FingerprintMono(fileBytesFlux);
    }

    private FingerprintMono(Flux<ByteBuffer> fileBytesFlux) {
        this.fileBytesFlux = fileBytesFlux;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("FOXE-4018502283649996041 - SHA-1 algorithm not available.", e);
        }
    }


    @Override
    public void subscribe(CoreSubscriber<? super FileFingerprint> actual) {
        fileBytesFlux.doOnComplete(this::complete).doOnCancel(this::cancel).doOnError(t -> this.future.completeExceptionally(t)).subscribe(buffer -> {
            // Subscribe to the flux to iterate across the chunks of data from the file and update our checksums
            this.totalLength.getAndAdd(buffer.remaining());
            this.crc32.update(buffer.array(), buffer.arrayOffset(), buffer.remaining());
            this.crc32c.update(buffer.array(), buffer.arrayOffset(), buffer.remaining());
            this.adler32.update(buffer.array(), buffer.arrayOffset(), buffer.remaining());
            this.sha1.update(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        });

        // Use the completable future to wait for the result
        Mono.fromFuture(this.future).subscribe(actual);
    }

    private void cancel() {
        this.future.completeExceptionally(new RuntimeException("FOXE-630216344922882352 - File fingerprint was cancelled before completing."));
    }

    public void complete() {
        future.complete(new FileFingerprint(this.crc32, this.crc32c, this.adler32, this.sha1.digest(), this.totalLength.get()));
    }
}
