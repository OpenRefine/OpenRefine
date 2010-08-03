package com.metaweb.gridworks.rdf.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Properties;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.exporters.Exporter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.rdf.Node;
import com.metaweb.gridworks.rdf.RdfSchema;
import com.metaweb.gridworks.rdf.Util;

public class RdfExporter implements Exporter{

    private String format;
    
    public RdfExporter(String f){
        this.format = f;
    }
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
        
        RdfSchema schema = Util.getProjectSchema(project);
        Model model = ModelFactory.createDefaultModel();
        URI baseUri = schema.getBaseUri();
        Node root = schema.getRoot();
        
        Resource[] blanks = new Resource[schema.get_blanks().size()];
        for (int i = 0; i < blanks.length; i++) {
            blanks[i] = model.createResource();
        }
        
        RowVisitor visitor = new RdfRowVisitor(model, baseUri, root,blanks);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, visitor);
        model.write(outputStream);
    }

    
    public void export(Project project, Properties options, Engine engine,
            Writer writer) throws IOException {
        RdfSchema schema = Util.getProjectSchema(project);
        Model model = ModelFactory.createDefaultModel();
        URI baseUri = schema.getBaseUri();
        Node root = schema.getRoot();
        
        Resource[] blanks = new Resource[schema.get_blanks().size()];
        for (int i = 0; i < blanks.length; i++) {
            blanks[i] = model.createResource();
        }

        RowVisitor visitor = new RdfRowVisitor(model, baseUri, root,blanks);
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, visitor);
        model.write(writer,format);
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
    
    protected static class RdfRowVisitor implements RowVisitor{
        Model model;
        URI base;
        Node root;
        Resource[] blanks;
        public RdfRowVisitor(Model m,URI base, Node root,Resource[] blanks){
            this.model = m;
            this.base = base;
            this.root = root;
            this.blanks = blanks;
        }
        public void end(Project project) {
            // do nothing
            
        }

        public void start(Project project) {
            // do nothing
            
        }

        public boolean visit(Project project, int rowIndex, Row row) {
            root.createNode(base, model, project, row, rowIndex,blanks);
            return false;
        }
    }

}
