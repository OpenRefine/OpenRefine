package com.google.refine.exporters;

import java.util.List;

import org.json.JSONObject;


public interface TabularSerializer {
    static public class CellData {
        final public String columnName;
        final public Object value;
        final public String text;
        final public String link;
        
        public CellData(String columnName, Object value, String text, String link) {
            this.columnName = columnName;
            this.value = value;
            this.text = text;
            this.link = link;
        }
    }
    
    public void startFile(JSONObject options);
    
    public void endFile();
    
    public void addRow(List<CellData> cells, boolean isHeader);
}
