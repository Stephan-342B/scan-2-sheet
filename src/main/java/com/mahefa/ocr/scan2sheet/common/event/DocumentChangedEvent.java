package com.mahefa.ocr.scan2sheet.common.event;

public class DocumentChangedEvent {

    private final String documentId;

    public DocumentChangedEvent(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }
}
