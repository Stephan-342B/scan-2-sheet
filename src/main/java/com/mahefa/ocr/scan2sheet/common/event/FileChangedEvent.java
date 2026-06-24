package com.mahefa.ocr.scan2sheet.common.event;

import com.mahefa.ocr.scan2sheet.data.DocumentSession;

public class FileChangedEvent {

    private final DocumentSession documentSession;

    public FileChangedEvent(DocumentSession documentSession) {
        this.documentSession = documentSession;
    }

    public DocumentSession getDocumentSession() {
        return documentSession;
    }
}
