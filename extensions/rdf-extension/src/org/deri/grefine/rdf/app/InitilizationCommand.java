package org.deri.grefine.rdf.app;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;

public class InitilizationCommand extends Command{

	private File workingDir;
	
	@Override
	public void init(RefineServlet servlet) {
		workingDir = servlet.getCacheDir("rdfExtension/export");
		super.init(servlet);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request,response);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		throw new UnsupportedOperationException("This command is not meant to be called. It is just necessary for initialization");
	}
	
	public void initRdfExportApplicationContext(ApplicationContext ctxt) throws IOException, JSONException{
		ctxt.init(workingDir);
	}

	
}
