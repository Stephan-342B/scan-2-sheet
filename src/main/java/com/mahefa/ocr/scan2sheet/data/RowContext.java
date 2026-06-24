package com.mahefa.ocr.scan2sheet.data;

import com.mahefa.ocr.scan2sheet.data.property.AxisProperty;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.List;

public class RowContext {

    private final Mat image;
    private final int rowIndex;
    private final String documentId;
    private final String sheetId;
    private final AxisProperty rowProperty;
    private final AxisProperty columnProperty;
    private final List<Rect> columns;

    private RowContext(Builder builder) {
        this.image = builder.image;
        this.rowIndex = builder.rowIndex;
        this.documentId = builder.documentId;
        this.sheetId = builder.sheetId;
        this.rowProperty = builder.rowProperty;
        this.columnProperty = builder.columnProperty;
        this.columns = builder.columns;
    }

    public Mat getImage() {
        return image;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public AxisProperty getRowProperty() {
        return rowProperty;
    }

    public AxisProperty getColumnProperty() {
        return columnProperty;
    }

    public List<Rect> getColumns() {
        return columns;
    }

    public static class Builder {
        private final Mat image;
        private final int rowIndex;
        private String documentId;
        private String sheetId;
        private AxisProperty rowProperty;
        private AxisProperty columnProperty;
        private List<Rect> columns;

        public Builder(Mat image, int rowIndex) {
            this.image = image;
            this.rowIndex = rowIndex;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder sheetId(String sheetId) {
            this.sheetId = sheetId;
            return this;
        }

        public Builder rowProperty(AxisProperty rowProperty) {
            this.rowProperty = rowProperty;
            return this;
        }

        public Builder columnProperty(AxisProperty columnProperty) {
            this.columnProperty = columnProperty;
            return this;
        }

        public Builder columns(List<Rect> columns) {
            this.columns = columns;
            return this;
        }

        public RowContext build() {
            return new RowContext(this);
        }
    }
}

