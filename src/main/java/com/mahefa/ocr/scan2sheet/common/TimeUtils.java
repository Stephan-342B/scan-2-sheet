package com.mahefa.ocr.scan2sheet.common;

import java.time.Duration;

public class TimeUtils {

    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }
}
