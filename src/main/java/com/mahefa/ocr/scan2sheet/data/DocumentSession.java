package com.mahefa.ocr.scan2sheet.data;

import java.io.File;
import java.nio.file.Path;

public class DocumentSession {

    private final Path path;
    private final String documentId;
    private final String sheetId;
    private int currentCount;
    private long totalFileCount;

    public DocumentSession(File file, String documentId, String sheetId) {
        this.path = file.toPath();
        this.documentId = documentId;
        this.sheetId = sheetId;
    }

    public Path getPath() {
        return path;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    public long getTotalFileCount() {
        return totalFileCount;
    }

    public void setTotalFileCount(long totalFileCount) {
        this.totalFileCount = totalFileCount;
    }
}
