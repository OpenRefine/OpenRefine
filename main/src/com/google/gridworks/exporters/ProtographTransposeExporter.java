package com.google.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.google.gridworks.browsing.Engine;
import com.google.gridworks.model.Project;
import com.google.gridworks.protograph.Protograph;
import com.google.gridworks.protograph.transpose.MqlwriteLikeTransposedNodeFactory;
import com.google.gridworks.protograph.transpose.TransposedNodeFactory;
import com.google.gridworks.protograph.transpose.Transposer;
import com.google.gridworks.protograph.transpose.TripleLoaderTransposedNodeFactory;

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
        
        Protograph protograph = (Protograph) project.overlayModels.get("freebaseProtograph");
        if (protograph != null) {
            TransposedNodeFactory nodeFactory = createNodeFactory(project, writer);
            
            Transposer.transpose(project, engine.getAllFilteredRows(), 
                    protograph, protograph.getRootNode(0), nodeFactory, -1);
            
            nodeFactory.flush();
        }
    }
    
    abstract protected TransposedNodeFactory createNodeFactory(Project project, Writer writer);
    
    static public class TripleLoaderExporter extends ProtographTransposeExporter {
		public TripleLoaderExporter() {
			super("application/x-unknown");
		}

		@Override
		protected TransposedNodeFactory createNodeFactory(Project project, Writer writer) {
			return new TripleLoaderTransposedNodeFactory(project, writer);
		}
    }

    static public class MqlwriteLikeExporter extends ProtographTransposeExporter {
		public MqlwriteLikeExporter() {
			super("application/x-unknown");
		}

		@Override
		protected TransposedNodeFactory createNodeFactory(Project project, Writer writer) {
			return new MqlwriteLikeTransposedNodeFactory(writer);
		}
    }

}
