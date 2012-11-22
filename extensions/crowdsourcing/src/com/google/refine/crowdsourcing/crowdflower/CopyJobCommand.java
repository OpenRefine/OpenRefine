package com.google.refine.crowdsourcing.crowdflower;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;

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


public class CopyJobCommand extends Command{
    static final Logger logger = LoggerFactory.getLogger("crowdflower_copyjob");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            
            
            String jsonString = request.getParameter("extension");
                        
            JSONObject extension = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            String apiKey = (String) CrowdsourcingUtil.getPreference("crowdflower.apikey");                       
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey);
            String newJobId = "";
            String result = "";
            String status = "";
            String message = "";
            LinkedHashMap<String, String> params = null;
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            try {

            //copy job, store id
            if(extension.has("job_id") && !extension.isNull("job_id")) {
                
                System.out.println("JOB ID: " + extension.getString("job_id"));
                
                if(extension.has("all_units") || extension.has("gold")) {
                    params = new LinkedHashMap<String, String>();
                }
                
                if(extension.has("all_units")) {
                    params.put("all_units", extension.getString("all_units"));
                }
                
                if(extension.has("gold")) {
                    params.put("gold", extension.getString("gold"));
                }
                
                if(params != null && params.size() > 0) {
                    result = cf_client.copyJob(extension.getString("job_id"), params);
                } else {
                    result = cf_client.copyJob(extension.getString("job_id"));
                }
                
                JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(result);
                status = obj.getString("status");
                
                JSONObject res = obj.getJSONObject("response"); 
            
                if(res.has("id") && !res.isNull("id")) {
                    newJobId = res.getString("id");
                }
                else {
                    message = res.getJSONObject("error").getString("message");
                    throw new Exception();
                    
                }
            } else {
                message = "Cannot obtain job id: no job was selected.";
                status = "ERROR";
                
                writer.object();
               throw new Exception(message); //TODO: refactor this code!!!
            }
            
            //get updated list of jobs, return new job id
            result = cf_client.getAllJobs();
            JSONObject res1 = ParsingUtilities.evaluateJsonStringToObject(result);
            
            JSONArray jobs_updated = null;
            
            if(res1.has("response")) {
                jobs_updated = res1.getJSONArray("response");
            } else {

                status = res1.getString("status");
                message = res1.getString("message");

                writer.object();                         
                throw new Exception(message);
            }
            
            
                writer.object();
                writer.key("status"); writer.value(status);
                writer.key("job_id"); writer.value(newJobId);
                writer.key("jobs").array();
                
                if(jobs_updated!= null) {
                
                    for(int i=0; i < jobs_updated.length(); i++) {
                        JSONObject current = jobs_updated.getJSONObject(i);
                        writer.object();
                        writer.key("id").value(current.get("id"));
                        writer.key("title");
                        if(current.get("title") != null) {
                            writer.value(current.get("title"));
                        }
                        else {
                            writer.value("No title entered yet");
                        }
    
                        writer.endObject();
                    }
                }
                writer.endArray();
                
            } catch(Exception e){
                logger.error("New job not created.");
                writer.key("status").value(status);
                writer.key("message").value(message);
            }
            finally {
                writer.endObject();
                w.flush();
                w.close();
            }

            
        } catch (JSONException e) {
            e.printStackTrace();
        } 
        
    }

}
