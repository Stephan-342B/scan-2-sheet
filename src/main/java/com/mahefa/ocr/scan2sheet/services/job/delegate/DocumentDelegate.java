package com.mahefa.ocr.scan2sheet.services.job.delegate;

import com.mahefa.ocr.scan2sheet.data.DocumentSession;
import com.mahefa.ocr.scan2sheet.data.RowContext;
import org.apache.commons.io.FilenameUtils;
import com.mahefa.ocr.scan2sheet.common.tools.ImageConverter;
import com.mahefa.ocr.scan2sheet.data.TableStructure;
import com.mahefa.ocr.scan2sheet.data.property.AxisProperty;
import com.mahefa.ocr.scan2sheet.services.job.ProgressReporter;
import com.mahefa.ocr.scan2sheet.services.job.extractor.RowExtractor;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class DocumentDelegate implements ResourceAwareItemReaderItemStream<RowContext> {

    private final Logger log = LoggerFactory.getLogger(DocumentDelegate.class);
    private final LocalDateTime currentDateTime = LocalDateTime.now();

    private File currentFile;
    private Path sourceRoot;
    private String currentDocumentId;
    private String currentSheetId;
    private int currentCount;
    private long totalFileCount;

    private RowExtractor currentRowExtractor;
    private AxisProperty rowProperty;
    private AxisProperty columnProperty;
    private int maxRowCount;

    private final ProgressReporter progressReporter;
    private Consumer<DocumentSession> onFileChangedListener;

    public DocumentDelegate(TableStructure tableStructure, Path sourceRoot,
                            ProgressReporter progressReporter,
                            Consumer<DocumentSession> onFileChangedListener) {
        this.rowProperty = tableStructure.getRowProperty();
        this.columnProperty = tableStructure.getColumnProperty();
        this.maxRowCount = tableStructure.getMaxRowCount();
        this.sourceRoot = sourceRoot;
        this.progressReporter = progressReporter;

        // Listeners
        this.onFileChangedListener = onFileChangedListener;
    }

    @Override
    public void setResource(Resource resource) {
        try {
            this.currentFile = resource.getFile();

            String filepath = currentFile.getAbsolutePath();
            String ext = FilenameUtils.getExtension(filepath).toLowerCase();

            Mat mat = ext.equals("gif")
                    ? ImageConverter.toOpenCVMat(ImageConverter.toBufferedImage(currentFile))
                    : Imgcodecs.imread(currentFile.getAbsolutePath());

            Path sourcePath = Paths.get(filepath);
            Path relativePath = sourceRoot.relativize(sourcePath);
            Path parent = relativePath.getParent();
            currentSheetId = FilenameUtils.getBaseName(filepath);
            String documentId = parent != null
                    ? parent.getFileName().toString()
                    : "scan-2-sheet_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSS").format(currentDateTime);

            if (!documentId.equals(currentDocumentId)) {
                Path folderPath = (parent != null) ? sourceRoot.resolve(parent) : sourceRoot;
                totalFileCount = Files.list(folderPath)
                        .filter(Files::isRegularFile)
                        .count();

                currentDocumentId = documentId;
                currentCount = 1;

                log.debug("Processing current folder {}", currentDocumentId);
            } else {
                currentCount++; // Increment file counter
            }

            currentRowExtractor = new RowExtractor(mat, rowProperty, columnProperty, maxRowCount);
            currentRowExtractor.setDocumentId(currentDocumentId);
            currentRowExtractor.setSheetId(currentSheetId);

            // Hand the current file to the live progress line (rendered by ProgressReporter).
            progressReporter.setFile(currentDocumentId, currentSheetId, currentCount, totalFileCount);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to convert Resource to File: " + resource.getFilename(), e);
        }
    }

    @Override
    public RowContext read() {
        if (currentRowExtractor == null || !currentRowExtractor.hasNext()) return null;
        return currentRowExtractor.nextItem();
    }

    @Override
    public void open(ExecutionContext executionContext) {
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // No-op
    }

    @Override
    public void close() {
        // Spring may invoke close() again after the last resource is exhausted (currentFile
        // nulled below). Skip the no-op destroy call so the listener isn't handed a null file.
        if (currentFile == null) {
            return;
        }

        DocumentSession documentSession = new DocumentSession(currentFile, currentDocumentId, currentSheetId);
        documentSession.setCurrentCount(currentCount);
        documentSession.setTotalFileCount(totalFileCount);

        onFileChangedListener.accept(documentSession);

        currentFile = null;
        currentSheetId = null;
    }
}
