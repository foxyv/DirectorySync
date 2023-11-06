package ninja.foxyv.vsync.entities;

import java.util.concurrent.atomic.AtomicLong;

public class CopyCounters {
    public final AtomicLong fileCounter = new AtomicLong(0);
    public final AtomicLong byteCounter = new AtomicLong(0);
    public final AtomicLong filesHashed = new AtomicLong(0);
}
