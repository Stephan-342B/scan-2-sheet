package com.mahefa.ocr.scan2sheet.data.property;

public class AxisProperty {

    private Integer startIndex;
    private Integer endIndex;
    private RangeSelection rangeSelection;
    private boolean headerPresent;
    private int totalElement;

    public AxisProperty(RangeSelection rangeSelection) {
        this.rangeSelection = rangeSelection;
        this.startIndex = rangeSelection.getMinValue();
        this.endIndex = (rangeSelection.getMaxValue() != null) ? rangeSelection.getMaxValue() + 1 : null;
        this.totalElement = rangeSelection.getSelectedIndexes().size();
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
    }

    public RangeSelection getRangeSelection() {
        return rangeSelection;
    }

    public void setRangeSelection(RangeSelection rangeSelection) {
        this.rangeSelection = rangeSelection;
    }

    public boolean isHeaderPresent() {
        return headerPresent;
    }

    public void setHeaderPresent(boolean headerPresent) {
        this.headerPresent = headerPresent;
    }

    public int getTotalElement() {
        return totalElement;
    }

    public void setTotalElement(int totalElement) {
        this.totalElement = totalElement;
    }
}
