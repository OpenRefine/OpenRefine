package org.deri.grefine.rdf.expr;

import java.io.IOException;
import java.util.Properties;

import org.deri.grefine.rdf.Util;
import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.VocabularyIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.expr.Binder;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class RdfBinder implements Binder {

	final static Logger logger = LoggerFactory.getLogger("RdfBinder");
	
	private ApplicationContext rdfContext;
	
	public RdfBinder(ApplicationContext ctxt){
		super();
		this.rdfContext = ctxt;
	}
    @Override
    public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell) {
        // nothing to do
    }

    @Override
    public void initializeBindings(Properties bindings, Project project) {
        try {
			bindings.put("baseURI", Util.getProjectSchema(rdfContext,project).getBaseUri());
		} catch (VocabularyIndexException e) {
			logger.error("Unable to bind baseURI. Unable to create an index for RDF Schema", e);
		} catch (IOException e) {
			logger.error("Unable to bind baseURI. Unable to create an index for RDF Schema", e);
		}
    }

}
