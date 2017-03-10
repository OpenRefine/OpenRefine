package org.deri.grefine.rdf.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.util.ParsingUtilities;

public class SavePrefixesCommand extends RdfCommand{

	public SavePrefixesCommand(ApplicationContext ctxt) {
		super(ctxt);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			Map<String, Vocabulary> prefixesMap = new HashMap<String, Vocabulary>();
			JSONArray prefixesArr = ParsingUtilities.evaluateJsonStringToArray(request.getParameter("prefixes"));
			for(int i =0;i<prefixesArr.length();i++){
				JSONObject prefixObj = prefixesArr.getJSONObject(i);
				String name = prefixObj.getString("name");
				prefixesMap.put(name,new Vocabulary(name, prefixObj.getString("uri")));
			}
			getRdfSchema(request).setPrefixesMap(prefixesMap);
			
			String projectId = request.getParameter("project");
			getRdfContext().getVocabularySearcher().synchronize(projectId, prefixesMap.keySet());
			
			response.setCharacterEncoding("UTF-8");
	        response.setHeader("Content-Type", "application/json");
	        JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.endObject();
		} catch (Exception e) {
            respondException(response, e);
        }
	}
}
