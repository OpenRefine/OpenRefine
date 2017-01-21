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
import nl.dtl.fairmetadata.io.*;
import nl.dtl.fairmetadata.model.*;
import java.util.List;
import nl.dtl.fairmetadata.utils.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.IRI;
/**
 * 
 * @author Shamanou van Leeuwen
 * @date 1-11-2016
 *
 */

public class GetFairDataPointInfoCommand extends Command{
    private MetadataParserUtils utils = new MetadataParserUtils();
    private static final ValueFactory f = SimpleValueFactory.getInstance();
    
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
                writer.key("content"); 
                writer.value(this.getFdpCatalogs(uri));
            } else if(req.getParameter("layer").equals("dataset")){
                writer.key("content"); 
                writer.value(this.getFdpDatasets(uri));
            }
            
            writer.endObject();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    private ArrayList<DatasetMetadata> getFdpDatasets(String url) throws IOException, RDFParseException, RDFHandlerException, LayerUnavailableException{
        ArrayList<DatasetMetadata> out = new ArrayList<DatasetMetadata>();
        TurtleParser parser = new TurtleParser();
        StatementCollector rdfStatementCollector = new StatementCollector();
        parser.setRDFHandler(rdfStatementCollector);
        BufferedReader reader;
        try{
            reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url).getContent()));
        }catch(Exception e){
            throw new LayerUnavailableException("catalog could not be retrieved");
        }
        parser.parse(reader, url);
        CatalogMetadataParser catalogMetadataParser = utils.getCatalogParser();
        DatasetMetadataParser datasetMetadataParser = utils.getDatasetParser(); 
        List<IRI> datasetUris = catalogMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()), f.createIRI(url)).getDatasets();
        for (IRI u : datasetUris){
            try{
                reader = new BufferedReader(new InputStreamReader(HttpUtils.get(u.toString()).getContent()));        
                parser.parse(reader, u.toString());
                out.add(datasetMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()), u));
            }catch(Exception e){
                throw new LayerUnavailableException("datasets could not be retrieved");
            }
        }
        return out;
    }
    
    private ArrayList<CatalogMetadata> getFdpCatalogs(String url) throws IOException, LayerUnavailableException, RDFParseException, RDFHandlerException{
        ArrayList<CatalogMetadata> out = new ArrayList<CatalogMetadata>();
        TurtleParser parser = new TurtleParser();
        StatementCollector rdfStatementCollector = new StatementCollector();
        parser.setRDFHandler(rdfStatementCollector);
        BufferedReader reader;
        try{
            reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url).getContent()));
        }catch(Exception e){
            throw new LayerUnavailableException("fdp could not be retrieved");
        }        
        parser.parse(reader, url);
        FDPMetadataParser fdpParser = utils.getFdpParser();
        CatalogMetadataParser catalogMetadataParser = utils.getCatalogParser();
        List<IRI> catalogUris = fdpParser.parse(new ArrayList(rdfStatementCollector.getStatements()), f.createIRI(url)).getCatalogs();
        for (IRI u : catalogUris){
            try{
                reader = new BufferedReader(new InputStreamReader(HttpUtils.get(u.toString()).getContent()));        
                parser.parse(reader, u.toString());
                out.add(catalogMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()),u));
            }catch(Exception e){
                throw new LayerUnavailableException("catalogs could not be retrieved");
            }
        }
        return out;
    }
}