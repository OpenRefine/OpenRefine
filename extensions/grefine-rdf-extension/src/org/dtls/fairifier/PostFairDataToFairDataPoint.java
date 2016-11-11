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
import org.openrdf.sail.memory.model.CalendarMemLiteral;
import org.openrdf.model.URI;
import org.openrdf.model.Literal;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;
import nl.dtl.fairmetadata.utils.*;
import org.openrdf.rio.RDFFormat;
import javax.xml.datatype.DatatypeConfigurationException; 
import org.apache.commons.io.IOUtils;
import java.util.ArrayList; 
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * 
 * @author Shamanou van Leeuwen
 * @date 7-11-2016
 *
 */

public class PostFairDataToFairDataPoint extends Command{
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ArrayList<URI> datasetUris = new ArrayList<URI>();
        ArrayList<URI> distributionUris = new ArrayList<URI>();
        ArrayList<URI> catalogThemes = new ArrayList<URI>();
        ArrayList<URI> datasetThemes = new ArrayList<URI>();
        ArrayList<Literal> keyWords = new ArrayList<Literal>();
        
        String catalogString;
        String datasetString;
        String distributionString;
        
        CatalogMetadata catalogMetadata = new CatalogMetadata();
        DatasetMetadata datasetMetadata = new DatasetMetadata();
        DistributionMetadata distributionMetadata = new DistributionMetadata();
        
        try{
            JSONObject jsonObject = new JSONObject(req.getParameter("fdp"));
            JSONObject catalog = jsonObject.getJSONObject("catalog");
            JSONObject dataset = jsonObject.getJSONObject("dataset");
            JSONObject distribution = jsonObject.getJSONObject("distribution");
            
            catalogMetadata.setHomepage(new URIImpl(catalog.getString("_homepage")));
            catalogThemes.add(new URIImpl(catalog.getString("_theme")));
            catalogMetadata.setThemeTaxonomy(catalogThemes);
            catalogMetadata.setTitle(new LiteralImpl(catalog.getString("_title")));
            catalogMetadata.setIdentifier(new LiteralImpl(catalog.getString("_identifier")));
            
            catalogMetadata.setIssued(RDFUtils.getCurrentTime());
            catalogMetadata.setModified(RDFUtils.getCurrentTime());
            catalogMetadata.setVersion(new LiteralImpl(catalog.getString("_version")));
            catalogMetadata.setUri(new URIImpl(jsonObject.getString("baseUri")));
            
            datasetUris.add( new URIImpl( jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + dataset.getString("_identifier") ));
            
            catalogMetadata.setDatasets(datasetUris);
            
            datasetMetadata.setLandingPage(new URIImpl(dataset.getString("_landingpage")));
            datasetThemes.add(new URIImpl(dataset.getString("_theme")));
            datasetMetadata.setThemes(datasetThemes);
            datasetMetadata.setContactPoint(new URIImpl(dataset.getString("_creator")));
            String[] keywordArray = dataset.getString("_keyword").split(",");
            for (String keyword : keywordArray){
                keyWords.add( new LiteralImpl(keyword) );
            }
            datasetMetadata.setKeywords(keyWords);
            
            distributionUris.add( new URIImpl( jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + dataset.getString("_identifier") + "/" +  distribution.getString("_identifier") ));
            
            datasetMetadata.setDistribution(distributionUris);
            datasetMetadata.setTitle(new LiteralImpl(dataset.getString("_title")));
            datasetMetadata.setIdentifier(new LiteralImpl( dataset.getString("_identifier")));
            datasetMetadata.setIssued( RDFUtils.getCurrentTime() );
            datasetMetadata.setModified( RDFUtils.getCurrentTime() );
            datasetMetadata.setVersion( new LiteralImpl(dataset.getString("_version")) );
            datasetMetadata.setDescription( new LiteralImpl(dataset.getString("_description")) );
            datasetMetadata.setUri( new URIImpl( jsonObject.getString("baseUri") + "/" + dataset.getString("_identifier") + "/" ) );
            
            distributionMetadata.setAccessURL( new URIImpl(distribution.getString("_accessUrl")) );
            distributionMetadata.setMediaType( new LiteralImpl(distribution.getString("_mediatype")) );
            distributionMetadata.setTitle( new LiteralImpl(distribution.getString("_title")) );
            distributionMetadata.setIdentifier( new LiteralImpl(distribution.getString("_identifier")) );
            distributionMetadata.setVersion( new LiteralImpl(distribution.getString("_version")) );
            distributionMetadata.setLicense(new URIImpl(distribution.getString("_license")));
            distributionMetadata.setUri( new URIImpl( jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" +  dataset.getString("_identifier") + "/" + distribution.getString("_identifier") ));

            catalogString = MetadataUtils.getString(catalogMetadata, RDFFormat.TURTLE).replaceAll("\\<" + catalogMetadata.getUri() + "\\>","<>");
            datasetString = MetadataUtils.getString(datasetMetadata, RDFFormat.TURTLE).replaceAll("\\<" + datasetMetadata.getUri() + "\\>","<>");
            distributionString = MetadataUtils.getString(distributionMetadata, RDFFormat.TURTLE).replaceAll("\\<" + distributionMetadata.getUri() + "\\>","<>");
            
            String catalogPost = IOUtils.toString(HttpUtils.post(jsonObject.getString("baseUri") + "?catalogID=" + catalog.getString("_identifier"), catalogString).getContent(), "UTF-8");
            String datasetPost = IOUtils.toString(HttpUtils.post(jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "?datasetID=" + dataset.getString("_identifier"), datasetString).getContent(),"UTF-8");
            String distributionPost = IOUtils.toString(HttpUtils.post(jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" +  dataset.getString("_identifier") + "?distributionID=" + distribution.getString("_identifier"), distributionString).getContent(),"UTF-8");
            
            
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