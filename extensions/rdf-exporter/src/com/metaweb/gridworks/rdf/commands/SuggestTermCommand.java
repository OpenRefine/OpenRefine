package com.metaweb.gridworks.rdf.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.rdf.vocab.RDFNode;
import com.metaweb.gridworks.rdf.vocab.VocabularyManager;

public class SuggestTermCommand extends Command{

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        
        response.setHeader("Content-Type", "application/json");
        JSONWriter writer = new JSONWriter(response.getWriter());
        
        String type = request.getParameter("type_strict");
        
        String prefix = request.getParameter("prefix");
        try{
            writer.object();
            
            writer.key("prefix");
            writer.value(prefix);
            
            writer.key("result");
            writer.array();
            List<RDFNode> nodes;
            if(type!=null && type.trim().equals("property")){
                nodes = VocabularyManager.getSingleton(servlet).searchProperties(prefix);
            }else{
                nodes = VocabularyManager.getSingleton(servlet).searchClasses(prefix);
            }
            for(RDFNode c:nodes){
                c.writeAsSearchResult(writer);
            }
            writer.endArray();
            writer.endObject();
        }catch(Exception e){
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    
}

class Result implements Jsonizable{

    private List<String[]> results = new ArrayList<String[]>();
    private String prefix;
    
    Result(String p){
        this.prefix = p;
    }
    void addResult(String id, String name){
        String[] res = new String[] {id,name};
        results.add(res);
    }
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        
        
        writer.key("prefix");
        writer.value(prefix);
        
        writer.key("result");
        writer.array();
        for(String[] res:results){
            writer.object();
            
            writer.key("id");
            writer.value(res[0]);
            
            writer.key("name");
            writer.value(res[1]);
            
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }
    
}
