package ninja.foxyv.vsync;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReactiveDirectorySynchronizer {


    public static void main(String[] args) {
        Scheduler fileScheduler = Schedulers.newParallel("FileCountScheduler", 10);
        try {
            File directory = new File("V:\\DCS_Liveries");
            Long aLong = countFiles(directory, true).subscribeOn(fileScheduler).block();
            System.out.println("Total file(s) in " + directory.getAbsolutePath() + ": " + aLong);
        } finally {
            fileScheduler.disposeGracefully().block();
        }
    }

    public static Mono<Long> countFiles(File directory, boolean recursive) {
            return filesInDirectory(directory, recursive)
                    // Turn this into a flux of grouped fluxes that we can do stuff with
                    .groups()
                    // Get a count from each group
                    .flatMap(gf -> gf.collect(Collectors.counting()))
                    // Sum up all the counts
                    .collect(Collectors.summingLong(l -> l));
    }


    public static ParallelFlux<File> filesInDirectory(File directory, boolean recursive) {
        if (directory.isDirectory()) {
            if (!recursive) {
                // Emit all the files from the directory
                return Mono.just(directory)
                        .flatMapMany(
                                dir -> Optional.ofNullable(dir.listFiles())
                                        .map(Flux::fromArray)
                                        .orElse(Flux.empty())
                        )
                        .filter(File::isFile).parallel();
            } else {
                // Emit all the files and directories from the directory then recursively apply it
                return Mono.just(directory)
                        .flatMapMany(
                                dir -> Optional.ofNullable(dir.listFiles())
                                        .map(Flux::fromArray)
                                        .orElse(Flux.empty())
                        )
                        .flatMap(file -> filesInDirectory(file, true))
                        .parallel()
                        .filter(File::isFile);
            }
        } else {
            // If a file is passed in then just return it.
            return Flux.just(directory).parallel();
        }

    }
}
