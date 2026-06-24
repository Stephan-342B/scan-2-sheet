package com.mahefa.ocr.scan2sheet.conf;

import net.sourceforge.tess4j.Tesseract;
import com.mahefa.ocr.scan2sheet.common.properties.Tess4jProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Map;

@Configuration
public class Tess4jConfig {

    // Prototype-scoped: Tesseract is NOT thread-safe, so each caller (e.g. each worker thread
    // in the parallel rowExtractorStep) obtains its own instance via ObjectProvider.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Tesseract tesseract(Tess4jProperties tess4jProperties) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tess4jProperties.getData().getPath());
        tesseract.setOcrEngineMode(tess4jProperties.getEngine().getMode());
        tesseract.setPageSegMode(tess4jProperties.getEngine().getPsm());

        if (tess4jProperties.getConf() != null && tess4jProperties.getConf().getTessvariable() != null) {
            Map<String, String> options = tess4jProperties.getConf().getTessvariable().getOptions();

            if (options != null && !options.isEmpty()) {
                options.forEach(tesseract::setTessVariable);
            }
        }

        return tesseract;
    }
}