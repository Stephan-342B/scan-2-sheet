package com.mahefa.ocr.scan2sheet.services.job.listener;

import com.mahefa.ocr.scan2sheet.common.event.DocumentChangedEvent;
import com.mahefa.ocr.scan2sheet.common.WriterServiceBuilder;
import com.mahefa.ocr.scan2sheet.common.enumerator.WriterMode;
import com.mahefa.ocr.scan2sheet.common.event.FileChangedEvent;
import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.data.DocumentSession;
import com.mahefa.ocr.scan2sheet.services.DocumentService;
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
    private WriterServiceBuilder writerServiceBuilder;

    private Queue<DocumentSession> disposableDocuments = new ConcurrentLinkedQueue<>();

    private DocumentService documentService;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        WriterMode writingMode = appProperties.getFile().getWriter().getWritingMode();
        documentService = writerServiceBuilder.getWriter(writingMode);

        log.info("Starting '{}': writer={}, chunkSize={}, workers={}",
                stepExecution.getStepName(),
                writingMode,
                appProperties.getBatch().getChunkSize(),
                appProperties.getBackgroundTask().getCorePoolSize());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Finished '{}': rows read={}, written={}, status={}",
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
            log.info("Processed file '{}' ({}/{}) — archived to {}",
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