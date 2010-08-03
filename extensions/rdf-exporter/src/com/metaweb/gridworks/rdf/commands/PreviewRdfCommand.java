package com.metaweb.gridworks.rdf.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONWriter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.rdf.Node;
import com.metaweb.gridworks.rdf.RdfSchema;
import com.metaweb.gridworks.util.ParsingUtilities;

public class PreviewRdfCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            FilteredRows filteredRows = engine.getAllFilteredRows();
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            String jsonString = request.getParameter("schema");
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            RdfSchema schema = RdfSchema.reconstruct(json);
            
            Model model = ModelFactory.createDefaultModel();
            URI baseUri = schema.getBaseUri();
            Node root = schema.getRoot();
            
            Resource[] blanks = new Resource[schema.get_blanks().size()];
            for (int i = 0; i < blanks.length; i++) {
                blanks[i] = model.createResource();
            }
            
            RowVisitor visitor = new RdfRowVisitor(model, baseUri, root,blanks,20);
            
            filteredRows.accept(project, visitor);
            StringWriter sw = new StringWriter();
            model.write(sw,"N3");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("v");
            writer.value(sw.getBuffer().toString());
            writer.endObject();
            //respond(response, "{v:" + sw.getBuffer().toString() + "}");
        }catch (Exception e) {
            respondException(response, e);
        }
    }
    

    protected static class RdfRowVisitor implements RowVisitor{
        Model model;
        URI base;
        Node root;
        Resource[] blanks;
        int limit;
        
        int _count;
        public RdfRowVisitor(Model m,URI base, Node root,Resource[] blanks,int l){
            this.model = m;
            this.base = base;
            this.root = root;
            this.blanks = blanks;
            this.limit = l;
        }
        public void end(Project project) {
            // do nothing
            
        }

        public void start(Project project) {
            // do nothing
            
        }

        public boolean visit(Project project, int rowIndex, Row row) {
            if(_count>=limit){
                return true;
            }
            root.createNode(base, model, project, row, rowIndex,blanks);
            _count +=1;
            return false;
        }
    }

}
