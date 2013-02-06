package com.google.refine.crowdsourcing.crowdflower;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.crowdsourcing.CrowdsourcingUtil;
import com.google.refine.util.ParsingUtilities;

import com.zemanta.crowdflower.client.CrowdFlowerClient;


public class PreviewExistingJobsCommand extends Command {
    static final Logger logger = LoggerFactory.getLogger("crowdflower_getjobspreview");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            
            response.setHeader("Content-Type", "application/json");
            response.setCharacterEncoding("UTF-8");
 
            String apiKey = (String) CrowdsourcingUtil.getPreference("crowdflower.apikey");                      
            
            if(apiKey == null || apiKey.equals("")) {
                generateErrorResponse(response,"No valid CrowdFlower API key found. Check your settings.");
            }
            else {
                       
                Object defTimeout = CrowdsourcingUtil.getPreference("crowdflower.defaultTimeout");
                String defaultTimeout = (defTimeout != null) ? (String)defTimeout : "1500";
                
                CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey, Integer.valueOf(defaultTimeout));
                String response_msg = cf_client.getAllJobs();
            
                JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(response_msg);
             
                if(obj.getString("status").equals("ERROR")) {
                    generateErrorResponse(response, obj);
                } else
                {
                    generateResponse(response, obj);
                }
            }
            
        } catch(IOException e) {
            generateErrorResponse(response, "Check your connection. Error message: \n" + e.getLocalizedMessage());
        } 
        catch(Exception e) {
            logger.error(e.getLocalizedMessage(),e);
            respondException(response, e);
        }        
    }
    
    
    private void generateResponse(HttpServletResponse response, JSONObject data)
            throws IOException, ServletException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);  
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("jobs");
            
            //not necessarily an array
            //TODO: check whether this is causing trouble
            JSONArray jobs = data.getJSONArray("response");

            if(jobs.length() > 0) {
                writer.array();
            
                for(int i=0; i < jobs.length(); i++) {
                    JSONObject current = jobs.getJSONObject(i);
                    writer.object();
                    writer.key("id").value(current.get("id"));                       
                    writer.key("title").value(current.get("title"));
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();
                w.flush();
                w.close();
            }
        } catch(Exception e){
            logger.error("Generating response failed.");
            respondException(response,e);
        }
    }
    
    private void generateErrorResponse(HttpServletResponse response, String message) 
            throws IOException, ServletException {

        Writer w = null;
        JSONWriter writer = null;
        
        try {
            w = response.getWriter();
            writer = new JSONWriter(w);

            writer.object();
            writer.key("status"); writer.value("ERROR");
            writer.key("message"); writer.value(message);
            writer.endObject();
            w.flush();
            w.close();
        } catch(Exception e){
            logger.error("Generating ERROR response failed.");
            respondException(response,e);
        }    
     }
    
    private void generateErrorResponse(HttpServletResponse response, JSONObject data)
            throws IOException, ServletException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("message"); writer.value(data.getJSONObject("error").get("message"));
            writer.endObject();
            w.flush();
            w.close();
        } catch(Exception e){
            logger.error("Generating ERROR response failed.");
            respondException(response, e);
        }
    }


}
