package org.deri.grefine.rdf.commands;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.rdf.Node;
import org.deri.grefine.rdf.RdfSchema;
import org.deri.grefine.rdf.exporters.RdfExporter;
import org.deri.grefine.rdf.exporters.RdfExporter.RdfRowVisitor;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class PreviewRdfCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            String jsonString = request.getParameter("schema");
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            final RdfSchema schema = RdfSchema.reconstruct(json);
            
            RdfRowVisitor visitor = new RdfRowVisitor(schema) {
            	final int limit = 10;
            	int _count;
				@Override
				public boolean visit(Project project, int rowIndex, Row row) {
					if(_count>=limit){
		                return true;
		            }
					for(Node root:roots){
						root.createNode(baseUri, factory, con, project, row, rowIndex,blanks);
					}
		            _count +=1;
		            return false;
				}
			};
			
            Repository model = RdfExporter.buildModel(project, engine, visitor);
            StringWriter sw = new StringWriter();
            try{
            	RepositoryConnection con = model.getConnection();
            	try{
            		RDFWriter w = Rio.createWriter(RDFFormat.TURTLE, sw);
            		for(Vocabulary v:schema.getPrefixesMap().values()){
            			w.handleNamespace(v.getName(), v.getUri());
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
}
