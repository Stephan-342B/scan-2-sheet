package com.mahefa.ocr.scan2sheet.data;

import java.io.File;

public class FileInfo {

    private final File file;
    private final String documentId;
    private final String sheetId;

    public FileInfo(File file, String documentId, String sheetId) {
        this.file = file;
        this.documentId = documentId;
        this.sheetId = sheetId;
    }

    public File getFile() {
        return file;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSheetId() {
        return sheetId;
    }
}
