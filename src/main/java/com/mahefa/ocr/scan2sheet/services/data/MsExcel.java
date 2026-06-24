package com.mahefa.ocr.scan2sheet.services.data;

import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;

public class MsExcel {

    private SXSSFWorkbook workbook;
    private SXSSFSheet currentSheet;
    private FileOutputStream outputStream;
    private int colLength;

    public MsExcel() {
        this.workbook = new SXSSFWorkbook();
    }

    public SXSSFWorkbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(SXSSFWorkbook workbook) {
        this.workbook = workbook;
    }

    public SXSSFSheet getCurrentSheet() {
        return currentSheet;
    }

    public void setCurrentSheet(SXSSFSheet currentSheet) {
        this.currentSheet = currentSheet;
    }

    public FileOutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(FileOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public int getColLength() {
        return colLength;
    }

    public void setColLength(int colLength) {
        this.colLength = colLength;
    }
}
