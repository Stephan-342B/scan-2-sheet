package com.mahefa.ocr.scan2sheet.conf.decorator;

import com.mahefa.ocr.scan2sheet.common.WriterModeContextHolder;
import com.mahefa.ocr.scan2sheet.common.enumerator.WriterMode;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class BackgroundTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        WriterMode writerMode = WriterModeContextHolder.getMode();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                // Restore context
                WriterModeContextHolder.setMode(writerMode);

                runnable.run();
            } finally {
                MDC.clear();
                WriterModeContextHolder.clear();
            }
        };
    }
}
