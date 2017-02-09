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
import java.lang.System;
import java.lang.Exception;
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
        
        try{
            StringBuffer jb = new StringBuffer();
            String line = null;
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
            jb.append(line);
   
            JSONObject fdp = new JSONObject(jb.toString().split("#%SPLITHERE%#")[0]);
            JSONObject catalog = fdp.getJSONObject("catalog");

            JSONObject dataset = fdp.getJSONObject("dataset");

            JSONObject distribution = fdp.getJSONObject("distribution");
           
//          optional
            try{
                catalogMetadata.setHomepage(f.createIRI(catalog.getString("'http://xmlns.com/foaf/0.1/homepage'")));
            }catch(Exception e){}
            for (int i = 0; i < catalog.getJSONArray("http://www.w3.org/ns/dcat#themeTaxonomy").length(); i++){
                catalogThemes.add(f.createIRI(catalog.getJSONArray("http://www.w3.org/ns/dcat#themeTaxonomy").getJSONObject(i).toString()));
            }
            catalogMetadata.setThemeTaxonomy(catalogThemes);
            catalogMetadata.setTitle(f.createLiteral(catalog.getString("http://purl.org/dc/terms/title")));
            identifier.setIdentifier(f.createLiteral(catalog.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier")));
            identifier.setUri( f.createIRI(catalog.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier")));
            catalogMetadata.setIdentifier(identifier);
            agent.setUri( f.createIRI(catalog.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier")));
            agent.setName( f.createLiteral(catalog.getString("http://purl.org/dc/terms/publisher")) );
            catalogMetadata.setPublisher(agent);
            catalogMetadata.setRights(f.createIRI(catalog.getString("http://purl.org/dc/terms/rights")));
            catalogMetadata.setVersion(f.createLiteral(catalog.getString("http://purl.org/dc/terms/hasVersion")));
            catalogMetadata.setUri(f.createIRI(fdp.getString("baseUri")));
            catalogMetadata.setIssued(f.createLiteral(date));
            catalogMetadata.setModified(f.createLiteral(date));
//          optional
            try{
                datasetMetadata.setLandingPage(f.createIRI(dataset.getString("http://www.w3.org/ns/dcat#landingPage")));
            }catch (Exception e){}
            for (int i = 0; i <  dataset.getJSONArray("http://www.w3.org/ns/dcat#theme").length(); i++){
                datasetThemes.add(f.createIRI(dataset.getJSONArray("http://www.w3.org/ns/dcat#theme").getJSONObject(i).toString()));
            }
            datasetMetadata.setThemes(datasetThemes);
//          optional
            for (int i = 0; i < dataset.getJSONArray("http://www.w3.org/ns/dcat#keyword").length(); i++){
                keyWords.add(f.createLiteral(dataset.getJSONArray("http://www.w3.org/ns/dcat#keyword").getJSONObject(i).toString()));
            }
            datasetMetadata.setKeywords(keyWords);
            datasetMetadata.setContactPoint(f.createIRI(dataset.getString("http://www.w3.org/ns/dcat#contactPoint")));
            datasetMetadata.setTitle(f.createLiteral(dataset.getString("http://purl.org/dc/terms/title")));
            identifier = new Identifier();
            identifier.setIdentifier(f.createLiteral(dataset.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier")));
            identifier.setUri( f.createIRI(fdp.getString("baseUri") + "/datasetID/" + dataset.getString("http://purl.org/dc/terms/title")+"_"+dataset.getString("http://purl.org/dc/terms/hasVersion")));
            datasetMetadata.setIdentifier(identifier);
            datasetMetadata.setIssued( f.createLiteral(date));
            datasetMetadata.setModified( f.createLiteral(date) );
            datasetMetadata.setVersion(f.createLiteral(dataset.getString("http://purl.org/dc/terms/hasVersion")) );
            datasetMetadata.setLanguage(f.createIRI(dataset.getString("http://purl.org/dc/terms/language")));            
            datasetMetadata.setRights(f.createIRI(dataset.getString("http://purl.org/dc/terms/rights")));
//          optional
            datasetMetadata.setDescription(f.createLiteral(dataset.getString("http://purl.org/dc/terms/description")) );
            String cUri = fdp.getString("baseUri") + "/catalog/" + catalog.getString("http://purl.org/dc/terms/title")+"_"+catalog.getString("http://purl.org/dc/terms/hasVersion"); 
//            System.out.println("cUri : " + cUri);
            datasetMetadata.setParentURI( f.createIRI(cUri) );
            agent = new Agent();
            agent.setUri( f.createIRI(fdp.getString("baseUri") + "/datasetAgent/" + dataset.getString("http://purl.org/dc/terms/title")+"_"+dataset.getString("http://purl.org/dc/terms/hasVersion")));
            agent.setName( f.createLiteral(dataset.getString("http://purl.org/dc/terms/publisher")) );
            datasetMetadata.setPublisher(agent);
            
            datasetMetadata.setUri( f.createIRI( fdp.getString("baseUri") + "/" + catalog.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier")+"_"+catalog.getString("http://purl.org/dc/terms/hasVersion") + "/" + dataset.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier")+"_"+dataset.getString("http://purl.org/dc/terms/hasVersion") ));
            
            distributionMetadata.setAccessURL(f.createIRI("ftp://" + fdp.getString("username") + ":" + fdp.getString("password") + "@" + fdp.getString("ftpHost") + fdp.getString("directory") + "FAIRdistribution_" + distribution.getString("_identifier") + ".ttl") );
//          optional
            distributionMetadata.setMediaType(f.createLiteral("application/rdf-turtle"));
            distributionMetadata.setTitle(f.createLiteral(distribution.getString("http://purl.org/dc/terms/title")) );
            distributionMetadata.setParentURI( f.createIRI( fdp.getString("baseUri") +"/dataset/" + dataset.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier") ));
            identifier = new Identifier();
            identifier.setIdentifier(f.createLiteral(distribution.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier")));
            identifier.setUri( f.createIRI(fdp.getString("baseUri") + "/distributionID/" + distribution.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier") ));
            distributionMetadata.setIdentifier(identifier);
            distributionMetadata.setVersion(f.createLiteral(distribution.getString("http://purl.org/dc/terms/hasVersion")) );
//          optional
            distributionMetadata.setLicense(f.createIRI(distribution.getString("http://purl.org/dc/terms/license")));
            distributionMetadata.setUri( f.createIRI( fdp.getString("baseUri") + "/" + catalog.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier") + "/" +  dataset.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier") + "/" + distribution.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier") ));
            distributionMetadata.setIssued(f.createLiteral(date));
            distributionMetadata.setModified(f.createLiteral(date));
            distributionMetadata.setDescription(f.createLiteral(distribution.getString("http://purl.org/dc/terms/description")) );

            
            catalogString = MetadataUtils.getString(catalogMetadata, RDFFormat.TURTLE).replaceAll("\\<" + catalogMetadata.getUri() + "\\>","<>");
            datasetString = MetadataUtils.getString(datasetMetadata, RDFFormat.TURTLE).replaceAll("\\<" + datasetMetadata.getUri() + "\\>","<>");
            distributionString = MetadataUtils.getString(distributionMetadata, RDFFormat.TURTLE).replaceAll("\\<" + distributionMetadata.getUri() + "\\>","<>");
//            
//            System.out.println(catalogString);
//            System.out.println(datasetString);
//            System.out.println(distributionString);
            String catalogPost = null;
            String datasetPost = null;
            if (!catalog.getBoolean("_exists")){
                catalogPost = IOUtils.toString(HttpUtils.post(fdp.getString("baseUri") + "/catalog?catalogID=" + catalog.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier"), catalogString).getContent(), "UTF-8");
            }
            if (!dataset.getBoolean("_exists")){
                datasetPost = IOUtils.toString(HttpUtils.post(fdp.getString("baseUri") + "/dataset?datasetID=" + dataset.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier"), datasetString).getContent(),"UTF-8");
            }
            String distributionPost = IOUtils.toString(HttpUtils.post(fdp.getString("baseUri") + "/distribution?distributionID=" + distribution.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier"), distributionString).getContent(),"UTF-8");
            
            String data = jb.toString().split("#%SPLITHERE%#")[1];
            PushFairDataToResourceAdapter adapter = new PushFairDataToResourceAdapter();
            Resource r = new FtpResource(
                    fdp.getString("ftpHost"), 
                    fdp.getString("username"), 
                    fdp.getString("password"), 
                    fdp.getString("directory"),
                    "FAIRdistribution_" + distribution.getString("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier") +"_" +  distribution.getString("http://purl.org/dc/terms/hasVersion")  + ".ttl");
            r.setFairData(data);
            adapter.setResource(r);
            adapter.push();
            
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(res.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            if (!catalog.getBoolean("_exists")){
                writer.key("catalogPost"); writer.value(catalogPost);
            }
            if (!dataset.getBoolean("_exists")){
                writer.key("datasetPost"); writer.value(datasetPost);
            }
            writer.key("distributionPost"); writer.value(distributionPost);
            writer.endObject();

        }catch(Exception ex){
            respondException(res, ex);
        }
    }
}
