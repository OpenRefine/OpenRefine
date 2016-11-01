package org.dtls.fairifier;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.deri.grefine.rdf.utils.HttpUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONWriter;
import com.google.refine.commands.Command;
import org.openrdf.rio.Rio;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.model.Model;
import java.io.ByteArrayInputStream;
import org.openrdf.model.Statement;
import java.util.ArrayList;
import java.io.InputStream;

/**
 * 
 * @author Shamanou van Leeuwen
 * @date 1-11-2016
 *
 */

public class GetFairDataPointCatalogCommand extends Command{
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String uri = req.getParameter("uri");
        try{
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(res.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("content"); writer.value(this.getFdpCatalog(uri));
            writer.endObject();
        }catch(Exception e){
            respondException(res, e);
        }
    }
    
    public ArrayList<String> getFdpCatalog(String url) throws IOException, RDFParseException, RDFHandlerException{
        ArrayList catalogs = new ArrayList<>();
        RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
        StatementCollector rdfStatementCollector = new StatementCollector();
        parser.setRDFHandler(rdfStatementCollector);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url).getContent()));
        
        parser.parse(reader, url);
        for ( Statement s : rdfStatementCollector.getStatements()){
            if (s.getPredicate().getLocalName().equals("contains")){
                catalogs.add(s.getObject().stringValue());
            }
        }
        return catalogs;
    }
}