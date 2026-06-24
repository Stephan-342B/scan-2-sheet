package com.mahefa.ocr.scan2sheet.services.job.extractor;

import com.mahefa.ocr.scan2sheet.common.tools.ImageProcessing;
import com.mahefa.ocr.scan2sheet.data.RowContext;
import com.mahefa.ocr.scan2sheet.data.property.AxisProperty;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.List;

public class RowExtractor extends ImageProcessing {

    private String documentId;
    private String sheetId;

    private final Mat originalImage;
    private final List<Rect> columns;
    private final int totalImgHeight;
    private final int height;

    private final AxisProperty rowProperty;
    private final AxisProperty columnProperty;

    private int r;

    public RowExtractor(Mat originalImage, AxisProperty rowProperty, AxisProperty columnProperty, int maxRowCount) {
        this.originalImage = originalImage;
        this.rowProperty = rowProperty;
        this.columnProperty = columnProperty;

        this.columns = processHeader(originalImage);
        this.totalImgHeight = originalImage.height();
        this.height = totalImgHeight / maxRowCount;

        if (rowProperty.getEndIndex() == null)
            rowProperty.setEndIndex(maxRowCount + 1);

        if (columnProperty.getEndIndex() == null)
            columnProperty.setEndIndex(columns.size());

        if (columnProperty.getTotalElement() == 0)
            columnProperty.setTotalElement(columns.size());

        this.r = (rowProperty.isHeaderPresent()) ? 0 : rowProperty.getStartIndex();
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public int getCurrentRowIndex() {
        return r;
    }

    public boolean hasNext() {
        return r < rowProperty.getEndIndex();
    }

    public RowContext nextItem() {
        RowContext rowContext = null;

        if (r == 0 || rowProperty.getRangeSelection().isSelected(r)) {
            int currentY = r * height;
            int endY = Math.min((currentY == 0) ? 22 : currentY + height, totalImgHeight);
            int currentHeight = endY - currentY;

            Mat currentRow = getROI(originalImage, 0, currentY, originalImage.width(), currentHeight);
//            Imgcodecs.imwrite("/home/stephan/Workspace/Personnal/scan-2-sheet/folder/processed/row_" + r + ".png", currentRow);

            rowContext = new RowContext.Builder(currentRow, currentY)
                    .documentId(documentId)
                    .sheetId(sheetId)
                    .rowProperty(rowProperty)
                    .columnProperty(columnProperty)
                    .columns(columns)
                    .build();
        }

        r++;

        /**
         * Returns the next selected RowContext based on row selection criteria.
         * Skips unselected rows without marking the file as fully processed.
         * This ensures that only specified rows (e.g., 1-3,7,1000) are extracted,
         * allowing partial extraction without premature lifecycle completion.
         *
         * If the current row is skipped, recursively fetch the next valid one.
         */

        return (rowContext == null && hasNext()) ? nextItem() : rowContext;
    }
}

