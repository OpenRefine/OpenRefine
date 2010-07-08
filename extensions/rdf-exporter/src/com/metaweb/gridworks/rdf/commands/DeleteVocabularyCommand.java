package com.metaweb.gridworks.rdf.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.rdf.vocab.VocabularyManager;

public class DeleteVocabularyCommand extends Command{

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getParameter("uri");
        try {    
            VocabularyManager.getSingleton(servlet).deleteVocabulary(uri);
            respondJSON(response, new Jsonizable() {
                
                @Override
                public void write(JSONWriter writer, Properties options)
                        throws JSONException {
                    writer.object();
                    writer.key("code"); writer.value("ok");
                    writer.endObject();
                }
            });
        } catch (JSONException e) {
            respondException(response, e);
        } catch (Exception e){
            respondException(response, e);
        }
    }

}
