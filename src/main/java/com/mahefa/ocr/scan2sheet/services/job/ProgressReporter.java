package com.mahefa.ocr.scan2sheet.services.job;

import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Renders a live, self-updating console line per file while the step runs:
 *
 *   ⠼ <folder> / <file>   (1/3)   412 rows · 3m10s
 *
 * When the reader advances to the next file (or the step ends) the current file is "committed"
 * as a permanent line so finished files stay visible as history:
 *
 *   ✓ <folder> / <file>   (1/3)   1000 rows · 8m58s
 *
 * A background daemon ticker animates the live line; the row counter is bumped by OCR worker
 * threads. All console writes are serialized on a lock so the ticker and file transitions never
 * interleave on the same line.
 */
@Component
public class ProgressReporter {

    private static final String[] FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final long TICK_MS = 250;
    private static final String CLEAR_TAIL = "          "; // overwrite leftovers when a line shrinks

    private final Object console = new Object();
    private final AtomicLong rows = new AtomicLong();        // current file
    private final AtomicLong totalRows = new AtomicLong();   // whole run

    private volatile String folder = "";
    private volatile String file = "";
    private volatile int fileNo;
    private volatile long fileTotal;
    private volatile long fileStartMillis;
    private volatile boolean fileActive = false;
    private int committedFiles;
    private int frame;
    private ScheduledExecutorService ticker;

    public synchronized void start() {
        if (ticker != null) {
            return;
        }
        committedFiles = 0;
        totalRows.set(0);
        ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "s2s-progress");
            t.setDaemon(true);
            return t;
        });
        ticker.scheduleAtFixedRate(this::redraw, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (ticker != null) {
            ticker.shutdownNow();
            ticker = null;
        }
        synchronized (console) {
            if (fileActive) {
                commitCurrent(); // finalize the last file
            }
        }
    }

    /** Called when the reader starts a new file. Commits the previous file, then resets. */
    public void setFile(String folder, String file, int current, long total) {
        synchronized (console) {
            if (fileActive) {
                commitCurrent();
            }
            this.folder = folder;
            this.file = file;
            this.fileNo = current;
            this.fileTotal = total;
            this.rows.set(0);
            this.fileStartMillis = System.currentTimeMillis();
            this.fileActive = true;
        }
    }

    /** Called once per processed row (OCR worker threads). */
    public void incrementRow() {
        rows.incrementAndGet();
        totalRows.incrementAndGet();
    }

    public int getCommittedFiles() {
        return committedFiles;
    }

    public long getTotalRows() {
        return totalRows.get();
    }

    public static String human(long seconds) {
        return seconds < 60 ? seconds + "s" : String.format("%dm%02ds", seconds / 60, seconds % 60);
    }

    private void redraw() {
        synchronized (console) {
            if (!fileActive) {
                return;
            }
            System.out.print("\r" + line(FRAMES[Math.abs(frame++) % FRAMES.length]) + CLEAR_TAIL);
            System.out.flush();
        }
    }

    // Caller must hold the console lock.
    private void commitCurrent() {
        System.out.print("\r" + line("✓") + CLEAR_TAIL + System.lineSeparator());
        System.out.flush();
        fileActive = false;
        committedFiles++;
    }

    private String line(String marker) {
        long secs = (System.currentTimeMillis() - fileStartMillis) / 1000;
        return String.format("  %s %s / %s   (%d/%d)   %d rows · %s",
                marker, folder, file, fileNo, fileTotal, rows.get(), human(secs));
    }
}
