package com.mahefa.ocr.scan2sheet.conf;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Batch infrastructure for a database-less job (Spring Batch 5).
 *
 * Spring Batch 5 removed the in-memory Map-based JobRepository that Boot 2.x fell back to when
 * no DataSource was present. Since this app deliberately runs without a database
 * (DataSourceAutoConfiguration is excluded), we provide the lightweight non-persistent
 * infrastructure explicitly: a ResourcelessJobRepository + ResourcelessTransactionManager.
 *
 * The JobLauncher runs jobs synchronously on the calling thread; parallelism lives INSIDE the
 * step (see QFileStepBuilderImpl#createStep), not at the job level.
 */
@Configuration
public class BatchInfrastructureConfig {

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new ResourcelessTransactionManager();
    }

    @Bean
    public JobRepository jobRepository() {
        return new ResourcelessJobRepository();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
