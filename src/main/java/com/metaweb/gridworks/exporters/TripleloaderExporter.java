package com.metaweb.gridworks.exporters;

import java.io.IOException; 
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.protograph.Protograph;
import com.metaweb.gridworks.protograph.transpose.Transposer;
import com.metaweb.gridworks.protograph.transpose.TripleLoaderTransposedNodeFactory;

public class TripleloaderExporter implements Exporter {
    public String getContentType() {
        return "application/x-unknown";
    }
    
    public void export(Project project, Properties options, Engine engine,
            Writer writer) throws IOException {
        
        if (project.protograph != null) {
            Protograph protograph = project.protograph;
            
            TripleLoaderTransposedNodeFactory nodeFactory = new TripleLoaderTransposedNodeFactory(writer);
            
            Transposer.transpose(project, protograph, protograph.getRootNode(0), nodeFactory);
            nodeFactory.flush();
        }
    }

}
