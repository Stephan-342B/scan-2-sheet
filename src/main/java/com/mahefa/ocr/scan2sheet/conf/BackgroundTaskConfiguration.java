package com.mahefa.ocr.scan2sheet.conf;

import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.conf.decorator.BackgroundTaskDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class BackgroundTaskConfiguration {

    private final Logger log = LoggerFactory.getLogger(BackgroundTaskConfiguration.class);

    private final AppProperties appProperties;

    public BackgroundTaskConfiguration(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean(name = "backgroundTaskExecutor")
    public ThreadPoolTaskExecutor backgroundTaskExecutor() {
        AppProperties.BackgroundTask props = appProperties.getBackgroundTask();

        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(props.getCorePoolSize());
        // Cap max = core so AsyncItemProcessor can't spawn one thread (and one ~31MB Tesseract
        // instance) per item in a chunk. CallerRunsPolicy applies back-pressure when the pool +
        // queue are saturated instead of rejecting or growing unbounded.
        threadPoolTaskExecutor.setMaxPoolSize(props.getCorePoolSize());
        threadPoolTaskExecutor.setQueueCapacity(props.getQueueCapacity());
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskExecutor.setKeepAliveSeconds(10);
        threadPoolTaskExecutor.setAllowCoreThreadTimeOut(true);
        threadPoolTaskExecutor.setTaskDecorator(new BackgroundTaskDecorator());
        threadPoolTaskExecutor.setThreadNamePrefix(props.getThreadNamePrefix());
        threadPoolTaskExecutor.initialize();

        return threadPoolTaskExecutor;
    }
}