package org.deri.grefine.reconcile.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RdfUtilitiesImpl implements RdfUtilities{

	final static Logger logger = LoggerFactory.getLogger("RdfUtilities");
	@Override
	public Model dereferenceUri(String uri) {
		Model model = ModelFactory.createDefaultModel();
		try{
			model.read(uri);
		}catch(Exception ex){
			//silent
			//if it fails try RDFa parsing
			try{
				Class.forName("net.rootdev.javardfa.jena.RDFaReader");
				model.read(uri, "HTML");
			}catch(Exception e){
				logger.error("Error loading RDFa parser", e);
			}
		}
		return model;
	}

}
