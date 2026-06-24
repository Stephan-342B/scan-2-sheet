package com.mahefa.ocr.scan2sheet.data;

public class DocumentKey {

    private final String documentId;
    private final String sheetId;

    public DocumentKey(String documentId, String sheetId) {
        this.documentId = documentId;
        this.sheetId = sheetId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSheetId() {
        return sheetId;
    }
}
