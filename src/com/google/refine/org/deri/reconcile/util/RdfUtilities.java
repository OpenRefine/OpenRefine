package com.google.refine.org.deri.reconcile.util;

import com.hp.hpl.jena.rdf.model.Model;

public interface RdfUtilities {
	public Model dereferenceUri(String uri);
}
