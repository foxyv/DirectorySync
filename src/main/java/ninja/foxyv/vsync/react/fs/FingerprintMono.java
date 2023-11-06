package ninja.foxyv.vsync.react.fs;

import ninja.foxyv.vsync.entities.FileFingerprint;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

/**
 * The classic reduce, collect, and other flat map methods of converting a flux to a mono are not really compatible with
 * some checksums. This Mono will subscribe to the flux and accumulate the checksums of data from the flux.
 */
public class FingerprintMono extends Mono<FileFingerprint> {

    final String filename;
    final CompletableFuture<FileFingerprint> future = new CompletableFuture<>();
    final Flux<ByteBuffer> fileBytesFlux;
    final AtomicLong totalLength = new AtomicLong();
    final Checksum checksum;

    public static FingerprintMono fromFileBytesFlux(String filename, Flux<ByteBuffer> fileBytesFlux) {
        return new FingerprintMono(filename, fileBytesFlux);
    }

    private FingerprintMono(String filename, Flux<ByteBuffer> fileBytesFlux) {
        this.filename = filename;
        this.fileBytesFlux = fileBytesFlux;
        this.checksum = new CRC32C();
    }


    @Override
    public void subscribe(CoreSubscriber<? super FileFingerprint> actual) {
        fileBytesFlux.doOnComplete(this::complete)
                .doOnCancel(this::cancel)
                .doOnError(this.future::completeExceptionally)
                .subscribe(buffer -> {
                    // Subscribe to the flux to iterate across the chunks of data from the file and update our checksums
                    this.totalLength.getAndAdd(buffer.remaining());
                    this.checksum.update(buffer.array(), buffer.arrayOffset(), buffer.remaining());
                });

        // Use the completable future to wait for the result
        Mono.fromFuture(this.future).subscribe(actual);
    }

    private void cancel() {
        this.future.completeExceptionally(new RuntimeException("FOXE-630216344922882352 - File fingerprint was cancelled before completing."));
    }

    public void complete() {
        future.complete(new FileFingerprint(filename, this.checksum, this.totalLength.get()));
    }
}
