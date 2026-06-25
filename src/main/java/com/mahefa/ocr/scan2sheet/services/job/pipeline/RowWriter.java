package com.mahefa.ocr.scan2sheet.services.job.pipeline;

import com.mahefa.ocr.scan2sheet.common.WriterServiceBuilder;
import com.mahefa.ocr.scan2sheet.common.enumerator.WriterMode;
import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.data.ColumnContext;
import com.mahefa.ocr.scan2sheet.services.DocumentService;
import com.mahefa.ocr.scan2sheet.services.job.ProgressReporter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@StepScope
public class RowWriter implements ItemWriter<ColumnContext> {

    private final WriterServiceBuilder writerServiceBuilder;
    private final AppProperties appProperties;
    private final ProgressReporter progressReporter;

    public RowWriter(WriterServiceBuilder writerServiceBuilder, AppProperties appProperties,
                     ProgressReporter progressReporter) {
        this.writerServiceBuilder = writerServiceBuilder;
        this.appProperties = appProperties;
        this.progressReporter = progressReporter;
    }

    @Override
    public void write(Chunk<? extends ColumnContext> chunk) throws Exception {
        List<? extends ColumnContext> items = chunk.getItems();
        WriterMode writingMode = appProperties.getFile().getWriter().getWritingMode();
        DocumentService documentService = writerServiceBuilder.getWriter(writingMode);

        // Group items by documentId
        Map<String, List<ColumnContext>> groupedByDocument = items.stream()
                .collect(Collectors.groupingBy(
                        ColumnContext::getDocumentId,
                        TreeMap::new,
                        Collectors.toList()
                ));

        // Process each document
        Iterator<Map.Entry<String, List<ColumnContext>>> documentIterator = groupedByDocument.entrySet().iterator();

        while (documentIterator.hasNext()) {
            Map.Entry<String, List<ColumnContext>> documentEntry = documentIterator.next();
            String currentDocumentId = documentEntry.getKey();
            List<ColumnContext> data = documentEntry.getValue();

            int colLength = data.stream()
                    .map(ColumnContext::getLength)
                    .findFirst()
                    .orElse(0);

            documentService = documentService.forDocument(currentDocumentId);

            // Group items by sheetId
            Map<String, List<ColumnContext>> groupedBySheet = data.stream()
                    .collect(Collectors.groupingBy(
                            ColumnContext::getSheetId,
                            TreeMap::new,
                            Collectors.toList()
                    ));

            // Process each sheet
            Iterator<Map.Entry<String, List<ColumnContext>>> sheetIterator = groupedBySheet.entrySet().iterator();

            while (sheetIterator.hasNext()) {
                Map.Entry<String, List<ColumnContext>> entry = sheetIterator.next();
                String currentSheetId = entry.getKey();
                List<ColumnContext> rows = entry.getValue();

                // Export data
                documentService = documentService.onSheet(currentDocumentId, currentSheetId, colLength);
                documentService.writeAll(currentDocumentId, currentSheetId, rows.stream().map(ColumnContext::getData).toList());

                // Drive the live progress from the persisted rows, attributed to their own file.
                progressReporter.recordRows(currentDocumentId, currentSheetId, rows.size());
            }
        }
    }
}
