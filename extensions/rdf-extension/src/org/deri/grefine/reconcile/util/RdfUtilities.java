package org.deri.grefine.reconcile.util;

import com.hp.hpl.jena.rdf.model.Model;

public interface RdfUtilities {
	public Model dereferenceUri(String uri);
}
