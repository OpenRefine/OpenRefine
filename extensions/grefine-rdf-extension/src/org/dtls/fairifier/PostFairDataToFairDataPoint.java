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
import org.openrdf.model.Statement;
import java.util.ArrayList;
import java.io.InputStream;
import nl.dtl.fairmetadata.io.*;
import nl.dtl.fairmetadata.model.*;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.model.URI;
import org.openrdf.model.ModelFactory;
import org.openrdf.model.vocabulary.*;
import java.util.List;
import org.openrdf.model.Model;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.sail.memory.model.CalendarMemLiteral;
import org.openrdf.model.impl.TreeModelFactory;
import org.openrdf.model.Namespace;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;

/**
 * 
 * @author Shamanou van Leeuwen
 * @date 7-11-2016
 *
 */

public class PostFairDataToFairDataPoint extends Command{
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try{
            JSONObject jsonObject = new JSONObject(req.getParameter("fdp"));
            JSONObject catalog = jsonObject.getJSONObject("catalog");
            JSONObject dataset = jsonObject.getJSONObject("dataset");
            JSONObject distribution = jsonObject.getJSONObject("distribution");
        
            URIImpl rootUri = new URIImpl(jsonObject.getString("baseUri"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        
            StringWriter writer = new StringWriter();
            TurtleWriter turtleWriter =  new TurtleWriter(writer);
            TreeModelFactory f = new TreeModelFactory();
            Model model = f.createEmptyModel();
            Namespace rdf = model.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            Namespace rdfs = model.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            Namespace dcat = model.setNamespace("dcat", "http://www.w3.org/ns/dcat#");
            Namespace xsd = model.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
            Namespace owl = model.setNamespace("owl", "http://www.w3.org/2002/07/owl#");
            Namespace dcterms = model.setNamespace("dcterms", "http://purl.org/dc/terms/");
            Namespace ldp = model.setNamespace("ldp", "http://www.w3.org/ns/ldp#");
            Namespace lang = model.setNamespace("lang", "http://id.loc.gov/vocabulary/iso639-1/");
            
        
            GregorianCalendar gregory = new GregorianCalendar();
            gregory.setTime(new Date());
            XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregory);

            model.add(rootUri, 
                    new URIImpl("http://purl.org/dc/terms/title"), 
                    new LiteralImpl(catalog.getString("_title")) );
            model.add(rootUri, 
                    new URIImpl("http://www.w3.org/2000/01/rdf-schema#label"), 
                    new LiteralImpl(catalog.getString("_label")) );
            model.add(rootUri, 
                    new URIImpl("http://purl.org/dc/terms/identifier"), 
                    new LiteralImpl(catalog.getString("_identifier")) );
            model.add(rootUri, 
                    new URIImpl("http://purl.org/dc/terms/hasVersion"), 
                    new LiteralImpl(catalog.getString("_version")) );
            model.add(rootUri, 
                    new URIImpl("http://purl.org/dc/terms/issued"), 
                    new CalendarMemLiteral("dtls fairifier" ,calendar ));
            model.add(rootUri, 
                    new URIImpl("http://purl.org/dc/terms/modified"), 
                    new CalendarMemLiteral("dtls fairifier" ,calendar ));
            model.add(rootUri, 
                    new URIImpl("http://purl.org/dc/terms/language"), 
                    new URIImpl(catalog.getString("_language")) );
            model.add(rootUri, 
                    new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                    new URIImpl("http://www.w3.org/ns/dcat#Catalog") );
            model.add(rootUri, 
                    new URIImpl("http://www.w3.org/ns/dcat#themeTaxonomy"), 
                    new URIImpl(catalog.getString("_theme")));
            model.add(rootUri, 
                    new URIImpl("http://www.w3.org/ns/dcat#dataset"), 
                    new URIImpl( jsonObject.getString("baseUri") + "/" + catalog.getString("_identifier") + "/" + dataset.getString("_identifier") ));
            turtleWriter.startRDF();
            for (Statement s : model){
                turtleWriter.handleStatement(s);
            }
            turtleWriter.endRDF();
        
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter jsonWriter = new JSONWriter(res.getWriter());
            jsonWriter.object();
            jsonWriter.key("code");
            jsonWriter.value("ok");
            jsonWriter.key("res");
            jsonWriter.value(writer.toString());
            jsonWriter.endObject();
        }catch(Exception ex){
            try{
                res.setCharacterEncoding("UTF-8");
                res.setHeader("Content-Type", "application/json");
                JSONWriter jsonWriter = new JSONWriter(res.getWriter());
                jsonWriter.object();
                jsonWriter.key("code");
                jsonWriter.value("error");
                jsonWriter.key("res");
                jsonWriter.value(ex.toString());
                jsonWriter.endObject();
            }catch(Exception e){
                
            }
        }
    }
}