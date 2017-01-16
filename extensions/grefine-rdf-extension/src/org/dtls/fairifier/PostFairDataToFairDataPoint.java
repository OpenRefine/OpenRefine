package org.dtls.fairifier;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.deri.grefine.rdf.utils.HttpUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONWriter;
import org.json.JSONObject;
import org.json.JSONException;
import com.google.refine.commands.Command;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.io.InputStream;
import nl.dtl.fairmetadata.io.*;
import nl.dtl.fairmetadata.model.*;
import java.util.List;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.sail.memory.model.CalendarMemLiteral;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.LiteralImpl;
import nl.dtl.fairmetadata.utils.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import javax.xml.datatype.DatatypeConfigurationException; 
import org.apache.commons.io.IOUtils;
import java.util.ArrayList; 
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.IRI;
import nl.dtl.fairmetadata.model.*;
import java.util.Date;
import java.net.URL;
import org.deri.grefine.rdf.app.ApplicationContext;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import org.openrdf.rio.RDFWriter; 
import org.openrdf.repository.Repository;
import org.openrdf.rio.turtle.TurtleWriter;
import java.io.StringWriter;
import org.deri.grefine.rdf.exporters.RdfExporter;


/**
 * 
 * @author Shamanou van Leeuwen
 * @date 7-11-2016
 *
 */

public class PostFairDataToFairDataPoint extends Command{
    private static final ValueFactory f = SimpleValueFactory.getInstance();
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ArrayList<IRI> datasetUris = new ArrayList<IRI>();
        ArrayList<IRI> distributionUris = new ArrayList<IRI>();
        ArrayList<IRI> catalogThemes = new ArrayList<IRI>();
        ArrayList<IRI> datasetThemes = new ArrayList<IRI>();
        ArrayList<Literal> keyWords = new ArrayList<Literal>();
        Identifier identifier = new Identifier();
        Agent agent = new Agent();
        Date date = new Date();
        
        String catalogString;
        String datasetString;
        String distributionString;
        
        CatalogMetadata catalogMetadata = new CatalogMetadata();
        DatasetMetadata datasetMetadata = new DatasetMetadata();
        DistributionMetadata distributionMetadata = new DistributionMetadata();
        
        Engine engine = null;
        Project project = getProject(req);
        try{
            engine = getEngine(req, project);
        }catch(Exception ex){}
        ApplicationContext ctxt = new ApplicationContext(); 
        RdfExporter exporter = new RdfExporter(ctxt, org.openrdf.rio.RDFFormat.TURTLE);
        
        try{
            JSONObject jsonObject = new JSONObject(req.getParameter("fdp"));
            JSONObject catalog = jsonObject.getJSONObject("catalog");
            JSONObject dataset = jsonObject.getJSONObject("dataset");
            JSONObject distribution = jsonObject.getJSONObject("distribution");
            
            catalogMetadata.setHomepage(f.createIRI(catalog.getString("_homepage")));
            catalogMetadata.setIssued( f.createLiteral( date ) );
            catalogMetadata.setModified( f.createLiteral( date ) );
            catalogThemes.add(f.createIRI(catalog.getString("_theme")));
            catalogMetadata.setThemeTaxonomy(catalogThemes);
            catalogMetadata.setTitle(f.createLiteral(catalog.getString("_title")));
            identifier.setIdentifier(f.createLiteral(catalog.getString("_identifier")));
            identifier.setUri( f.createIRI(jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier")));
            catalogMetadata.setIdentifier(identifier);
            agent.setUri( f.createIRI(jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier")));
            agent.setName( f.createLiteral(catalog.getString("_publisher")) );
            catalogMetadata.setPublisher(agent);
            catalogMetadata.setVersion(f.createLiteral(catalog.getString("_version")));
            catalogMetadata.setUri(f.createIRI(jsonObject.getString("baseUri")));
            
            datasetUris.add(f.createIRI( jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + dataset.getString("_identifier") ));
            
            catalogMetadata.setDatasets(datasetUris);
            datasetMetadata.setLandingPage(f.createIRI(dataset.getString("_landingpage")));
            datasetThemes.add(f.createIRI(dataset.getString("_theme")));
            datasetMetadata.setThemes(datasetThemes);
            String[] keywordArray = dataset.getString("_keyword").split(",");
            for (String keyword : keywordArray){
                keyWords.add(f.createLiteral(keyword) );
            }
            datasetMetadata.setKeywords(keyWords);
            
            distributionUris.add( f.createIRI( jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + dataset.getString("_identifier") + "/" +  distribution.getString("_identifier") ));
            
            datasetMetadata.setDistribution(distributionUris);
            datasetMetadata.setTitle(f.createLiteral(dataset.getString("_title")));
            identifier.setIdentifier(f.createLiteral(dataset.getString("_identifier")));
            identifier.setUri( f.createIRI(jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + dataset.getString("_identifier")));
            datasetMetadata.setIdentifier(identifier);
            datasetMetadata.setIssued( f.createLiteral(date) );
            datasetMetadata.setModified( f.createLiteral(date) );
            datasetMetadata.setVersion(f.createLiteral(dataset.getString("_version")) );
            datasetMetadata.setDescription(f.createLiteral(dataset.getString("_description")) );
            
            agent.setUri( f.createIRI(jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + dataset.getString("_identifier")));
            agent.setName( f.createLiteral(catalog.getString("_publisher")) );
            datasetMetadata.setPublisher(agent);
            
            datasetMetadata.setUri( f.createIRI( jsonObject.getString("baseUri") + "/" + dataset.getString("_identifier") + "/" ) );
            
            distributionMetadata.setAccessURL( f.createIRI(distribution.getString("_accessUrl")) );
            distributionMetadata.setMediaType(f.createLiteral(distribution.getString("_mediatype")) );
            distributionMetadata.setTitle(f.createLiteral(distribution.getString("_title")) );
            
            identifier.setIdentifier(f.createLiteral(distribution.getString("_identifier")));
            identifier.setUri( f.createIRI(jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + distribution.getString("_identifier") ));
            distributionMetadata.setIdentifier(identifier);
            distributionMetadata.setVersion(f.createLiteral(distribution.getString("_version")) );
            distributionMetadata.setLicense(f.createIRI(distribution.getString("_license")));
            distributionMetadata.setUri( f.createIRI( jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" +  dataset.getString("_identifier") + "/" + distribution.getString("_identifier") ));
            distributionMetadata.setIssued( f.createLiteral( date ) );
            distributionMetadata.setModified( f.createLiteral( date ) );
            
            catalogString = MetadataUtils.getString(catalogMetadata, RDFFormat.TURTLE).replaceAll("\\<" + catalogMetadata.getUri() + "\\>","<>");
            datasetString = MetadataUtils.getString(datasetMetadata, RDFFormat.TURTLE).replaceAll("\\<" + datasetMetadata.getUri() + "\\>","<>");
            distributionString = MetadataUtils.getString(distributionMetadata, RDFFormat.TURTLE).replaceAll("\\<" + distributionMetadata.getUri() + "\\>","<>");
            
            String catalogPost = IOUtils.toString(HttpUtils.post(jsonObject.getString("baseUri").replaceAll("/$", "") + "?catalogID=" + catalog.getString("_identifier"), catalogString).getContent(), "UTF-8");
            String datasetPost = IOUtils.toString(HttpUtils.post(jsonObject.getString("baseUri").replaceAll("/$", "") + "/" + catalog.getString("_identifier") + "?datasetID=" + dataset.getString("_identifier"), datasetString).getContent(),"UTF-8");
            String distributionPost = IOUtils.toString(HttpUtils.post(jsonObject.getString("baseUri").replaceAll("/$", "") + "/" + catalog.getString("_identifier") + "/" +  dataset.getString("_identifier") + "?distributionID=" + distribution.getString("_identifier"), distributionString).getContent(),"UTF-8");

            
            String data = exporter.exportToString(project, engine, new TurtleWriter(new StringWriter()));
            PushFairDataToResourceAdapter adapter = new PushFairDataToResourceAdapter();
            URL host = new URL(req.getParameter("_directory"));
            adapter.setResource(
                    new FtpResource(host, req.getParameter("_username"), 
                            req.getParameter("_password"), 
                            req.getParameter("_host")
                    )
            );
            adapter.push();
            
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(res.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("catalogPost"); writer.value(catalogPost);
            writer.key("datasetPost"); writer.value(datasetPost);
            writer.key("distributionPost"); writer.value(distributionPost);
            writer.endObject();

        }catch(Exception ex){
            respondException(res, ex);
        }
    }
}