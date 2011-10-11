package com.google.refine.exporters;

import java.util.List;

import org.json.JSONObject;


/**
 * An interface to be implemented by exporters which use 
 * {@link CustomizableTabularExporterUtilities#exportRows(com.google.refine.model.Project, com.google.refine.browsing.Engine, java.util.Properties, TabularSerializer)}
 *
 */
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
