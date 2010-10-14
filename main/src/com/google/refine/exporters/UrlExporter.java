package com.google.refine.exporters;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;


public interface UrlExporter extends Exporter {

    
    public void export(Project project, Properties options, Engine engine, URL url) throws IOException;

}
