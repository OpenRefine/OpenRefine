package com.metaweb.gridworks.commands.project;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.MetaParser.LanguageInfo;
import com.metaweb.gridworks.model.OverlayModel;
import com.metaweb.gridworks.model.Project;

public class GetModelsCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Project project = getProject(request);
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Properties options = new Properties();
            JSONWriter writer = new JSONWriter(response.getWriter());
            
            writer.object();
            writer.key("columnModel"); project.columnModel.write(writer, options);
            writer.key("recordModel"); project.recordModel.write(writer, options);
            
            writer.key("overlayModels"); writer.object();
            for (String modelName : project.overlayModels.keySet()) {
                OverlayModel overlayModel = project.overlayModels.get(modelName);
                if (overlayModel != null) {
                    writer.key(modelName);
                    
                    project.overlayModels.get(modelName).write(writer, options);
                }
            }
            writer.endObject();
            
            writer.key("scripting"); writer.object();
            for (String languagePrefix : MetaParser.getLanguagePrefixes()) {
                LanguageInfo info = MetaParser.getLanguageInfo(languagePrefix);
                
                writer.key(languagePrefix);
                writer.object();
                    writer.key("name"); writer.value(info.name);
                    writer.key("defaultExpression"); writer.value(info.defaultExpression);
                writer.endObject();
            }
            writer.endObject();
            
            writer.endObject();
        } catch (JSONException e) {
            respondException(response, e);
        }
    }

}
