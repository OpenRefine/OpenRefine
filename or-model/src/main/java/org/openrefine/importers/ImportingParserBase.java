/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.importers;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.openrefine.ProjectMetadata;
import org.openrefine.importers.ImporterUtilities.MultiFileReadingProgress;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingParser;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Importer which handles the import of multiple files as a single one.
 */
abstract public class ImportingParserBase implements ImportingParser {
    final static Logger logger = LoggerFactory.getLogger("ImportingParserBase");

    final protected DatamodelRunner runner;
    
    /**
     */
    protected ImportingParserBase(DatamodelRunner runner) {
        this.runner = runner;
    }
    
    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "includeFileSources", fileRecords.size() > 1);
        
        return options;
    }
    
    @Override
    public GridState parse(ProjectMetadata metadata,
            final ImportingJob job, List<ImportingFileRecord> fileRecords, String format,
            long limit, ObjectNode options) throws Exception {
    	FileSystem hdfs = runner.getFileSystem();
        MultiFileReadingProgress progress = ImporterUtilities.createMultiFileReadingProgress(job, fileRecords, hdfs);
        List<GridState> gridStates = new ArrayList<>(fileRecords.size());
        
        if (fileRecords.isEmpty()) {
        	throw new IllegalArgumentException("No file provided");
        }
        
        long totalRows = 0;
        for (ImportingFileRecord fileRecord : fileRecords) {
            if (job.canceled) {
                break;
            }
            
            long fileLimit = limit < 0 ? limit : Math.max(limit-totalRows,1L);
            GridState gridState = parseOneFile(metadata, job, fileRecord, fileLimit, options, progress);
			gridStates.add(gridState);
            totalRows += gridState.rowCount();

            if (limit > 0 && totalRows >= limit) {
                break;
            }
        }
        return mergeGridStates(gridStates);
    }
    
    /**
     * Merges grids of individual files into one single grid.
     * 
     * @param gridStates
     *    a list of grids returned by the importers
     * @return
     */
    protected GridState mergeGridStates(List<GridState> gridStates) {
        if (gridStates.isEmpty()) {
            throw new IllegalArgumentException("No grid states provided");
        }
        GridState current = gridStates.get(0);
        for (int i = 1; i != gridStates.size(); i++) {
            current = ImporterUtilities.mergeGridStates(current, gridStates.get(i));
        }
        return current;
	}

	public GridState parseOneFile(
        ProjectMetadata metadata,
        ImportingJob job,
        ImportingFileRecord fileRecord,
        long limit,
        ObjectNode options,
        final MultiFileReadingProgress progress
    ) throws Exception {
        
        final String fileSource = fileRecord.getFileSource();
        
        progress.startFile(fileSource);
        pushImportingOptions(metadata, fileSource, options);
       
    	if (this instanceof HDFSImporter) {
    		return ((HDFSImporter)this).parseOneFile(metadata, job, fileSource, fileRecord.getDerivedSparkURI(job.getRawDataDir()), limit, options);
    	} else {
    		final File file = fileRecord.getFile(job.getRawDataDir());
    		try {
	            InputStream inputStream = ImporterUtilities.openAndTrackFile(fileSource, file, progress);
	            try {
	                if (this instanceof InputStreamImporter) {
	                    return ((InputStreamImporter)this).parseOneFile(metadata, job, fileSource, inputStream, limit, options);
	                } else {
	                    String commonEncoding = JSONUtilities.getString(options, "encoding", null);
	                    if (commonEncoding != null && commonEncoding.isEmpty()) {
	                        commonEncoding = null;
	                    }
	                    
	                    Reader reader = ImporterUtilities.getReaderFromStream(
	                        inputStream, fileRecord, commonEncoding);
	                    
	                    return ((ReaderImporter)this).parseOneFile(metadata, job, fileSource, reader, limit, options);
	                }
	            } finally {
	                inputStream.close();
	            }
    		} finally {
    	        progress.endFile(fileSource, file.length());
	        }
    	}
        
    }

    private void pushImportingOptions(ProjectMetadata metadata, String fileSource, ObjectNode options) {
        options.put("fileSource", fileSource);
        // set the import options to metadata:
        metadata.appendImportOptionMetadata(options);
    }
   
}
