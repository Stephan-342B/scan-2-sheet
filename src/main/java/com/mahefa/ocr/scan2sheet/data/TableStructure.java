package com.mahefa.ocr.scan2sheet.data;

import com.mahefa.ocr.scan2sheet.common.properties.AppProperties;
import com.mahefa.ocr.scan2sheet.data.property.RangeSelection;
import com.mahefa.ocr.scan2sheet.data.property.AxisProperty;
import org.springframework.stereotype.Component;

@Component
public class TableStructure {

    private AxisProperty rowProperty;
    private AxisProperty columnProperty;
    private int maxRowCount;

    public TableStructure(AppProperties appProperties) {
        AppProperties.Table table = appProperties.getTable();
        rowProperty = new AxisProperty(new RangeSelection(table.getRow().getSelect()));
        rowProperty.setHeaderPresent(table.getRow().isHeaderPresent());
        columnProperty = new AxisProperty(new RangeSelection(table.getCol().getSelect()));
        maxRowCount = table.getRow().getMaxCount();
    }

    public AxisProperty getRowProperty() {
        return rowProperty;
    }

    public AxisProperty getColumnProperty() {
        return columnProperty;
    }

    public int getMaxRowCount() {
        return maxRowCount;
    }
}
