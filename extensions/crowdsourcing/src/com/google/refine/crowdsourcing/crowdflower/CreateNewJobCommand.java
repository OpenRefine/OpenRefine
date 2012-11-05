package com.google.refine.crowdsourcing.crowdflower;

//TODO: does it make sense to implement an importer to import data *from* CF?

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.preference.PreferenceStore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zemanta.crowdflower.client.CF_DataType;
import com.zemanta.crowdflower.client.CrowdFlowerClient;

public class CreateNewJobCommand extends Command{
    static final Logger logger = LoggerFactory.getLogger("crowdflower_createnewjob");

    private Object getPreference(String prefName) {
        PreferenceStore ps = ProjectManager.singleton.getPreferenceStore();
        Object pref = ps.get(prefName);
        
        return pref;
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            
            //TODO: store this data into JSON extension
            //TODO: check if extension not null!
            
            JSONObject extension = new JSONObject(request.getParameter("extension")); 
            String job_id = "";  //= "12345";
            String status = "ERROR";

            String apiKey = (String) getPreference("crowdflower.apikey");
            System.out.println("---- API KEY: " + apiKey);
                       
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey);
            JSONObject results;
            String message = "";
            
            String job_title = extension.getString("title");
            String job_instructions = extension.getString("instructions");

            
            //user wants to upload data
            if(extension.has("column_indices")) {
                
                CF_DataType content_type;
                
                if(extension.getBoolean("upload")) {
                    
                    //upload first few lines (one row) to create data
                    //rest of data should be uploaded in an operation! <- does this make sense?
                    //get data for selected columns and selected rows!
                    JSONArray cols = extension.getJSONArray("column_indices");                    
                    
                    if(extension.getString("content_type").equals("csv")) {
                        content_type = CF_DataType.SPREADSHEET_CSV;
                    } else {
                        content_type = CF_DataType.JSON;
                    }
                    
                    //TODO:  get data for the first row
                    JSONObject data = new JSONObject();
                    
                    message = "OK!";//cf_client.bulkUploadToNewJob(job_title, job_instructions, content_type, data.toString());

                    
                    
                    return;
                }
                else {
                    
                    //how do I create structure without data?
                }
                
                               
                System.out.println(message);
            }
            else {
                message = cf_client.createEmptyJob(job_title, job_instructions);   
                System.out.println(message);            
            }
            
            if(message!= null) {
                results = new JSONObject(message);
                System.out.println("-----> Job ID: " + results.get("id"));
                job_id = results.getString("id");
            }
            else
            {
                status = "ERROR";
                results = null;
                System.out.println("No job was created!");
                job_id = "ERROR";
            }
            
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            try {
                writer.object();
                writer.key("status"); writer.value(status);
                writer.key("job_id"); writer.value(job_id);    
            } catch(Exception e){
                logger.error("New job not created.");
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
