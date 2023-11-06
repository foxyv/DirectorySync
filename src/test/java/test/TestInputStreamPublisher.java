package test;

import ninja.foxyv.vsync.react.fs.InputStreamFlux;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static test.utils.TmpDirectoryUtils.prepareTMPDirectory;

public class TestInputStreamPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(TestInputStreamPublisher.class);

    @Test
    public void test() throws IOException {
        File tmp = new File("tmp");
        prepareTMPDirectory(tmp);

        File mobyDick = new File(tmp, "files/MobyDick.txt");

        String mobyDickText;
        try (InputStream is = new FileInputStream(mobyDick)) {
            mobyDickText = String.join("", InputStreamFlux.flux(is)
                    .map(buffer -> new String(buffer.array(), buffer.arrayOffset(), buffer.remaining()))
                    .doOnComplete(() -> LOG.info("Flux completed."))
                    .doOnTerminate(() -> LOG.info("Flux terminated."))
                    .collectList()
                    .doOnTerminate(() -> LOG.info("Mono terminated."))
                    .blockOptional()
                    .orElseThrow());
        }



        String expected;
        try (InputStream is = new FileInputStream(mobyDick); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            is.transferTo(baos);
            expected = baos.toString();
        }

        Assertions.assertEquals(expected, mobyDickText);
    }
}
