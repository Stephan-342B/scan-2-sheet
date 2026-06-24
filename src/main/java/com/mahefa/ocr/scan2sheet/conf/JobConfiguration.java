package com.mahefa.ocr.scan2sheet.conf;

import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.data.RowContext;
import com.mahefa.ocr.scan2sheet.data.TableStructure;
import com.mahefa.ocr.scan2sheet.services.QFileStepBuilder;
import com.mahefa.ocr.scan2sheet.services.job.delegate.CustomMultiResourceItemReader;
import com.mahefa.ocr.scan2sheet.services.job.delegate.DocumentDelegate;
import org.apache.commons.io.FileUtils;
import com.mahefa.ocr.scan2sheet.common.event.FileChangedEvent;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Configuration
public class JobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionListener customJobExecutionListener;
    private final AppProperties appProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final com.mahefa.ocr.scan2sheet.services.QFileStepBuilder QFileStepBuilder;
    private final TableStructure tableStructure;

    public JobConfiguration(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("customJobExecutionListener") JobExecutionListener customJobExecutionListener,
            AppProperties appProperties,
            ApplicationEventPublisher eventPublisher,
            QFileStepBuilder QFileStepBuilder,
            TableStructure tableStructure
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.customJobExecutionListener = customJobExecutionListener;
        this.appProperties = appProperties;
        this.eventPublisher = eventPublisher;
        this.QFileStepBuilder = QFileStepBuilder;
        this.tableStructure = tableStructure;
    }

    @Bean
    public Job jobBuilder() {
        return new JobBuilder("Q-FileJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(customJobExecutionListener)
                .start(QFileStepBuilder.createStep())
                .next(cleanupStep())
                .build();
    }

    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    File sourceDir = new File(appProperties.getDir().getSource());
                    File[] contents = sourceDir.listFiles();

                    if (contents != null) {
                        for (File file : contents) {
                            FileUtils.forceDelete(file);
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public CustomMultiResourceItemReader<RowContext> fileReader(AppProperties appProperties) throws IOException {
        CustomMultiResourceItemReader<RowContext> reader = new CustomMultiResourceItemReader<>();
        Resource[] resources = ResourcePatternUtils
                .getResourcePatternResolver(new DefaultResourceLoader())
                .getResources("file:" + appProperties.getDir().getSource() + "/**");

        List<Resource> imageFiles = Arrays.stream(resources)
                .filter(Resource::isReadable)
                .filter(Resource::isFile)
                .filter(r -> r.getFilename().matches(".*\\.(png|jpg|jpeg|gif)$"))
                .sorted(Comparator.comparing(Resource::getFilename))
                .toList();

        reader.setResources(imageFiles.toArray(new Resource[0]));
        reader.setDelegate(new DocumentDelegate(
                tableStructure,
                Path.of(appProperties.getDir().getSource()),
                documentSession -> eventPublisher.publishEvent(new FileChangedEvent(documentSession))
        ));

        return reader;
    }
}
