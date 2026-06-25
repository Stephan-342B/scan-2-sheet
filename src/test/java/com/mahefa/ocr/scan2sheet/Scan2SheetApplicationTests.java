package com.mahefa.ocr.scan2sheet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Disable Boot's batch auto-run for tests so `mvn install` only verifies the context loads and
// does NOT execute the OCR job or write to Google Sheets.
@SpringBootTest(classes = Scan2SheetApplication.class, properties = "spring.batch.job.enabled=false")
class Scan2SheetApplicationTests {

	@Test
	void contextLoads() {
	}

}
