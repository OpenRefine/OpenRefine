package org.deri.grefine.reconcile.commands;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.grefine.reconcile.GRefineServiceManager;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;

public class InitializationCommand extends Command{

	final static Logger logger = LoggerFactory.getLogger("rdf_reconcile_initializer");
	
	@Override
	public void init(RefineServlet servlet) {
		File workingDir = servlet.getCacheDir("rdfExtension/reconcile");
		try {
			GRefineServiceManager.initialize(workingDir);
		} catch (JSONException e) {
			logger.error("Failed to initialize the extension. ", e);
		} catch (IOException e) {
			logger.error("Failed to initialize the extension. ", e);
		}
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
}
