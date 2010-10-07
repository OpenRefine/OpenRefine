package com.google.refine.rdf.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.refine.commands.Command;
import com.google.refine.rdf.RdfSchema;
import com.google.refine.rdf.Util;
import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.VocabularyIndexException;

public abstract class RdfCommand extends Command{

	private ApplicationContext rdfContext;
	
	public RdfCommand(ApplicationContext ctxt){
		super();
		this.rdfContext = ctxt;
	}
	public ApplicationContext getRdfContext() {
		return rdfContext;
	}
	public RdfSchema getRdfSchema(HttpServletRequest request) throws ServletException{
		try {
			return Util.getProjectSchema(getRdfContext(), getProject(request));
		} catch (VocabularyIndexException e) {
			throw new ServletException("Unable to create index for RDF schema",e);
		} catch (IOException e) {
			throw new ServletException("Unable to create index for RDF schema",e);
		}
	}
}
