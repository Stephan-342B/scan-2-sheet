package com.mahefa.ocr.scan2sheet.services.job.pipeline;

import com.mahefa.ocr.scan2sheet.data.RowContext;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import com.mahefa.ocr.scan2sheet.data.ColumnContext;
import com.mahefa.ocr.scan2sheet.services.job.ProgressReporter;
import com.mahefa.ocr.scan2sheet.services.job.extractor.ColumnExtractor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class RowProcessor implements ItemProcessor<RowContext, ColumnContext> {

    private final ObjectProvider<Tesseract> tesseractProvider;
    private final ProgressReporter progressReporter;

    // The rowExtractorStep runs multi-threaded and Tesseract is not thread-safe, so each worker
    // thread gets its own ColumnExtractor (backed by its own prototype Tesseract instance).
    private final ThreadLocal<ColumnExtractor> columnExtractor;

    public RowProcessor(ObjectProvider<Tesseract> tesseractProvider, ProgressReporter progressReporter) {
        this.tesseractProvider = tesseractProvider;
        this.progressReporter = progressReporter;
        this.columnExtractor = ThreadLocal.withInitial(() -> new ColumnExtractor(this.tesseractProvider.getObject()));
    }

    @Override
    public ColumnContext process(RowContext context) throws TesseractException {
        String[] dataRow = columnExtractor.get().toDataRow(
                context.getImage(),
                context.getRowIndex(),
                context.getColumns(),
                context.getColumnProperty()
        );

        progressReporter.incrementRow();

        return new ColumnContext.Builder(context.getDocumentId(), context.getSheetId())
                .data(dataRow)
                .build();
    }
}
