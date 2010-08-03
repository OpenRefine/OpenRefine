package com.google.gridworks.rdf.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.gridworks.commands.Command;
import com.google.gridworks.rdf.vocab.Vocabulary;
import com.google.gridworks.rdf.vocab.VocabularyManager;

public class ListVocabulariesCommand extends Command{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        try{
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("vocabularies");
            writer.array();
            Properties p = new Properties();
            for(Vocabulary v:VocabularyManager.getSingleton(servlet).getVocabularies()){
                v.write(writer, p);
            }
            writer.endArray();
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
