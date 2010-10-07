package com.google.refine.commands.expr;

import java.io.IOException;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.grel.Control;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class GetExpressionLanguageInfoCommand extends Command {
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            Properties options = new Properties();
            
            writer.object();
            
            writer.key("functions");
            writer.object();
            {
                for (Entry<String, Function> entry : ControlFunctionRegistry.getFunctionMapping()) {
                    writer.key(entry.getKey());
                    entry.getValue().write(writer, options);
                }
            }
            writer.endObject();
            
            writer.key("controls");
            writer.object();
            {
                for (Entry<String, Control> entry : ControlFunctionRegistry.getControlMapping()) {
                    writer.key(entry.getKey());
                    entry.getValue().write(writer, options);
                }
            }
            writer.endObject();
            
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
