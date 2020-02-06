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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.ProjectMetadata;
import org.openrefine.importers.ImporterUtilities.MultiFileReadingProgress;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingParser;
import org.openrefine.importing.ImportingUtilities;
import org.openrefine.model.GridState;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

/**
 * Importer which handles the import of multiple files as a single one.
 */
abstract public class ImportingParserBase implements ImportingParser {

    final static Logger logger = LoggerFactory.getLogger("ImportingParserBase");

    final protected boolean useInputStream;
    final protected JavaSparkContext sparkContext;

    /**
     * @param useInputStream
     *            true if parser takes an InputStream, false if it takes a Reader.
     */
    protected ImportingParserBase(boolean useInputStream, JavaSparkContext context) {
        this.useInputStream = useInputStream;
        this.sparkContext = context;
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ObjectNode> fileRecords, String format) {
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "includeFileSources", fileRecords.size() > 1);

        return options;
    }

    @Override
    public GridState parse(ProjectMetadata metadata,
            final ImportingJob job, List<ObjectNode> fileRecords, String format,
            long limit, ObjectNode options) throws Exception {
        MultiFileReadingProgress progress = ImporterUtilities.createMultiFileReadingProgress(job, fileRecords);
        List<GridState> gridStates = new ArrayList<>(fileRecords.size());

        if (fileRecords.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        long totalRows = 0;
        for (ObjectNode fileRecord : fileRecords) {
            if (job.canceled) {
                break;
            }

            long fileLimit = limit < 0 ? limit : Math.max(limit - totalRows, 1L);
            GridState gridState = parseOneFile(metadata, job, fileRecord, fileLimit, options, progress);
            gridStates.add(gridState);
            totalRows += gridState.size();

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
     *            a list of grids returned by the importers
     * @return
     */
    private GridState mergeGridStates(List<GridState> gridStates) {
        if (gridStates.size() == 1) {
            return gridStates.get(0);
        } else {
            // TODO add back multiple file support
            // - merge the column models with a "File" column at the start, containing the filename
            // - do the union of grid states
            throw new IllegalStateException("Support for multiple files not implemented");
        }
    }

    public GridState parseOneFile(
            ProjectMetadata metadata,
            ImportingJob job,
            ObjectNode fileRecord,
            long limit,
            ObjectNode options,
            final MultiFileReadingProgress progress) throws Exception {
        final File file = ImportingUtilities.getFile(job, fileRecord);
        final String fileSource = ImportingUtilities.getFileSource(fileRecord);

        progress.startFile(fileSource);
        try {
            InputStream inputStream = ImporterUtilities.openAndTrackFile(fileSource, file, progress);
            try {
                pushImportingOptions(metadata, fileSource, options);
                if (useInputStream) {
                    return parseOneFile(metadata, job, fileSource, inputStream, limit, options);
                } else {
                    String commonEncoding = JSONUtilities.getString(options, "encoding", null);
                    if (commonEncoding != null && commonEncoding.isEmpty()) {
                        commonEncoding = null;
                    }

                    Reader reader = ImportingUtilities.getReaderFromStream(
                            inputStream, fileRecord, commonEncoding);

                    return parseOneFile(metadata, job, fileSource, reader, limit, options);
                }
            } finally {
                inputStream.close();
            }
        } finally {
            progress.endFile(fileSource, file.length());
        }
    }

    /**
     * Parses one file, read from a {@class Reader} object, into a GridState.
     * 
     * @param metadata
     * @param job
     * @param fileSource
     * @param reader
     * @param limit
     * @param options
     * @return
     * @throws Exception
     */
    public GridState parseOneFile(
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            Reader reader,
            long limit,
            ObjectNode options) throws Exception {
        throw new NotImplementedException("Importer does not support reading from a Reader");
    }

    /**
     * Parses one file, read from an {@class InputStream} object, into a GridState.
     * 
     * @param metadata
     * @param job
     * @param fileSource
     * @param inputStream
     * @param limit
     * @param options
     * @return
     * @throws Exception
     */
    public GridState parseOneFile(
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            InputStream inputStream,
            long limit,
            ObjectNode options) throws Exception {
        throw new NotImplementedException("Importer does not support reading from an InputStream");
    }

    private void pushImportingOptions(ProjectMetadata metadata, String fileSource, ObjectNode options) {
        options.put("fileSource", fileSource);
        // set the import options to metadata:
        metadata.appendImportOptionMetadata(options);
    }

}