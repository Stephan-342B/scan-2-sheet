package com.mahefa.ocr.scan2sheet.services.job;

import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Renders live console progress as a tree, grouping files under their folder so the folder name is
 * written once instead of being repeated on every line:
 *
 *   &lt;folder&gt;
 *   ├─ ✓ &lt;file&gt;   6 rows · 14s
 *   ├─ ⠼ &lt;file&gt;   4 rows · 6s        ← live line for the file being processed
 *   └─ 3 files · 18 rows · 38s          ← folder recap (omitted for single-file folders)
 *
 * The current file's line animates in place (spinner + growing row count); when the reader advances
 * to the next file it is "committed" as a permanent ├─ line. When the folder changes (or the run
 * ends) the previous folder is closed with a └─ recap.
 *
 * A background daemon ticker animates the live line; the row counter is bumped by OCR worker
 * threads. All console writes are serialized on a lock so the ticker, file transitions and folder
 * boundaries never interleave on the same line.
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
    private volatile long fileStartMillis;
    private volatile boolean fileActive = false;

    // Current folder roll-up, finalized into the └─ recap when the folder closes.
    private boolean folderActive = false;
    private long folderStartMillis;
    private long folderRows;
    private int folderFiles;

    private int committedFiles;
    private int frame;
    private ScheduledExecutorService ticker;

    public synchronized void start() {
        if (ticker != null) {
            return;
        }
        committedFiles = 0;
        totalRows.set(0);
        folder = "";
        folderActive = false;
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
            if (folderActive) {
                finishFolder(); // close the last folder's subtree
            }
        }
    }

    /** Called when the reader starts a new file. Commits the previous file, opens a new folder header
     *  when the folder changes, then resets the current-file counters. */
    public void setFile(String folder, String file, int current, long total) {
        synchronized (console) {
            if (fileActive) {
                commitCurrent();
            }
            if (!folderActive || !folder.equals(this.folder)) {
                if (folderActive) {
                    finishFolder();
                }
                beginFolder(folder);
            }
            this.file = file;
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
            System.out.print("\r" + fileLine(FRAMES[Math.abs(frame++) % FRAMES.length]) + CLEAR_TAIL);
            System.out.flush();
        }
    }

    // Caller must hold the console lock. Opens a new folder header and resets the roll-up.
    private void beginFolder(String folder) {
        this.folder = folder;
        this.folderRows = 0;
        this.folderFiles = 0;
        this.folderStartMillis = System.currentTimeMillis();
        this.folderActive = true;
        System.out.print(System.lineSeparator() + "  " + folder + System.lineSeparator());
        System.out.flush();
    }

    // Caller must hold the console lock. Closes the current folder with a recap (skipped when the
    // folder held a single file, since the recap would just repeat that file's line).
    private void finishFolder() {
        if (folderFiles > 1) {
            long secs = (System.currentTimeMillis() - folderStartMillis) / 1000;
            System.out.printf("  └─ %d files · %d rows · %s%n", folderFiles, folderRows, human(secs));
            System.out.flush();
        }
        folderActive = false;
    }

    // Caller must hold the console lock. Finalizes the live file line and rolls it into the folder.
    private void commitCurrent() {
        System.out.print("\r" + fileLine("✓") + CLEAR_TAIL + System.lineSeparator());
        System.out.flush();
        fileActive = false;
        committedFiles++;
        folderFiles++;
        folderRows += rows.get();
    }

    private String fileLine(String marker) {
        long secs = (System.currentTimeMillis() - fileStartMillis) / 1000;
        return String.format("  ├─ %s %s   %d rows · %s", marker, file, rows.get(), human(secs));
    }
}
