package org.snaccooperative.commands;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
// import com.google.refine.commands.Command;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
//import com.google.refine.util.ParsingUtilities;

//import com.google.refine.model.Project;
//import com.google.refine.browsing.Engine;

public class SNACPreviewSchemaCommand extends Command  {

   // @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // try{
            //Project project = getProject(request);
            //Engine engine = getEngine(request,project);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
        // }
        // catch (Exception e){
        //     respondException(response, e);
        // }
        
    }

    // @Override
    // public void doGet(HttpServletRequest request, HttpServletResponse response)
    //         throws ServletException, IOException {
    //     doPost(request, response);
    // }
}
