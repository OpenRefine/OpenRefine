package com.google.refine.rdf.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Map.Entry;
import java.util.Properties;

import org.openrdf.model.BNode;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.exporters.Exporter;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.rdf.Node;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.VocabularyIndexException;

public class RdfExporter implements Exporter{

    private RDFFormat format;
    private ApplicationContext applicationContext;
    
	public RdfExporter(ApplicationContext ctxt, RDFFormat f){
        this.format = f;
        this.applicationContext = ctxt;
    }
	
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
    
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
    	RdfSchema schema;
    	try{
    		schema = Util.getProjectSchema(applicationContext,project);
    	}catch(VocabularyIndexException ve){
    		throw new IOException("Unable to create index for RDF schema",ve);
    	}
        Repository model = buildModel(project, engine, schema);
        try{
        	RepositoryConnection con = model.getConnection();
        	try{
        		RDFWriter writer = Rio.createWriter(format, outputStream);
        		for(Entry<String, String> prefix:schema.getPrefixesMap().entrySet()){
        			writer.handleNamespace(prefix.getKey(),prefix.getValue());
        		}
        		con.export(writer);
			}finally{
        		con.close();
        	}
        }catch(RepositoryException ex){
        	throw new RuntimeException(ex);
        }catch(RDFHandlerException ex){
        	throw new RuntimeException(ex);
        }
    }

    
    public void export(Project project, Properties options, Engine engine,
            Writer writer) throws IOException {
    	RdfSchema schema;
    	try{
    		schema = Util.getProjectSchema(applicationContext,project);
    	}catch(VocabularyIndexException ve){
    		throw new IOException("Unable to create index for RDF schema",ve);
    	}
        Repository model = buildModel(project, engine, schema);
        try{
        	RepositoryConnection con = model.getConnection();
        	try{
        		RDFWriter w = Rio.createWriter(format, writer);
        		for(Entry<String, String> prefix:schema.getPrefixesMap().entrySet()){
        			w.handleNamespace(prefix.getKey(),prefix.getValue());
        		}
        		con.export(w);
			}finally{
        		con.close();
        	}
        }catch(RepositoryException ex){
        	throw new RuntimeException(ex);
        }catch(RDFHandlerException ex){
        	throw new RuntimeException(ex);
        }
    }

    public Repository buildModel(final Project project, Engine engine, RdfSchema schema) throws IOException{
    	RdfRowVisitor visitor = new RdfRowVisitor(schema) {
			
			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				root.createNode(baseUri, factory, con, project, row, rowIndex,blanks);
	            return false;
			}
		};
		Repository model = buildModel(project, engine,visitor);
		
        return model;
    }
    
    public static Repository buildModel(Project project, Engine engine, RdfRowVisitor visitor) {
//    	RdfSchema schema = Util.getProjectSchema(project);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, visitor);
        return visitor.getModel();
        
    }
    
    public String getContentType() {
        if(format.equals("N3")){
            return "text/rdf+n3";
        }else{
            return "application/rdf+xml";
        }
    }

    public boolean takeWriter() {
        return true;
    }
    
    public static abstract class RdfRowVisitor implements RowVisitor{
        protected Repository model;
        protected URI baseUri;
        protected BNode[] blanks;
        protected Node root;
        private RdfSchema schema;
        
        protected ValueFactory factory;
        protected RepositoryConnection con;
        
        public Repository getModel() {
			return model;
		}

        public RdfRowVisitor(RdfSchema schema){
        	this.schema = schema;
        	baseUri = schema.getBaseUri();
            root = schema.getRoot();

            //initilaizing repository
            model = new SailRepository(new MemoryStore());
            try{
            	model.initialize();
            	RepositoryConnection con = model.getConnection();
            	try{
            		ValueFactory factory = con.getValueFactory();
            		blanks = new BNode[schema.get_blanks().size()];
            		for (int i = 0; i < blanks.length; i++) {
            			blanks[i] = factory.createBNode();
            		}
            	}finally{
            		con.close();
            	}
            }catch(RepositoryException ex){
            	throw new RuntimeException(ex);
            }
        }
        public void end(Project project) {
        	try {
				if(con.isOpen()){
					con.close();
				}
			} catch (RepositoryException e) {
				throw new RuntimeException("",e);
			}
        }

        public void start(Project project) {
        	try{
        		con = model.getConnection();
        		factory = con.getValueFactory();
        	}catch(RepositoryException ex){
        		throw new RuntimeException("",ex);
        	}
        }

        abstract public boolean visit(Project project, int rowIndex, Row row);
        public RdfSchema getRdfSchema(){
        	return schema;
        }
    }

}
