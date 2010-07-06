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
import com.metaweb.gridworks.rdf.vocab.VocabularyExistException;
import com.metaweb.gridworks.rdf.vocab.VocabularyManager;

public class ImportVocabularyCommand extends Command{

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String prefix = request.getParameter("prefix");
		String url = request.getParameter("url");
		String namespace = request.getParameter("namespace");
		try {	
			VocabularyManager.singleton.addVocabulary(url, prefix, namespace);
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
		} catch (VocabularyExistException e) {
			respondException(response, e);
		} catch (Exception e){
			respondException(response, e);
		}
	}

	
}
