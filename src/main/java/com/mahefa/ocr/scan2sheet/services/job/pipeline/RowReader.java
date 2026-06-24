package com.mahefa.ocr.scan2sheet.services.job.pipeline;

import nu.pattern.OpenCV;
import com.mahefa.ocr.scan2sheet.data.RowContext;
import com.mahefa.ocr.scan2sheet.services.job.delegate.CustomMultiResourceItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class RowReader implements ItemReader<RowContext> {

    private final CustomMultiResourceItemReader<RowContext> fileReader;

    @Autowired
    public RowReader(CustomMultiResourceItemReader<RowContext> fileReader) {
        this.fileReader = fileReader;
        OpenCV.loadLocally();
    }

    @Override
    public RowContext read() throws Exception {
        return fileReader.read();
    }
}