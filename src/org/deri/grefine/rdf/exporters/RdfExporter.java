package org.deri.grefine.rdf.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.deri.grefine.rdf.Node;
import org.deri.grefine.rdf.RdfSchema;
import org.deri.grefine.rdf.Util;
import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.deri.grefine.rdf.vocab.VocabularyIndexException;
import org.openrdf.model.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;

import info.aduna.iteration.CloseableIteration;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfExporter implements WriterExporter{

    private RDFFormat format;
    private ApplicationContext applicationContext;
    final static Logger logger = LoggerFactory.getLogger("RdfExporter");

	public RdfExporter(ApplicationContext ctxt, RDFFormat f){
        this.format = f;
        this.applicationContext = ctxt;
    }
	
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
    
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
	    export(project, options, engine, Rio.createWriter(format, outputStream));
    }

	public void export(Project project, Properties options, Engine engine,
					   Writer writer) throws IOException {
		export(project, options, engine, Rio.createWriter(format, writer));
	}

	private void export(Project project, Properties options, Engine engine,
						RDFWriter writer) throws IOException {
    	RdfSchema schema;
    	try{
    		schema = Util.getProjectSchema(applicationContext,project);
    	}catch(VocabularyIndexException ve){
    		throw new IOException("Unable to create index for RDF schema",ve);
    	}
        try{
		for(Vocabulary v:schema.getPrefixesMap().values()){
		        writer.handleNamespace(v.getName(), v.getUri());
		}

		exportModel(project, engine, schema, writer);
        }catch(RDFHandlerException ex){
        	throw new RuntimeException(ex);
        }
    }

    public Repository exportModel(final Project project, Engine engine, RdfSchema schema, RDFWriter writer) throws IOException{
    	RdfRowVisitor visitor = new RdfRowVisitor(schema, writer) {
			
			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				for(Node root:roots){
					root.createNode(baseUri, factory, con, project, row, rowIndex,blanks);

					try {
					    // flush here to preserve root ordering in the output file
					    flushStatements();
					} catch (RepositoryException e) {
					    throw new RuntimeException(e);
					} catch (RDFHandlerException e) {
						throw new RuntimeException(e);
					}
				}

				return false;
			}
		};
		return buildModel(project, engine, visitor);
    }
    
    public static Repository buildModel(Project project, Engine engine, RdfRowVisitor visitor) {
//    	RdfSchema schema = Util.getProjectSchema(project);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, visitor);
        return visitor.getModel();
        
    }
    
    public String getContentType() {
        if(format.equals(RDFFormat.TURTLE)){
            return "text/turtle";
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
        protected List<Node> roots;
        private RdfSchema schema;
        
        protected ValueFactory factory;
        protected RepositoryConnection con;
	    protected RDFWriter writer;
        
        public Repository getModel() {
			return model;
		}

        public RdfRowVisitor(RdfSchema schema, RDFWriter writer){
        	this.schema = schema;
	        this.writer = writer;
        	baseUri = schema.getBaseUri();
            roots = schema.getRoots();

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
		        writer.endRDF();
				if(con.isOpen()){
					con.close();
				}
			} catch (RepositoryException e) {
				throw new RuntimeException("",e);
			} catch (RDFHandlerException e) {
		        throw new RuntimeException("",e);
	        }
        }

        public void start(Project project) {
        	try{
        		con = model.getConnection();
        		factory = con.getValueFactory();

		        // Open RDF output
		        writer.startRDF();

		        // Export namespace information
		        CloseableIteration<? extends Namespace, RepositoryException> nsIter = con.getNamespaces();
		        try {
			    	while (nsIter.hasNext()) {
						Namespace ns = nsIter.next();
						writer.handleNamespace(ns.getPrefix(), ns.getName());
					}
			    } finally {
		            nsIter.close();
		        }

        	}catch(RepositoryException ex){
        		throw new RuntimeException("",ex);
        	} catch (RDFHandlerException e) {
		        e.printStackTrace();
	        }
        }

	    protected void flushStatements() throws RepositoryException, RDFHandlerException{
		    List<Resource> resourceList = con.getContextIDs().asList();
		    Resource[] resources = resourceList.toArray(new Resource[resourceList.size()]);

		    // Export statements
		    CloseableIteration<? extends Statement, RepositoryException> stIter =
				    con.getStatements(null, null, null, false, resources);

		    try {
			    while (stIter.hasNext()) {
				    this.writer.handleStatement(stIter.next());
			    }
		    } finally {
			    stIter.close();
		    }

		    // empty the repository
		    con.clear();
	    }

        abstract public boolean visit(Project project, int rowIndex, Row row);
        public RdfSchema getRdfSchema(){
        	return schema;
        }
    }

}
