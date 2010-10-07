package com.google.refine.rdf.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.ProjectManager;
import com.google.refine.model.Project;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.SearchResultItem;

public class SuggestTermCommand extends RdfCommand{

	private static Pattern qnamePattern = Pattern.compile("^[_a-zA-Z][-._a-zA-Z0-9]*:([_a-zA-Z][-._a-zA-Z0-9]*)?");
	
	public SuggestTermCommand(ApplicationContext ctxt) {
		super(ctxt);
	}
	
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	//type will hold the project Id. parameters names are defined by the JavaScript library. 
        String projectId = request.getParameter("type");
        
        response.setHeader("Content-Type", "application/json");
        JSONWriter writer = new JSONWriter(response.getWriter());
        
        String type = request.getParameter("type_strict");
        
        String query = request.getParameter("prefix");
        try{
            writer.object();
            
            writer.key("prefix");
            writer.value(query);
            
            writer.key("result");
            writer.array();
            List<SearchResultItem> nodes;
            if(type!=null && type.trim().equals("property")){
                nodes = getRdfContext().getVocabularySearcher().searchProperties(query,projectId);
            }else{
                nodes = getRdfContext().getVocabularySearcher().searchClasses(query,projectId);
            }
            
            if(nodes.size()==0){
            	RdfSchema schema = Util.getProjectSchema(getRdfContext(),getProject(request));
            	nodes = search(schema,query);
            }
            for(SearchResultItem c:nodes){
                c.writeAsSearchResult(writer);
            }
            writer.endArray();
            writer.endObject();
        }catch(Exception e){
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    
    

	@Override
	protected Project getProject(HttpServletRequest request)
			throws ServletException {
    	String projectId = request.getParameter("type");
    	return ProjectManager.singleton.getProject(Long.parseLong(projectId));
	}

	private boolean isPrefixedQName(String s){
    	return qnamePattern.matcher(s).find();
    }
    
    private List<SearchResultItem> search(RdfSchema schema, String query){
    	List<SearchResultItem> result = new ArrayList<SearchResultItem>();
    	
    	if(isPrefixedQName(query)){
    		int index = query.indexOf(":");
    		String prefix = query.substring(0,index);
    		String lPart = query.substring(index + 1);
    		for(Entry<String, String> entry:schema.getPrefixesMap().entrySet()){
    			String name = entry.getKey();
    			if (name.equals(prefix)){
    				String uri = entry.getValue();
    				result.add(new SearchResultItem(uri+lPart, prefix, lPart, "", "Not in the imported vocabulary definition"));
    			}
    		}
    	}else{
    		for(Entry<String, String> entry:schema.getPrefixesMap().entrySet()){
    			String name = entry.getKey();
    			if (name.startsWith(query)){
    				String uri = entry.getValue();
    				result.add(new SearchResultItem(uri, name, "", "", "Not in the imported vocabulary definition"));
    			}
    		}
    	}
    	return result;
    	
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
