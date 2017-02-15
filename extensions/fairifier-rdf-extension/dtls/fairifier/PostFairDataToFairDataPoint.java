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
        
        String catalogString = null;
        String datasetString = null;
        String distributionString = null;
        
        CatalogMetadata catalogMetadata = new CatalogMetadata();
        DatasetMetadata datasetMetadata = new DatasetMetadata();
        DistributionMetadata distributionMetadata = new DistributionMetadata();
        
        try{
            StringBuffer jb = new StringBuffer();
            String line = null;
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
            jb.append(line);
   
            JSONObject fdp = new JSONObject(jb.toString()).getJSONObject("metadata");
            JSONObject catalog = fdp.getJSONObject("catalog");

            JSONObject dataset = fdp.getJSONObject("dataset");

            JSONObject distribution = fdp.getJSONObject("distribution");
            if (!catalog.getBoolean("_exists")){

    //          optional
                try{
                    catalogMetadata.setHomepage(f.createIRI(catalog.getString("http://xmlns.com/foaf/0.1/homepage")));
                }catch(Exception ex){}
                catalogThemes.add(f.createIRI(catalog.getString("http://www.w3.org/ns/dcat#themeTaxonomy")));
                catalogMetadata.setThemeTaxonomy(catalogThemes);
                catalogMetadata.setTitle(f.createLiteral(catalog.getString("http://purl.org/dc/terms/title")));
                identifier.setIdentifier(f.createLiteral(catalog.getString("http://purl.org/dc/terms/title")+"_"+catalog.getString("http://purl.org/dc/terms/hasVersion")));
                identifier.setUri( f.createIRI(catalog.getJSONObject("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier").getString("url")));
                catalogMetadata.setIdentifier(identifier);
                agent.setUri( f.createIRI(fdp.getString("baseUri")));
                agent.setName( f.createLiteral(catalog.getJSONObject("http://purl.org/dc/terms/publisher").getString("url")));
                catalogMetadata.setPublisher(agent);
                try{
                    catalogMetadata.setRights(f.createIRI(catalog.getString("http://purl.org/dc/terms/rights")));
                }catch(Exception ex){}
                catalogMetadata.setVersion(f.createLiteral(catalog.getString("http://purl.org/dc/terms/hasVersion")));
                catalogMetadata.setUri(f.createIRI(fdp.getString("baseUri")));
                catalogMetadata.setIssued(f.createLiteral(date));
                catalogMetadata.setModified(f.createLiteral(date));
                catalogString = MetadataUtils.getString(catalogMetadata, RDFFormat.TURTLE).replaceAll("\\<" + catalogMetadata.getUri() + "\\>","<>");
            }
            if (!dataset.getBoolean("_exists")){
    //          optional
                try{
                    datasetMetadata.setLandingPage(f.createIRI(dataset.getString("http://www.w3.org/ns/dcat#landingPage")));
                }catch(Exception ex){}

                for (int i = 0; i <  dataset.getJSONArray("http://www.w3.org/ns/dcat#theme").length(); i++){
                    datasetThemes.add(f.createIRI(dataset.getJSONArray("http://www.w3.org/ns/dcat#theme").getString(i)));
                }
                datasetMetadata.setThemes(datasetThemes);
    //          optional
                try{
                    for (int i = 0; i < dataset.getJSONArray("http://www.w3.org/ns/dcat#keyword").length(); i++){
                        keyWords.add(f.createLiteral(dataset.getJSONArray("http://www.w3.org/ns/dcat#keyword").getString(i)));
                    }
                    datasetMetadata.setKeywords(keyWords);
                }catch(Exception ex){}
//optional
                try{
                    datasetMetadata.setContactPoint(f.createIRI(dataset.getString("http://www.w3.org/ns/dcat#contactPoint")));
                }catch(Exception ex){}      
                datasetMetadata.setTitle(f.createLiteral(dataset.getString("http://purl.org/dc/terms/title")));
                identifier = new Identifier();
                identifier.setIdentifier(f.createLiteral(dataset.getString("http://purl.org/dc/terms/title")+"_"+dataset.getString("http://purl.org/dc/terms/hasVersion")));
                identifier.setUri( f.createIRI( dataset.getJSONObject("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier").getString("url") ));
                datasetMetadata.setIdentifier(identifier);
                datasetMetadata.setIssued( f.createLiteral(date));
                datasetMetadata.setModified( f.createLiteral(date) );
                datasetMetadata.setVersion(f.createLiteral(dataset.getString("http://purl.org/dc/terms/hasVersion")) );
                try{
                    datasetMetadata.setLanguage(f.createIRI(dataset.getJSONArray("http://purl.org/dc/terms/language").getString(0))); 
                }catch(Exception ex){}
                try{      
                    datasetMetadata.setRights(f.createIRI(dataset.getString("http://purl.org/dc/terms/rights")));
                }catch(Exception ex){}
    //          optional
                try{
                    datasetMetadata.setDescription(f.createLiteral(dataset.getString("http://purl.org/dc/terms/description")) );
                }catch(Exception ex){}
                datasetMetadata.setParentURI( f.createIRI(fdp.getString("baseUri") + "/catalog/" + catalog.getString("http://purl.org/dc/terms/title")+"_"+catalog.getString("http://purl.org/dc/terms/hasVersion")) );
                agent = new Agent();
                agent.setUri( f.createIRI(fdp.getString("baseUri") + "/datasetAgent/" + dataset.getString("http://purl.org/dc/terms/title")+"_"+dataset.getString("http://purl.org/dc/terms/hasVersion")));
                agent.setName( f.createLiteral(dataset.getJSONObject("http://purl.org/dc/terms/publisher").getString("url")));
                datasetMetadata.setPublisher(agent);
                datasetMetadata.setUri(f.createIRI( "http://base/catalog/dataset" ));
                datasetString = MetadataUtils.getString(datasetMetadata, RDFFormat.TURTLE).replaceAll("\\<" + datasetMetadata.getUri() + "\\>","<>");
            }            


            distributionMetadata.setAccessURL(f.createIRI("ftp://" + fdp.getString("username") + ":" + fdp.getString("password") + "@" + fdp.getString("ftpHost") + fdp.getString("directory") + "FAIRdistribution_" + distribution.getString("http://purl.org/dc/terms/title")+"_"+distribution.getString("http://purl.org/dc/terms/hasVersion") + ".ttl") );
//          optional
            try{
                distributionMetadata.setMediaType(f.createLiteral("application/rdf-turtle"));
            }catch(Exception ex){}
            distributionMetadata.setTitle(f.createLiteral(distribution.getString("http://purl.org/dc/terms/title")) );
            distributionMetadata.setParentURI( f.createIRI( fdp.getString("baseUri") +"/dataset/" + dataset.getString("http://purl.org/dc/terms/title")+"_"+dataset.getString("http://purl.org/dc/terms/hasVersion") ));
            identifier = new Identifier();
            identifier.setIdentifier(f.createLiteral(distribution.getJSONObject("http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier").getString("url")));
            identifier.setUri( f.createIRI(fdp.getString("baseUri") + "/distributionID/" + distribution.getString("http://purl.org/dc/terms/title")+"_"+distribution.getString("http://purl.org/dc/terms/hasVersion") ));
            distributionMetadata.setIdentifier(identifier);
            distributionMetadata.setVersion(f.createLiteral(distribution.getString("http://purl.org/dc/terms/hasVersion")) );
//          optional
            try{
                distributionMetadata.setLicense(f.createIRI(distribution.getString("http://purl.org/dc/terms/license")));
            }catch(Exception ex){}
            distributionMetadata.setUri( f.createIRI( "http://base/catalog/dataset/distribution"));
            distributionMetadata.setIssued(f.createLiteral(date));
            distributionMetadata.setModified(f.createLiteral(date));
//          optional
            try{
                distributionMetadata.setDescription(f.createLiteral(distribution.getString("http://purl.org/dc/terms/description")) );
            }catch(Exception ex){}
            distributionString = MetadataUtils.getString(distributionMetadata, RDFFormat.TURTLE).replaceAll("\\<" + distributionMetadata.getUri() + "\\>","<>");
//            
//            System.out.println(catalogString);
//            System.out.println(datasetString);
//            System.out.println(distributionString);
            String catalogPost = null;
            String datasetPost = null;
            if (!catalog.getBoolean("_exists")){
                catalogPost = IOUtils.toString(HttpUtils.post(fdp.getString("baseUri") + "/catalog?catalogID=" + catalog.getString("http://purl.org/dc/terms/title")+"_"+catalog.getString("http://purl.org/dc/terms/hasVersion"), 
                    catalogString).getContent(), "UTF-8");
            }
            if (!dataset.getBoolean("_exists")){
                datasetPost = IOUtils.toString(HttpUtils.post(fdp.getString("baseUri") + "/dataset?datasetID=" + dataset.getString("http://purl.org/dc/terms/title")+"_"+dataset.getString("http://purl.org/dc/terms/hasVersion"), datasetString).getContent(),"UTF-8");
            }
            String distributionPost = IOUtils.toString(HttpUtils.post(fdp.getString("baseUri") + "/distribution?distributionID=" +  distribution.getString("http://purl.org/dc/terms/title")+"_"+distribution.getString("http://purl.org/dc/terms/hasVersion"), distributionString).getContent(),"UTF-8");
            
            String data = new JSONObject(jb.toString()).getString("data");
            PushFairDataToResourceAdapter adapter = new PushFairDataToResourceAdapter();
            FtpResource r = new FtpResource(
                fdp.getString("ftpHost"), 
                fdp.getString("username"), 
                fdp.getString("password"), 
                fdp.getString("directory"),
                "FAIRdistribution_" + distribution.getString("http://purl.org/dc/terms/title")+"_"+distribution.getString("http://purl.org/dc/terms/hasVersion") + ".ttl");
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
