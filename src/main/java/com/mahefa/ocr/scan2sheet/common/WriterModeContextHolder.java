package com.mahefa.ocr.scan2sheet.common;

import com.mahefa.ocr.scan2sheet.common.enumerator.WriterMode;

public class WriterModeContextHolder {

    private static final ThreadLocal<WriterMode> CURRENT_WRITER_MODE = new ThreadLocal<>();

    public static void setMode(WriterMode mode) {
        CURRENT_WRITER_MODE.set(mode);
    }

    public static WriterMode getMode() {
        return CURRENT_WRITER_MODE.get();
    }

    public static void clear() {
        CURRENT_WRITER_MODE.remove();
    }
}
