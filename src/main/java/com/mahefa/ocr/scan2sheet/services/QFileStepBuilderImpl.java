package com.mahefa.ocr.scan2sheet.services;

import com.mahefa.ocr.scan2sheet.data.RowContext;
import com.mahefa.ocr.scan2sheet.services.job.listener.CustomStepExecutionListener;
import com.mahefa.ocr.scan2sheet.services.job.pipeline.RowProcessor;
import com.mahefa.ocr.scan2sheet.services.job.pipeline.RowReader;
import com.mahefa.ocr.scan2sheet.services.job.pipeline.RowWriter;
import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.data.ColumnContext;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Future;

@Service
public class QFileStepBuilderImpl implements QFileStepBuilder {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CustomStepExecutionListener customStepExecutionListener;
    private final RowReader rowReader;
    private final RowProcessor rowProcessor;
    private final RowWriter rowWriter;
    private final TaskExecutor taskExecutor;
    private final int chunkSize;

    public QFileStepBuilderImpl(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RowReader rowReader,
            RowProcessor rowProcessor,
            RowWriter rowWriter,
            @Qualifier("CustomStepExecutionListener") CustomStepExecutionListener customStepExecutionListener,
            @Qualifier("backgroundTaskExecutor") TaskExecutor taskExecutor,
            AppProperties appProperties
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.customStepExecutionListener = customStepExecutionListener;
        this.rowReader = rowReader;
        this.rowProcessor = rowProcessor;
        this.rowWriter = rowWriter;
        this.taskExecutor = taskExecutor;
        this.chunkSize = appProperties.getBatch().getChunkSize();
    }

    public Step createStep() {
        // The reader stays single-threaded (preserves row order). OCR (the expensive processor)
        // runs across the pool via AsyncItemProcessor; AsyncItemWriter unwraps the Futures in the
        // original read order before the delegate appends them, so output order is preserved.
        AsyncItemProcessor<RowContext, ColumnContext> asyncProcessor = new AsyncItemProcessor<>();
        asyncProcessor.setDelegate(rowProcessor);
        asyncProcessor.setTaskExecutor(taskExecutor);

        AsyncItemWriter<ColumnContext> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(rowWriter);

        return new StepBuilder("rowExtractorStep", jobRepository)
                .<RowContext, Future<ColumnContext>>chunk(chunkSize, transactionManager)
                .reader(rowReader)
                .processor(asyncProcessor)
                .writer(asyncWriter)
                .faultTolerant()
                .listener((ChunkListener) customStepExecutionListener)
                .listener((StepExecutionListener) customStepExecutionListener)
                .build();
    }
}
