package com.mahefa.ocr.scan2sheet.services.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class JobRunner {

    private final JobLauncher jobLauncher;
    private final Job job;

    public JobRunner(JobLauncher jobLauncher, @Qualifier("jobBuilder") Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    public void runJob(String sourceDir) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("sourceDir", sourceDir)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(job, params);
    }
}

