package com.google.refine.exporters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.sorting.SortingRecordVisitor;
import com.google.refine.sorting.SortingRowVisitor;
import com.google.refine.templating.Parser;
import com.google.refine.templating.Template;
import com.google.refine.util.ParsingUtilities;

public class TemplatingExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "application/x-unknown";
    }

    @Override
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
        
        String templateString = options.getProperty("template");
        String prefixString = options.getProperty("prefix");
        String suffixString = options.getProperty("suffix");
        String separatorString = options.getProperty("separator");
        
        Template template;
        try {
            template = Parser.parse(templateString);
        } catch (ParsingException e) {
            throw new IOException("Missing or bad template", e);
        }
        
        template.setPrefix(prefixString);
        template.setSuffix(suffixString);
        template.setSeparator(separatorString);
        
        if (!"true".equals(options.getProperty("preview"))) {
            StringWriter stringWriter = new StringWriter();
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            try {
                jsonWriter.object();
                jsonWriter.key("template"); jsonWriter.value(templateString);
                jsonWriter.key("prefix"); jsonWriter.value(prefixString);
                jsonWriter.key("suffix"); jsonWriter.value(suffixString);
                jsonWriter.key("separator"); jsonWriter.value(separatorString);
                jsonWriter.endObject();
            } catch (JSONException e) {
                // ignore
            }
            
            project.getMetadata().getPreferenceStore().put("exporters.templating.template", stringWriter.toString());
        }
        
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
