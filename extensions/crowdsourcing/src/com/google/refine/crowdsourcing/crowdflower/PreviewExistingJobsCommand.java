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
import com.google.refine.util.JSONUtilities;

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
 
                Writer w = response.getWriter();
                JSONWriter writer = new JSONWriter(w);

                try {
                
                    CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey);
                    //TODO: more like a hack
                    cf_client.setTimeout(2000); //more time needed than default
                    String response_msg = "";
                    
                    response_msg = cf_client.getAllJobs();
                    JSONObject obj = new JSONObject(response_msg);
                    
                    writer.object();
                    writer.key("status"); writer.value(obj.getString("status"));
                
                    if(obj != null && obj.has("response") && obj.get("response") != null) {
                    
                      writer.key("jobs").array();
                      
                      JSONArray obj2 = obj.getJSONArray("response");
                      System.out.println("Response: " + JSONUtilities.toStringList(obj2));

                    
                        for (int i=0; i < obj2.length(); i++) {
                            JSONObject current = obj2.getJSONObject(i);
                            System.out.println("Current.id: " + current.get("id"));
                            
                            writer.object();
                            writer.key("id").value(current.get("id"));                       
                            writer.key("title").value(current.get("title"));
                            writer.endObject();

                            System.out.println("current.title: " + current.get("title"));
                            
                         }
                         writer.endArray();
                    } else {
                        writer.key("message"); writer.value(obj.getString("message"));
                    }
                } catch(JSONException e) {
                    writer.key("status"); writer.value("ERROR");
                    writer.key("jobs"); writer.array();writer.endArray();
                    System.out.println("Job was not created.");
                }
                finally {
                    writer.endObject();                    
                    w.flush();
                    w.close();
                }
            
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage(),e);
        }        
    }
    
    

}
