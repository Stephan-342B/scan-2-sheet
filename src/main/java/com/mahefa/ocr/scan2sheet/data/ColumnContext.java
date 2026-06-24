package com.mahefa.ocr.scan2sheet.data;

public class ColumnContext {

    private final String documentId;
    private final String sheetId;
    private final String[] data;

    private ColumnContext(Builder builder) {
        this.documentId = builder.documentId;
        this.sheetId = builder.sheetId;
        this.data = builder.data;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String[] getData() {
        return data;
    }

    public int getLength() {
        return (data != null) ? data.length : 0;
    }

    public static class Builder {
        private final String documentId;
        private final String sheetId;
        private String[] data;

        public Builder(String documentId, String sheetId) {
            this.documentId = documentId;
            this.sheetId = sheetId;
        }

        public Builder data(String[] data) {
            this.data = data;
            return this;
        }

        public ColumnContext build() {
            return new ColumnContext(this);
        }
    }
}
