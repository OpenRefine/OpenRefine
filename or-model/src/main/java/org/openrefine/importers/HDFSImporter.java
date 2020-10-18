package org.openrefine.importers;

import org.openrefine.ProjectMetadata;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A base class for importers which read files via the Hadoop file system (HDFS)
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class HDFSImporter extends ImportingParserBase {

	protected HDFSImporter(DatamodelRunner runner) {
		super(runner);
	}
	
	/**
	 * Parses one file, designated by a HDFS URI (understood by Spark, Flink…).
	 * 
	 * @param metadata
	 *    the project metadata associated with the project to parse (which can be
	 *    modified by the importer)
	 * @param job
	 *    the importing job where this import is being done
	 * @param fileSource
	 *    the original path or source of the file (could be "clipboard" or a URL as well)
	 * @param uri
	 *    the HDFS URI where to read the data from
	 * @param limit
	 *    the maximum number of rows to read
	 * @param options
	 *    any options passed to the importer as a JSON payload
	 * @return
	 *    a parsed GridState
	 */
	public abstract GridState parseOneFile(ProjectMetadata metadata, ImportingJob job, String fileSource, String uri,
			long limit, ObjectNode options) throws Exception;


}
