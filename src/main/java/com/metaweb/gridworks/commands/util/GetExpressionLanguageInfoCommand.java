package com.metaweb.gridworks.commands.util;

import java.io.IOException;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.expr.Control;
import com.metaweb.gridworks.expr.Function;
import com.metaweb.gridworks.expr.Parser;

public class GetExpressionLanguageInfoCommand extends Command {
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			JSONWriter writer = new JSONWriter(response.getWriter());
			Properties options = new Properties();
			
			writer.object();
			
			writer.key("functions");
			writer.object();
			{
			    for (Entry<String, Function> entry : Parser.functionTable.entrySet()) {
			        writer.key(entry.getKey());
			        entry.getValue().write(writer, options);
			    }
			}
			writer.endObject();
			
            writer.key("controls");
            writer.object();
            {
                for (Entry<String, Control> entry : Parser.controlTable.entrySet()) {
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
