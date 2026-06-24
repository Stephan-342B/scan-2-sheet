package com.mahefa.ocr.scan2sheet.services.job.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

@Component("customJobExecutionListener")
@JobScope
public class CustomJobExecutionListener implements JobExecutionListener {

    private final Logger log = LoggerFactory.getLogger(CustomJobExecutionListener.class);

    private JobExecution jobExecution;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        this.jobExecution = jobExecution;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
    }
}
