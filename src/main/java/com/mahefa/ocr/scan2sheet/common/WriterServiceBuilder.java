package com.mahefa.ocr.scan2sheet.common;

import com.mahefa.ocr.scan2sheet.common.enumerator.WriterMode;
import com.mahefa.ocr.scan2sheet.services.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WriterServiceBuilder {

    private final Map<String, DocumentService> writers;

    @Autowired
    public WriterServiceBuilder(Map<String, DocumentService> writers) {
        this.writers = writers;
    }

    public DocumentService getWriter(WriterMode mode) {
        DocumentService writer = writers.get(mode.name());

        if (writer == null) {
            throw new IllegalArgumentException("Unsupported writer mode: " + mode);
        }

        return writer;
    }
}
