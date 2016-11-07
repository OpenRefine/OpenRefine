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
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.openrdf.rio.helpers.StatementCollector;
import java.io.ByteArrayInputStream;
import org.openrdf.model.Statement;
import java.util.ArrayList;
import java.io.InputStream;
import nl.dtl.fairmetadata.io.*;
import nl.dtl.fairmetadata.model.*;
import org.openrdf.model.URI;
import java.util.List;

/**
 * 
 * @author Shamanou van Leeuwen
 * @date 1-11-2016
 *
 */

public class GetFairDataPointInfoCommand extends Command{
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String uri = req.getParameter("uri");
        try{
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(res.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            if (req.getParameter("layer").equals("catalog")){
                writer.key("content"); writer.value(this.getFdpCatalogs(uri));
            } else if(req.getParameter("layer").equals("dataset")){
                writer.key("content"); writer.value(this.getFdpDatasets(uri));
            }
            
            writer.endObject();
        }catch(Exception e){
            respondException(res, e);
        }
    }

    public ArrayList<DatasetMetadata> getFdpDatasets(String url) throws IOException, RDFParseException, RDFHandlerException{
        ArrayList<DatasetMetadata> out = new ArrayList<DatasetMetadata>();
        TurtleParser parser = new TurtleParser();
        StatementCollector rdfStatementCollector = new StatementCollector();
        parser.setRDFHandler(rdfStatementCollector);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url).getContent()));        
        parser.parse(reader, url);
        CatalogMetadataParser catalogMetadataParser = new CatalogMetadataParser();
        DatasetMetadataParser datasetMetadataParser = new DatasetMetadataParser(); 
        URI uri = new URIImpl(url);
        List<URI> datasetUris = catalogMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()), uri).getDatasets();
        for (URI u : datasetUris){
            reader = new BufferedReader(new InputStreamReader(HttpUtils.get(u.toString()).getContent()));        
            parser.parse(reader, u.toString());
            out.add(datasetMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()), u));
        }
        return out;
    }
    
    public ArrayList<CatalogMetadata> getFdpCatalogs(String url) throws IOException, RDFParseException, RDFHandlerException{
        ArrayList<CatalogMetadata> out = new ArrayList<CatalogMetadata>();
        TurtleParser parser = new TurtleParser();
        StatementCollector rdfStatementCollector = new StatementCollector();
        parser.setRDFHandler(rdfStatementCollector);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url).getContent()));        
        parser.parse(reader, url);
        FDPMetadataParser fdpParser = new FDPMetadataParser();
        CatalogMetadataParser catalogMetadataParser = new CatalogMetadataParser();
        URI uri = new URIImpl(url);
        List<URI> catalogUris = fdpParser.parse(new ArrayList(rdfStatementCollector.getStatements()), uri).getCatalogs();
        for (URI u : catalogUris){
            reader = new BufferedReader(new InputStreamReader(HttpUtils.get(u.toString()).getContent()));        
            parser.parse(reader, u.toString());
            out.add(catalogMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()), u));
        }
        return out;
    }
}