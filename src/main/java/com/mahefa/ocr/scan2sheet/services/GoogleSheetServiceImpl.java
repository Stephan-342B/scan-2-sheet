package com.mahefa.ocr.scan2sheet.services;

import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.mahefa.ocr.scan2sheet.services.api.GoogleSheetAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service("GOOGLE_SHEET")
public class GoogleSheetServiceImpl implements DocumentService {

    private final Logger log = LoggerFactory.getLogger(GoogleSheetServiceImpl.class);

    @Autowired
    private GoogleSheetAPI googleSheetAPI;

    private Map<String, Spreadsheet> spreadsheetMap = new ConcurrentHashMap<>(0);
    private int colLength;

    @Override
    public DocumentService forDocument(String documentId) {
        spreadsheetMap.computeIfAbsent("default", s -> googleSheetAPI.getDefaultSpreadsheet());
        return this;
    }

    @Override
    public DocumentService onSheet(String documentId, String sheetId, int colLength) throws Exception {
        Spreadsheet spreadsheet = spreadsheetMap.getOrDefault("default", null);

        if (spreadsheet != null) {
            synchronized (spreadsheet) {
                Sheet sheet = googleSheetAPI.getSheet(spreadsheet, sheetId);

                if (sheet == null) {
                    googleSheetAPI.newSheet(spreadsheet, sheetId);
                    spreadsheet = googleSheetAPI.getSpreadsheet(spreadsheet.getSpreadsheetId());
                    spreadsheetMap.put(documentId, spreadsheet);
                }

                this.colLength = colLength;
            }
        }

        return this;
    }

    @Override
    public void writeAll(String documentId, String sheetId, List<String[]> rows) throws IOException {
        Spreadsheet spreadsheet = spreadsheetMap.getOrDefault("default", null);

        if (spreadsheet == null) {
            throw new IllegalStateException("GoogleSheetService not initialized. Call forDocument(String documentId) and " +
                    "onSheet(String documentId, String sheetId, int colLength) before writing.");
        }

        synchronized (spreadsheet) {
            Sheet sheet = googleSheetAPI.getSheet(spreadsheet, sheetId);
            List<List<Object>> dataRows = rows.stream()
                    .map(Arrays::asList)
                    .map(list -> new ArrayList<Object>(list))
                    .collect(Collectors.toList());

            googleSheetAPI.writeAll(spreadsheet, sheet, dataRows);
        }
    }

    @Override
    public void resizeCol(String documentId, String sheetId) {
        Spreadsheet spreadsheet = spreadsheetMap.getOrDefault("default", null);

        if (spreadsheet != null) {
            synchronized (spreadsheet) {
                try {
                    Sheet sheet = googleSheetAPI.getSheet(spreadsheet, sheetId);

                    if (sheet != null) {
                        googleSheetAPI.autoResizeGrid(spreadsheet, sheet, colLength);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close(String documentId) {
        spreadsheetMap.remove(documentId);
        colLength = 0;
    }
}
