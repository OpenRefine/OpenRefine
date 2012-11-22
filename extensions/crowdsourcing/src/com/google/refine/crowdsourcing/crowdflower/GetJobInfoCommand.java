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
            String status = "";
            String title = null;
            String cml = null;
            String instructions = null;
            String message = "";
            ArrayList<String> fields = new ArrayList<String>();
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);

            
            try {

                //copy job, store id
                if(extension.has("job_id") && !extension.isNull("job_id")) {
                    
                            
                    result = cf_client.getJob(extension.getString("job_id"));          
                    JSONObject res = ParsingUtilities.evaluateJsonStringToObject(result);
          
                    status = res.getString("status");                
                    JSONObject obj = res.getJSONObject("response");
                    
                    if(obj.has("cml") && !obj.isNull("cml")) {
                        cml = obj.getString("cml");
                        fields = CrowdsourcingUtil.parseCmlFields(obj.getString("cml"));
                        System.out.println("CML fields!");
                    }
                    
                    if(obj.has("title") && !obj.isNull("title")) {
                        title = obj.getString("title");
                    }
                    
                    if(obj.has("instructions") && !obj.isNull("instructions")) {
                        instructions = obj.getString("instructions");
                    }
    
                } else {
                    status = "ERROR";
                    message = "No job id was provided!";
                    throw new Exception();
                }
            
            
                writer.object();
                writer.key("status"); writer.value(status);
                
                writer.key("title"); writer.value(title);
                writer.key("instructions"); writer.value(instructions);
                writer.key("cml"); writer.value(cml);
                
                
                writer.key("fields").array();
                
                for(int i= 0; i < fields.size(); i++) {
                    writer.value(fields.get(i));
                }
                writer.endArray();
                
            } catch(Exception e){
                logger.error("Cannot get job information.");
                writer.key("status").value(status);
                writer.key("message").value(message);
            }
            
            finally {
                writer.endObject();
                w.flush();
                w.close();
            }

            
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        
    }

}
