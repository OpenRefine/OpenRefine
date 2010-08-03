package com.google.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.google.gridworks.browsing.Engine;
import com.google.gridworks.model.Project;

public interface Exporter {
    public String getContentType();
    
    public boolean takeWriter();
    
    public void export(Project project, Properties options, Engine engine, OutputStream outputStream) throws IOException;
    
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException;
}
