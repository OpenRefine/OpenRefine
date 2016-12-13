package org.dtls.fairifier;

import org.deri.grefine.rdf.exporters.RdfExporter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.google.refine.commands.Command;
import java.net.URL;
import org.deri.grefine.rdf.app.ApplicationContext;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import org.openrdf.rio.RDFWriter; 
import org.openrdf.repository.Repository;
import org.openrdf.rio.turtle.TurtleWriter;
import java.io.StringWriter;
/**
 * 
 * @author Shamanou van Leeuwen
 * @date 13-12-2016
 *
 */

public class PushFairDataToFtpCommand extends Command{
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {
        Engine engine = null;
        Project project = getProject(req);
        try{
            engine = getEngine(req, project);
        }catch(Exception ex){}
        ApplicationContext ctxt = new ApplicationContext(); 
        RdfExporter exporter = new RdfExporter(ctxt, org.openrdf.rio.RDFFormat.TURTLE);
        String data = exporter.exportToString(project, engine, new TurtleWriter(new StringWriter()));
        PushFairDataToResourceAdapter adapter = new PushFairDataToResourceAdapter();
        URL host = new URL(req.getParameter("location"));
        adapter.setResource(
                new FtpResource(host, req.getParameter("username"), 
                        req.getParameter("password"), 
                        req.getParameter("name")
                )
        );
        adapter.push();
    }
}
