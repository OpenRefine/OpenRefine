package com.google.refine.crowdsourcing.crowdflower;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.crowdsourcing.CrowdsourcingUtil;
import com.google.refine.util.ParsingUtilities;
import com.zemanta.crowdflower.client.CrowdFlowerClient;


public class GetJobInfoCommand extends Command{
    static final Logger logger = LoggerFactory.getLogger("crowdflower_getjobinfo");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            
            
            String jsonString = request.getParameter("extension");
                        
            JSONObject extension = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            String apiKey = (String) CrowdsourcingUtil.getPreference("crowdflower.apikey");                       
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey);
            String result = "";
            
            response.setCharacterEncoding("UTF-8");
            
            //copy job, store id
            if(extension.has("job_id") && !extension.isNull("job_id")) {
                
                        
                result = cf_client.getJob(extension.getString("job_id"));                   
                JSONObject res = ParsingUtilities.evaluateJsonStringToObject(result);
      
                if(res.getString("status").equals("ERROR"))
                {
                    generateErrorResponse(response, res);
                }
                     
                JSONObject obj = res.getJSONObject("response");
                obj.put("status", "OK"); //TODO: return additional message form API if there is any
                generateResponse(response, obj);
               
            } else {
                
                JSONObject err = new JSONObject();
                err.put("status", "ERROR");
                err.put("message", "Job id was not provided. could not obtain job information.");
                    
                    generateErrorResponse(response, err);
                }
            
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        
    }


    
    private void generateResponse(HttpServletResponse response, JSONObject data)
            throws IOException, JSONException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            writer.key("status"); writer.value(data.getString("status"));
            
            writer.key("title"); writer.value(data.getString("title"));
            writer.key("instructions"); writer.value(data.getString("instructions"));

            String cml = data.getString("cml");
            ArrayList<String> fields = new ArrayList<String>();
            fields = CrowdsourcingUtil.parseCmlFields(data.getString("cml"));
            
            writer.key("cml"); writer.value(cml);
            writer.key("fields").array();
            
            for(int i= 0; i < fields.size(); i++) {
                writer.value(fields.get(i));
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
        
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            
            if(data.has("error")) {
                writer.key("message"); writer.value(data.getJSONObject("error").getString("message")); 
            } else {
                writer.key("message"); writer.value(data.get("message")); 
            }
            
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
