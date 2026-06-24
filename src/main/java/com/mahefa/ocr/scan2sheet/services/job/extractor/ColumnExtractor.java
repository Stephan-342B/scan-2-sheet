package com.mahefa.ocr.scan2sheet.services.job.extractor;

import com.mahefa.ocr.scan2sheet.common.tools.ImageConverter;
import com.mahefa.ocr.scan2sheet.common.tools.ImageProcessing;
import com.mahefa.ocr.scan2sheet.data.property.AxisProperty;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.List;

public class ColumnExtractor extends ImageProcessing {

    private final Logger log = LoggerFactory.getLogger(ColumnExtractor.class);
    private final Tesseract tesseract;

    public ColumnExtractor(Tesseract tesseract) {
        this.tesseract = tesseract;
    }

    public String[] toDataRow(Mat currentRow, int rowIndex, List<Rect> columns, AxisProperty columnProperty) throws TesseractException {
        String[] data = new String[columnProperty.getTotalElement()];

        int idx = 0;
        int currentX = 0;
        int currentHeight = currentRow.height();
        int r = (rowIndex / currentHeight);

        for (int c = columnProperty.getStartIndex(); c < columnProperty.getEndIndex(); c++) {
            if (columnProperty.getRangeSelection().skip(c)) continue;

            Rect label = columns.get(c);

            if (c > 0) {
                // Get prior column intel
                final Rect priorLabel = columns.get(c - 1);
                currentX = (priorLabel.x + priorLabel.width + 1);
            }

            // ROI: Current cell
            int currentWidth = (label.x + label.width + 1) - currentX;
            Mat currentCol = getROI(currentRow, currentX, 0, currentWidth, currentHeight);

            // Process current ROI
            Mat processedSection = processChunkImage(currentCol, r, c);
            BufferedImage bufferedImage = ImageConverter.toBufferedImage(processedSection);

            // OCR
            String rawText = tesseract.doOCR(bufferedImage);
            data[idx++] = (rawText != null) ? rawText.trim() : "";

            // Debug output
//            Imgcodecs.imwrite("/home/stephan/Workspace/Personnal/Serious_Project/scan-2-sheet/folder/processed/row_" + rowIndex + "col_" + c + ".png", currentCol);
        }

        log.debug("Row {}: {}", r, String.join(" | ", data));

        return data;
    }
}

