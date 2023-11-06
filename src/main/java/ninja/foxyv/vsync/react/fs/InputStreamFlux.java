package ninja.foxyv.vsync.react.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of a bounded flux that will output the contents of the file in chunks. Each chunk will be wrapped
 * in a ByteBuffer.
 */
public class InputStreamFlux extends Flux<ByteBuffer> {

    private static final Logger LOG = LoggerFactory.getLogger(InputStreamFlux.class);

    // Default buffer size is 12kb
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 12;
    final InputStream is;
    final int bufferSize;

    final AtomicBoolean subscribed = new AtomicBoolean(false);

    public static Flux<ByteBuffer> flux(InputStream is) {
        return new InputStreamFlux(is);
    }

    public static Flux<ByteBuffer> flux(InputStream is, int bufferSize) {
        return new InputStreamFlux(is, bufferSize);
    }

    private InputStreamFlux(InputStream is) {
        super();
        this.is = is;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
    }

    private InputStreamFlux(InputStream is, int bufferSize) {
        this.is = is;
        this.bufferSize = bufferSize;
    }


    @Override
    public void subscribe(CoreSubscriber<? super ByteBuffer> subscriber) {
        boolean alreadySubscribed = subscribed.getAndSet(true);
        if (alreadySubscribed) {
            subscriber.onError(new RuntimeException("FOXE-3459870034010402937 - Input stream published can only be subscribed to once."));
        }

        long totalRead = 0;
        try {
            int numRead;
            byte[] buffer = new byte[bufferSize];
            while ((numRead = is.read(buffer)) != -1) {
                ByteBuffer bb = ByteBuffer.allocate(numRead);
                bb.put(buffer, 0, numRead);
                bb.flip();
                subscriber.onNext(bb);
                totalRead += numRead;
            }
            LOG.debug("Completed reading " + totalRead + " bytes of data from input stream.");

            // For empty files we need to emit an empty buffer
            if(totalRead == 0) {
                subscriber.onNext(ByteBuffer.allocate(0));
            }

            // Need to use this rather than subscriber.onComplete() to terminate the flux properly
            Operators.complete(subscriber);
        } catch (IOException e) {
            subscriber.onError(e);
        }

    }
}
