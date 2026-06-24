package com.mahefa.ocr.scan2sheet.data.property;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.PriorityQueue;

public class RangeSelection {

    private Integer minValue;
    private Integer maxValue;
    private PriorityQueue<Integer> selectedIndexes = new PriorityQueue<>();

    public RangeSelection(String selectedValueProperty) {
        if (selectedValueProperty != null && !selectedValueProperty.trim().equalsIgnoreCase("NONE")) {
            String[] selectValues = selectedValueProperty.split(",");

            for (String value : selectValues) {
                value = value.trim();

                if (value.contains("-")) {
                    String[] bounds = value.split("-");

                    if (bounds.length == 2) {
                        int start = Integer.parseInt(bounds[0].trim());
                        int end = Integer.parseInt(bounds[1].trim());

                        for (int i = start; i <= end; i++) {
                            addSelectedIndex(i);
                        }
                    }
                } else {
                    int index = Integer.parseInt(value);
                    addSelectedIndex(index);
                }
            }
        }

        // Accept all, no filtering
    }

    public Integer getMinValue() {
        if (CollectionUtils.isNotEmpty(selectedIndexes)) {
            return selectedIndexes.peek();
        }

        return 0;
    }

    public Integer getMaxValue() {
        if (maxValue == null && CollectionUtils.isNotEmpty(selectedIndexes)) {
            return Collections.max(selectedIndexes);
        }

        return maxValue;
    }

    public PriorityQueue<Integer> getSelectedIndexes() {
        return selectedIndexes;
    }

    /**
     * Returns true if the current index is not selected (i.e., should be skipped).
     */
    public boolean skip(int idx) {
        return !isSelected(idx);
    }

    public boolean isSelected(int idx) {
        return CollectionUtils.isEmpty(selectedIndexes) || selectedIndexes.contains(idx);
    }

    public void addSelectedIndex(Integer value) {
        selectedIndexes.add(value);
        maxValue = value;
    }

    public void clearSelection() {
        selectedIndexes.clear();
    }
}
