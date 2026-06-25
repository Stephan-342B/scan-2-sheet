package com.mahefa.ocr.scan2sheet.services.job.listener;

import com.mahefa.ocr.scan2sheet.common.WriterServiceBuilder;
import com.mahefa.ocr.scan2sheet.common.enumerator.WriterMode;
import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.services.DocumentService;
import com.mahefa.ocr.scan2sheet.services.job.ProgressReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

@Component("customJobExecutionListener")
@JobScope
public class CustomJobExecutionListener implements JobExecutionListener {

    private final Logger log = LoggerFactory.getLogger(CustomJobExecutionListener.class);

    private final AppProperties appProperties;
    private final WriterServiceBuilder writerServiceBuilder;
    private final ProgressReporter progressReporter;

    // The output writer is a job-level concern (one target per run): initialized here and shared
    // with the step listener via getDocumentService().
    private DocumentService documentService;
    private long startedAt;

    public CustomJobExecutionListener(AppProperties appProperties,
                                      WriterServiceBuilder writerServiceBuilder,
                                      ProgressReporter progressReporter) {
        this.appProperties = appProperties;
        this.writerServiceBuilder = writerServiceBuilder;
        this.progressReporter = progressReporter;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        WriterMode writingMode = appProperties.getFile().getWriter().getWritingMode();
        this.documentService = writerServiceBuilder.getWriter(writingMode);
        this.startedAt = System.currentTimeMillis();

        AppProperties.Table table = appProperties.getTable();
        log.info("▶ scan-2-sheet  →  {}", destination(writingMode));
        log.info("  rows {} · cols {} · chunk {} · {} workers",
                selectLabel(table.getRow().getSelect()),
                selectLabel(table.getCol().getSelect()),
                appProperties.getBatch().getChunkSize(),
                appProperties.getBackgroundTask().getCorePoolSize());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long secs = (System.currentTimeMillis() - startedAt) / 1000;
        boolean ok = jobExecution.getStatus() == BatchStatus.COMPLETED;
        log.info("{} {} · {} files · {} rows · {}",
                ok ? "✓" : "✗",
                ok ? "Done" : ("Job " + jobExecution.getStatus()),
                progressReporter.getCommittedFiles(),
                progressReporter.getTotalRows(),
                ProgressReporter.human(secs));
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    // Mirror RangeSelection's "no filter" semantics for the banner: an unset/NONE select means
    // every row/column on that axis is extracted, so show it as ALL rather than the literal "NONE".
    private static String selectLabel(String select) {
        return (select == null || select.isBlank() || select.trim().equalsIgnoreCase("NONE"))
                ? "ALL"
                : select;
    }

    private static String destination(WriterMode mode) {
        return switch (mode) {
            case GOOGLE_SHEET -> "Google Sheet";
            case XLS -> "Excel";
            case CSV -> "CSV";
        };
    }
}
