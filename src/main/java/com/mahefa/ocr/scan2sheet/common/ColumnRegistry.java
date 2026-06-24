package com.mahefa.ocr.scan2sheet.common;

import org.opencv.core.Rect;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ColumnRegistry {

    private final Map<String, List<Rect>> columnMap = new ConcurrentHashMap<>();

    public void register(String filename, List<Rect> columns) {
        columnMap.put(filename, columns);
    }

    public List<Rect> get(String filename) {
        return columnMap.getOrDefault(filename, Collections.emptyList());
    }

    public boolean contains(String filename) {
        return columnMap.containsKey(filename);
    }

    public void clear(String filename) {
        columnMap.remove(filename);
    }

    public void clearAll() {
        columnMap.clear();
    }
}

