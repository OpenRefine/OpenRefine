package com.google.refine.crowdsourcing.crowdflower;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
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
            
            String apiKey = (String) CrowdsourcingUtil.getPreference("crowdflower.apikey");                       
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
 
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey);
            //TODO: more like a hack
            cf_client.setTimeout(2000); //more time needed than default
            String response_msg = cf_client.getAllJobs();
            JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(response_msg);
            
            System.out.println("Preview result: " + obj.toString());
            
            System.out.println("obj.getString(status): " + obj.getString("status"));
            
            if(obj.getString("status").equals("ERROR")) {
                generateErrorResponse(response, obj);
            } else
            {
                generateResponse(response, obj);
            }
            
            
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage(),e);
        }        
    }
    
    
    private void generateResponse(HttpServletResponse response, JSONObject data)
            throws IOException, JSONException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("jobs");
            writer.array();
            JSONArray jobs = data.getJSONArray("response");
            
            for(int i=0; i < jobs.length(); i++) {
                JSONObject current = jobs.getJSONObject(i);
                writer.object();
                writer.key("id").value(current.get("id"));                       
                writer.key("title").value(current.get("title"));
                writer.endObject();
            }
            writer.endArray();
        } catch(Exception e){
            logger.error("Generating response failed.");
        }
        finally {
            writer.endObject();
            w.flush();
            w.close();
        }
    }
    
    private void generateErrorResponse(HttpServletResponse response, JSONObject data)
            throws IOException, JSONException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        
        System.out.println("Data in error response: " + data);
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("message"); writer.value(data.getJSONObject("error").get("message"));
        } catch(Exception e){
            logger.error("Generating ERROR response failed.");
        }
        finally {
            writer.endObject();
            w.flush();
            w.close();
        }
    }


}
