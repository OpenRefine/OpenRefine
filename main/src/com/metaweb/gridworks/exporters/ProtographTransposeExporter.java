package com.metaweb.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.protograph.Protograph;
import com.metaweb.gridworks.protograph.transpose.MqlwriteLikeTransposedNodeFactory;
import com.metaweb.gridworks.protograph.transpose.TransposedNodeFactory;
import com.metaweb.gridworks.protograph.transpose.Transposer;
import com.metaweb.gridworks.protograph.transpose.TripleLoaderTransposedNodeFactory;

abstract public class ProtographTransposeExporter implements Exporter {
	final protected String _contentType;
	
	public ProtographTransposeExporter(String contentType) {
		_contentType = contentType;
	}
	
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
    
    public void export(Project project, Properties options, Engine engine,
            Writer writer) throws IOException {
        
        if (project.protograph != null) {
            Protograph protograph = project.protograph;
            
            TransposedNodeFactory nodeFactory = createNodeFactory(writer);
            
            Transposer.transpose(project, engine.getAllFilteredRows(), protograph, protograph.getRootNode(0), nodeFactory, -1);
            
            nodeFactory.flush();
        }
    }
    
    abstract protected TransposedNodeFactory createNodeFactory(Writer writer);
    
    static public class TripleLoaderExporter extends ProtographTransposeExporter {
		public TripleLoaderExporter() {
			super("application/x-unknown");
		}

		@Override
		protected TransposedNodeFactory createNodeFactory(Writer writer) {
			return new TripleLoaderTransposedNodeFactory(writer);
		}
    }

    static public class MqlwriteLikeExporter extends ProtographTransposeExporter {
		public MqlwriteLikeExporter() {
			super("application/x-unknown");
		}

		@Override
		protected TransposedNodeFactory createNodeFactory(Writer writer) {
			return new MqlwriteLikeTransposedNodeFactory(writer);
		}
    }

}
