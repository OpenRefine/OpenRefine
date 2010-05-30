package com.metaweb.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRecords;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.browsing.Engine.Mode;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.sorting.SortingRecordVisitor;
import com.metaweb.gridworks.sorting.SortingRowVisitor;
import com.metaweb.gridworks.templating.Parser;
import com.metaweb.gridworks.templating.Template;
import com.metaweb.gridworks.util.ParsingUtilities;

public class TemplatingExporter implements Exporter {
    public String getContentType() {
        return "application/x-unknown";
    }
    
    public boolean takeWriter() {
        return true;
    }
    
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
        throw new RuntimeException("Not implemented");
    }
    
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
    	String limitString = options.getProperty("limit");
    	int limit = limitString != null ? Integer.parseInt(limitString) : -1;
    	
        JSONObject sortingJson = null;
        try{
            String json = options.getProperty("sorting");
            sortingJson = (json == null) ? null : 
            	ParsingUtilities.evaluateJsonStringToObject(json);
        } catch (JSONException e) {
        }
        
        Template template;
        try {
			template = Parser.parse(options.getProperty("template"));
		} catch (ParsingException e) {
			throw new IOException("Missing or bad template", e);
		}
		
		template.setPrefix(options.getProperty("prefix"));
		template.setSuffix(options.getProperty("suffix"));
		template.setSeparator(options.getProperty("separator"));

        if (engine.getMode() == Mode.RowBased) {
            FilteredRows filteredRows = engine.getAllFilteredRows();
            RowVisitor visitor = template.getRowVisitor(writer, limit);
            
            if (sortingJson != null) {
            	try {
                	SortingRowVisitor srv = new SortingRowVisitor(visitor);
					srv.initializeFromJSON(project, sortingJson);
					
            		if (srv.hasCriteria()) {
            			visitor = srv;
            		}
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
            
            filteredRows.accept(project, visitor);
        } else {
            FilteredRecords filteredRecords = engine.getFilteredRecords();
            RecordVisitor visitor = template.getRecordVisitor(writer, limit);
            
            if (sortingJson != null) {
            	try {
            		SortingRecordVisitor srv = new SortingRecordVisitor(visitor);
            		srv.initializeFromJSON(project, sortingJson);
            		
            		if (srv.hasCriteria()) {
            			visitor = srv;
            		}
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
            
            filteredRecords.accept(project, visitor);
        }
    }
    
}
