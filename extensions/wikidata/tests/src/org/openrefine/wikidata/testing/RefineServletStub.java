package org.openrefine.wikidata.testing;

import java.io.File;
import java.io.IOException;

import org.openrefine.RefineServlet;

public class RefineServletStub extends RefineServlet {
	private static final long serialVersionUID = 1L;
	private static File tempDir = null;

    @Override
	public File getTempDir() {
	    if (tempDir == null) {
	        try {
	            tempDir = File.createTempFile("refine-test-dir", "");
	            tempDir.deleteOnExit();
	        } catch (IOException e) {
	            throw new RuntimeException("Failed to create temp directory",e);
	        }
	    }
	    return tempDir; 
	}
}
