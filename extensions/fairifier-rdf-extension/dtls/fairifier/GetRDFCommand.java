package org.dtls.fairifier;

import java.lang.Exception;
import java.lang.System;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.deri.grefine.rdf.utils.HttpUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONWriter;
import com.google.refine.commands.Command;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.io.InputStream;
import org.deri.grefine.rdf.app.ApplicationContext;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import org.openrdf.rio.RDFWriter; 
import org.openrdf.repository.Repository;
import org.openrdf.rio.turtle.TurtleWriter;
import java.io.StringWriter;
import org.deri.grefine.rdf.exporters.RdfExporter;
import java.lang.System;
import java.lang.Exception;
/**
 * 
 * @author Shamanou van Leeuwen
 * @date 1-11-2016
 *
 */

public class GetRDFCommand extends Command{
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Engine engine = null;
        Project project = getProject(req);
        try{
            engine = getEngine(req, project);
        }catch(Exception ex){}
        ApplicationContext ctxt = new ApplicationContext();
        RdfExporter exporter = new RdfExporter(ctxt, org.openrdf.rio.RDFFormat.TURTLE);
        StringWriter stringwriter = new StringWriter();
        exporter.export(project, null, engine, stringwriter);
        String data = stringwriter.toString();
        try{
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(res.getWriter());
            writer.object();
            writer.key("data"); writer.value(data);
            writer.endObject();
        }catch(Exception e){
            respondException(res, e);
        }
    }
}