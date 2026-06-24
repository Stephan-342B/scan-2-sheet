package com.mahefa.ocr.scan2sheet.services;

import java.io.IOException;
import java.util.List;

/**
 * Service interface for managing document operations such as opening, writing, and closing.
 */
public interface DocumentService {

    DocumentService forDocument(String documentId) throws Exception;

    DocumentService onSheet(String documentId, String sheetId, int colLength) throws Exception;

    void writeAll(String documentId, String sheetId, List<String[]> rows) throws IOException;

    void resizeCol(String documentId, String sheetId);

    void close(String documentId);
}


