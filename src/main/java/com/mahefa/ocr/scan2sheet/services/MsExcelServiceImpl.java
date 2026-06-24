package com.mahefa.ocr.scan2sheet.services;

import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.services.data.MsExcel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("XLS")
public class MsExcelServiceImpl implements DocumentService {

    @Autowired
    private AppProperties appProperties;

    private Map<String, MsExcel> msExcelMap = new ConcurrentHashMap<>(0);

    @Override
    public DocumentService forDocument(String documentId) throws FileNotFoundException {
        MsExcel currentMsExcel = msExcelMap.computeIfAbsent(documentId, s -> new MsExcel());

        synchronized (currentMsExcel) {
            Path outputPath = Path.of(appProperties.getDir().getOutput());
            Path filepath = Path.of(documentId + ".xlsx");

            currentMsExcel.setOutputStream(new FileOutputStream(outputPath.resolve(filepath).toFile()));
            return this;
        }
    }

    @Override
    public DocumentService onSheet(String documentId, String sheetId, int colLength) {
        MsExcel currentMsExcel = msExcelMap.getOrDefault(documentId, null);

        synchronized (currentMsExcel) {
            SXSSFWorkbook workbook = currentMsExcel.getWorkbook();
            SXSSFSheet currentSheet = workbook.getSheet(sheetId);

            if (currentSheet == null) {
                currentSheet = workbook.createSheet(sheetId);
                currentSheet.trackAllColumnsForAutoSizing();
            }

            currentMsExcel.setCurrentSheet(currentSheet);
            currentMsExcel.setColLength(colLength);
            return this;
        }
    }

    @Override
    public void writeAll(String documentId, String sheetId, List<String[]> rows) throws IOException {
        MsExcel currentMsExcel = msExcelMap.getOrDefault(documentId, null);

        if (currentMsExcel == null) {
            throw new IllegalStateException("MsExcelService not initialized. Call forDocument(String documentId) and " +
                    "onSheet(String documentId, String sheetId, int colLength) before writing.");
        }

        synchronized (currentMsExcel) {
            SXSSFSheet currentSheet = currentMsExcel.getCurrentSheet();

            rows.forEach(row -> {
                Row dataRow = currentSheet.createRow(currentSheet.getPhysicalNumberOfRows());

                for (int col = 0; col < row.length; col++) {
                    Cell cell = dataRow.createCell(col);
                    Object value = row[col];

                    // Format cell based on type
                    if (value instanceof ZonedDateTime) {
                        cell.setCellValue(Date.from(((ZonedDateTime) value).toInstant()));
                    } else if (value instanceof LocalDateTime) {
                        cell.setCellValue(Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant()));
                    } else if (value instanceof LocalDate) {
                        cell.setCellValue(Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    } else if (value instanceof Instant) {
                        cell.setCellValue(Date.from((Instant) value));
                    } else if (value instanceof Date) {
                        cell.setCellValue((Date) value);
                    } else if (value instanceof Double) {
                        cell.setCellValue((Double) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue((Integer) value);
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                }
            });

            currentSheet.flushRows();
        }
    }

    @Override
    public void resizeCol(String documentId, String sheetId) {
        MsExcel currentMsExcel = msExcelMap.getOrDefault(documentId, null);

        if (currentMsExcel != null) {
            synchronized (currentMsExcel) {
                SXSSFWorkbook workbook = currentMsExcel.getWorkbook();
                SXSSFSheet sheet = workbook.getSheet(sheetId);

                if (sheet != null) {
                    // Auto-size columns
                    for (int col = 0; col < currentMsExcel.getColLength(); col++) {
                        sheet.autoSizeColumn(col);
                    }
                }
            }
        }
    }

    @Override
    public void close(String documentId) {
        MsExcel currentMsExcel = msExcelMap.getOrDefault(documentId, null);

        if (currentMsExcel != null) {
            synchronized (currentMsExcel) {
                SXSSFWorkbook workbook = currentMsExcel.getWorkbook();
                FileOutputStream outputStream = currentMsExcel.getOutputStream();

                try {
                    workbook.write(outputStream);
                    workbook.dispose();
                    outputStream.close();

                    msExcelMap.remove(documentId);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }
}
