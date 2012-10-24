package com.google.refine.crowdsourcing.crowdflower;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;

import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateNewJobCommand extends Command{
    static final Logger logger = LoggerFactory.getLogger("crowdflower_createnewjob");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            
            String job_title = request.getParameter("title");
            String job_instructions = request.getParameter("instructions");
            String job_id = "12345";
            
            //Project project = getProject(request);
            //project.columnModel.columns
            //create new request to CrowdFlower API
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            System.out.println("Job title: " + job_title);
            System.out.println("Job instructions: " + job_instructions);
            
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            try {
                writer.object();
                writer.key("status"); writer.value("ok");
                writer.key("job_id"); writer.value(job_id);
                //add data about created job, e.g. job_id!
    
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
