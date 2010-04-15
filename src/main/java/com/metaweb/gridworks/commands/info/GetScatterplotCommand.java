package com.metaweb.gridworks.commands.info;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.charting.ScatterplotCharter;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;

public class GetScatterplotCommand extends Command {

    final private ScatterplotCharter charter = new ScatterplotCharter();
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            //long start = System.currentTimeMillis();
            
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            JSONObject conf = getJsonParameter(request,"plotter");
            
            response.setHeader("Content-Type", charter.getContentType());
            
            ServletOutputStream sos = null;
            
            try {
                sos = response.getOutputStream();
                charter.draw(sos, project, engine, conf);
            } finally {
                sos.close();
            }
            
            //Gridworks.log("Drawn scatterplot in " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }
}
