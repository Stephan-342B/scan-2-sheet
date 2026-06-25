package com.mahefa.ocr.scan2sheet;

import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.common.properties.GoogleApiProperties;
import com.mahefa.ocr.scan2sheet.common.properties.Tess4jProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({
		AppProperties.class,
		Tess4jProperties.class,
		GoogleApiProperties.class
})
@SpringBootApplication
public class Scan2SheetApplication {

	public static void main(String[] args) {
		SpringApplication.run(Scan2SheetApplication.class, args);
	}
}
