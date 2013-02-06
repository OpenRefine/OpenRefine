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
            Object defTimeout = CrowdsourcingUtil.getPreference("crowdflower.defaultTimeout");
            String defaultTimeout = (defTimeout != null) ? (String)defTimeout : "1500";
            
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey, Integer.valueOf(defaultTimeout));
                     
            response.setCharacterEncoding("UTF-8");
            
            JSONObject result = new JSONObject(); 
            
            result = copyJobAndReturnID(extension, cf_client);
            
            if(result.has("status") && !result.isNull("status") 
                    && result.getString("status").equals("ERROR")) {
                generateErrorResponse(response, result);
            } 
            else {
            
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    logger.info("Sleeping interrupted");
                }
                JSONObject obj = getUpdatedJobList(cf_client);
                
                if(obj.has("status") && obj.getString("status").equals("ERROR"))  {
                    obj.put("source", "[updating job list]");
                    generateErrorResponse(response, obj);
                } 
                else {
                
                    logger.info("Job list updated.");
                    
                    if(obj.has("jobs") && !obj.isNull("jobs")) {
                        result.put("jobs", obj.getJSONArray("jobs"));
                    }
                    
                    result.put("status", "OK");
                    generateResponse(response, result);
                }
            }
 
            
        } catch (Exception e) {
            //e.printStackTrace();
            respondException(response, e);
        } 
        
    }

    private JSONObject getUpdatedJobList(CrowdFlowerClient cf_client) throws JSONException {

        JSONObject result = new JSONObject();
        
        String res_msg  = cf_client.getAllJobs();
        JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(res_msg);
        
        JSONArray jobs_updated = null;
        
        if(obj.has("status") && obj.getString("status").equals("ERROR")) {
            return obj;
        }
        
        if(obj.has("response")) {
            jobs_updated = obj.getJSONArray("response");
            result.put("jobs", jobs_updated);
        } 
            
        return result;
    
    }

    private JSONObject copyJobAndReturnID(JSONObject extension, CrowdFlowerClient cf_client)
            throws JSONException {

        LinkedHashMap<String, String> params = null;
        JSONObject result = new JSONObject();
        
        String res_msg;

        if(extension.has("job_id") && !extension.isNull("job_id")) {
            
            if(extension.has("all_units") || extension.has("gold")) {
                params = new LinkedHashMap<String, String>();
            }
            
            if(extension.has("all_units")) {
                params.put("all_units", extension.getString("all_units"));
            }
            else if(extension.has("gold")) {
                params.put("gold", extension.getString("gold"));
            }
            
            if(params != null && params.size() > 0) {
                res_msg = cf_client.copyJob(extension.getString("job_id"), params);
            } else {
                res_msg = cf_client.copyJob(extension.getString("job_id"));
            }
            
            JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(res_msg);
            
            if(obj.getString("status").equals("ERROR")) {
                result = obj;
            }
            else {
                JSONObject res = obj.getJSONObject("response"); 
                if(res.has("id") && !res.isNull("id")) {
                    result.put("job_id",res.getString("id"));
                }
                else {
                    result = res; //contains error message
                }
            }
            
        } else {            
            result.put("status", "ERROR");
            result.put("message", "Cannot obtain job id: no job was selected.");
        }
        
        return result;
    }
    
    private void generateResponse(HttpServletResponse response, JSONObject data)
            throws IOException, JSONException {

        
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        try {
           
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("job_id"); writer.value(data.get("job_id"));

            if(data.has("jobs") && !data.isNull("jobs")) {
            
                writer.key("jobs"); 
                writer.array();
                JSONArray jobs_updated = data.getJSONArray("jobs");
                
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
                writer.endArray();
        }

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
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("message");
            
            String error_msg = "";
            
            if(data.has("error")) {
                error_msg = data.getJSONObject("error").getString("message");
            } else {
                error_msg = data.getString("message");
            }
            
            if(data.has("source")) {
                error_msg += " -- error source: " + data.getString("source");
            }
            
            writer.value(error_msg);
            
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
