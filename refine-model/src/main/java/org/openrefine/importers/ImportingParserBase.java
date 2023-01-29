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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.ProjectMetadata;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingParser;
import org.openrefine.model.Grid;
import org.openrefine.model.Runner;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

/**
 * Importer which handles the import of multiple files as a single one.
 */
abstract public class ImportingParserBase implements ImportingParser {

    final static Logger logger = LoggerFactory.getLogger("ImportingParserBase");

    @Override
    public ObjectNode createParserUIInitializationData(Runner runner,
            ImportingJob job, List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "includeFileSources", fileRecords.size() > 1);
        return options;
    }

    @Override
    public Grid parse(Runner runner,
            ProjectMetadata metadata, final ImportingJob job, List<ImportingFileRecord> fileRecords,
            String format, long limit, ObjectNode options) throws Exception {
        MultiFileReadingProgress progress = ImporterUtilities.createMultiFileReadingProgress(job, fileRecords);
        List<Grid> grids = new ArrayList<>(fileRecords.size());

        if (fileRecords.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        long totalRows = 0;
        for (ImportingFileRecord fileRecord : fileRecords) {
            if (job.canceled) {
                break;
            }

            long fileLimit = limit < 0 ? limit : Math.max(limit - totalRows, 1L);
            Grid grid = parseOneFile(runner, metadata, job, fileRecord, fileLimit, options, progress);
            grids.add(grid);
            totalRows += grid.rowCount();

            if (limit > 0 && totalRows >= limit) {
                break;
            }
        }
        return mergeGrids(grids);
    }

    /**
     * Merges grids of individual files into one single grid.
     * 
     * @param grids
     *            a list of grids returned by the importers
     */
    protected Grid mergeGrids(List<Grid> grids) {
        if (grids.isEmpty()) {
            throw new IllegalArgumentException("No grids provided");
        }
        Grid current = grids.get(0);
        for (int i = 1; i != grids.size(); i++) {
            current = ImporterUtilities.mergeGrids(current, grids.get(i));
        }
        return current;
    }

    public Grid parseOneFile(
            Runner runner,
            ProjectMetadata metadata,
            ImportingJob job,
            ImportingFileRecord fileRecord,
            long limit,
            ObjectNode options, final MultiFileReadingProgress progress) throws Exception {

        String fileSource = fileRecord.getFileSource();
        String archiveFileName = fileRecord.getArchiveFileName();

        progress.startFile(fileSource);
        ObjectNode optionsCopy = options.deepCopy();
        pushImportingOptions(metadata, fileSource, archiveFileName, optionsCopy);

        if (this instanceof URIImporter) {
            return ((URIImporter) this).parseOneFile(runner, metadata, job, fileSource, archiveFileName,
                    fileRecord.getDerivedSparkURI(job.getRawDataDir()), limit, options, progress);
        } else {
            final File file = fileRecord.getFile(job.getRawDataDir());
            try {
                InputStream inputStream = ImporterUtilities.openAndTrackFile(fileSource, file, progress);
                try {
                    if (this instanceof InputStreamImporter) {
                        return ((InputStreamImporter) this).parseOneFile(runner, metadata, job, fileSource, archiveFileName, inputStream,
                                limit, options);
                    } else {
                        String commonEncoding = JSONUtilities.getString(options, "encoding", null);
                        if (commonEncoding != null && commonEncoding.isEmpty()) {
                            commonEncoding = null;
                        }

                        Reader reader = ImporterUtilities.getReaderFromStream(
                                inputStream, fileRecord, commonEncoding);

                        return ((ReaderImporter) this).parseOneFile(runner, metadata, job, fileSource, archiveFileName, reader, limit,
                                options);
                    }
                } finally {
                    inputStream.close();
                }
            } finally {
                progress.endFile(fileSource, file.length());
            }
        }

    }

    private void pushImportingOptions(ProjectMetadata metadata, String fileSource, String archiveFileName, ObjectNode options) {
        options.put("fileSource", fileSource);
        options.put("archiveFileName", archiveFileName);
        // set the import options to metadata:
        metadata.appendImportOptionMetadata(options);
    }

}
