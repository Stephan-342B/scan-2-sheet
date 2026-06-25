package com.mahefa.ocr.scan2sheet.services.job.listener;

import com.mahefa.ocr.scan2sheet.common.event.DocumentChangedEvent;
import com.mahefa.ocr.scan2sheet.common.event.FileChangedEvent;
import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.data.DocumentSession;
import com.mahefa.ocr.scan2sheet.services.DocumentService;
import com.mahefa.ocr.scan2sheet.services.job.ProgressReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component("CustomStepExecutionListener")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class CustomStepExecutionListener implements StepExecutionListener, ChunkListener {

    private final Logger log = LoggerFactory.getLogger(CustomStepExecutionListener.class);

    @Autowired
    private CustomJobExecutionListener jobExecutionListener;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ProgressReporter progressReporter;

    private Queue<DocumentSession> disposableDocuments = new ConcurrentLinkedQueue<>();

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // Writer config is logged by the job listener; here we open the live progress block.
        System.out.println();    // blank line between the job header and the file lines
        progressReporter.start();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        progressReporter.stop();  // commits the last file's line
        System.out.println();     // blank line before the job summary
        log.debug("Step '{}' read={}, written={} ({})",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getStatus());
        return ExitStatus.COMPLETED;
    }

    @Override
    public void beforeChunk(ChunkContext context) {
    }

    @Override
    public void afterChunk(ChunkContext context) {
        DocumentService documentService = jobExecutionListener.getDocumentService();

        while (!disposableDocuments.isEmpty()) {
            DocumentSession session = disposableDocuments.poll();
            String documentId = session.getDocumentId();
            String sheetId = session.getSheetId();

            documentService.resizeCol(documentId, sheetId);

            // All files inside the current document have been processed. we can safely close it
            if (session.getCurrentCount() == session.getTotalFileCount()) {
                documentService.close(documentId);
            }
        }
    }

    @Override
    public void afterChunkError(ChunkContext context) {
    }

    @EventListener
    public void onFileChanged(FileChangedEvent event) {
        try {
            DocumentSession session = event.getDocumentSession();
            Path sourcePath = session.getPath();

            // Convert archive path string to Path
            Path archiveDir = Paths.get(appProperties.getDir().getArchive());
            Path sourceRoot = Paths.get(appProperties.getDir().getSource());

            // Compute relative path from source root
            Path relativePath = sourceRoot.relativize(sourcePath);

            // Resolve target path using relative structure
            Path targetPath = archiveDir.resolve(relativePath);

            // Ensure parent directory exists
            Files.createDirectories(targetPath.getParent());

            // Move the file
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            // Per-file detail kept at debug so it doesn't interrupt the in-place progress line.
            log.debug("Archived '{}' ({}/{}) -> {}",
                    session.getSheetId(), session.getCurrentCount(), session.getTotalFileCount(), targetPath);

            // Auto resize grid
            disposableDocuments.add(session);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    @EventListener
    public void onDocumentChanged(DocumentChangedEvent event) {
        final String documentId = event.getDocumentId();

//        disposableDocuments.add(new DocumentSession(documentId, null));
        log.debug("Document {} closed successfully", documentId);
    }
}
