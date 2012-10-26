package com.google.refine.crowdsourcing.crowdflower;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.preference.PreferenceStore;

import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            
            String job_title = request.getParameter("title");
            String job_instructions = request.getParameter("instructions");
            String job_id = "";  //= "12345";

            String apiKey = (String) getPreference("crowdflower.apikey");
            System.out.println("---- API KEY: " + apiKey);
                        
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            System.out.println("Job title: " + job_title);
            System.out.println("Job instructions: " + job_instructions);
            
            
            
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey);
            String res = cf_client.createEmptyJob(job_title, job_instructions);
            JSONObject results;
            
            System.out.println("----------------------------------");
            System.out.println(res);
            System.out.println("----------------------------------");
            
            if(res!= null) {
                results = new JSONObject(res);
                System.out.println("-----> Job ID: " + results.get("id"));
                job_id = results.getString("id");
            }
            else
            {
                System.out.println("No job was created!");
                job_id = "ERROR";
            }
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            try {
                writer.object();
                writer.key("status"); writer.value("ok");
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
