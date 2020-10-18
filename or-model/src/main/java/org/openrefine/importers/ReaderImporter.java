package org.openrefine.importers;

import java.io.Reader;

import org.openrefine.ProjectMetadata;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A base class for importers which read files in text mode (with a {@link java.io.Reader}).
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class ReaderImporter extends ImportingParserBase {

	protected ReaderImporter(DatamodelRunner runner) {
		super(runner);
	}
	
	/**
	 * Parses one file, read from a {@class Reader} object,
	 * into a GridState.
	 * 
	 * @param metadata
	 *    the project metadata associated with the project to parse (which can be
	 *    modified by the importer)
	 * @param job
	 *    the importing job where this import is being done
	 * @param fileSource
	 *    the path or source of the file (could be "clipboard" or a URL as well)
	 * @param reader
	 *    the reader object where to read the data from
	 * @param limit
	 *    the maximum number of rows to read
	 * @param options
	 *    any options passed to the importer as a JSON payload
	 * @return
	 *    a parsed GridState
	 * @throws Exception
	 */
    public abstract GridState parseOneFile(
	        ProjectMetadata metadata,
	        ImportingJob job,
	        String fileSource,
	        Reader reader,
	        long limit,
	        ObjectNode options
	    ) throws Exception;

}
