/*

Copyright 2011, Google Inc.
Copyright 2012,2020 OpenRefine contributors
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

package com.google.refine.importers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.ImporterUtilities.MultiFileReadingProgress;
import com.google.refine.importing.EncodingGuesser;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingParser;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

abstract public class ImportingParserBase implements ImportingParser {

    final static Logger logger = LoggerFactory.getLogger("ImportingParserBase");

    final protected boolean useInputStream;

    /**
     * @param useInputStream
     *            true if parser takes an InputStream, false if it takes a Reader.
     */
    protected ImportingParserBase(boolean useInputStream) {
        this.useInputStream = useInputStream;
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ObjectNode> fileRecords, String format) {
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "includeFileSources", fileRecords.size() > 1);
        JSONUtilities.safePut(options, "includeArchiveFileName", ImportingUtilities.hasArchiveFileField(fileRecords));
        EncodingGuesser.guessInitialEncoding(fileRecords, options);
        return options;
    }

    @Override
    public void parse(Project project, ProjectMetadata metadata,
            final ImportingJob job, List<ObjectNode> fileRecords, String format,
            int limit, ObjectNode options, List<Exception> exceptions) {
        MultiFileReadingProgress progress = ImporterUtilities.createMultiFileReadingProgress(job, fileRecords);
        for (ObjectNode fileRecord : fileRecords) {
            if (job.canceled) {
                break;
            }

            try {
                parseOneFile(project, metadata, job, fileRecord, limit, options, exceptions, progress);
            } catch (IOException e) {
                exceptions.add(e);
            }

            if (limit > 0 && project.rows.size() >= limit) {
                break;
            }
        }
    }

    // TODO: Make private? At least protected?
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            ObjectNode fileRecord,
            int limit,
            ObjectNode options,
            List<Exception> exceptions,
            final MultiFileReadingProgress progress) throws IOException {
        final File file = ImportingUtilities.getFile(job, fileRecord);
        final String fileSource = ImportingUtilities.getFileSource(fileRecord);
        final String archiveFileName = ImportingUtilities.getArchiveFileName(fileRecord);
        int filenameColumnIndex = -1;
        int archiveColumnIndex = -1;
        int startingRowCount = project.rows.size();

        progress.startFile(fileSource);
        try {
            InputStream inputStream = ImporterUtilities.openAndTrackFile(fileSource, file, progress);
            try {

                if (JSONUtilities.getBoolean(options, "includeArchiveFileName", false)
                        && archiveFileName != null) {
                    archiveColumnIndex = addArchiveColumn(project);
                }
                if (JSONUtilities.getBoolean(options, "includeFileSources", false)) {
                    filenameColumnIndex = addFilenameColumn(project, archiveColumnIndex >= 0);
                }

                if (useInputStream) {
                    parseOneFile(project, metadata, job, fileSource, inputStream, limit, options, exceptions);
                } else {
                    String commonEncoding = JSONUtilities.getString(options, "encoding", null);
                    if (commonEncoding != null && commonEncoding.isEmpty()) {
                        commonEncoding = null;
                    }

                    Reader reader = ImportingUtilities.getReaderFromStream(
                            inputStream, fileRecord, commonEncoding);

                    parseOneFile(project, metadata, job, fileSource, reader, limit, options, exceptions);
                }

                // Fill in filename and archive name column for all rows added from this file
                int endingRowCount = project.rows.size();
                for (int i = startingRowCount; i < endingRowCount; i++) {
                    Row row = project.rows.get(i);
                    if (archiveColumnIndex >= 0) {
                        row.setCell(archiveColumnIndex, new Cell(archiveFileName, null));
                    }
                    if (filenameColumnIndex >= 0) {
                        row.setCell(filenameColumnIndex, new Cell(fileSource, null));
                    }
                }

                ObjectNode fileOptions = options.deepCopy();
                JSONUtilities.safePut(fileOptions, "fileSource", fileSource);
                JSONUtilities.safePut(fileOptions, "archiveFileName", archiveFileName);
                // TODO: This will save a separate copy for each file in the import, but they're
                // going to be mostly the same
                metadata.appendImportOptionMetadata(fileOptions);
            } finally {
                inputStream.close();
            }
        } finally {
            progress.endFile(fileSource, file.length());
        }
    }

    /**
     * Parsing method to be implemented by Reader-based parsers. ie those initialized with useInputStream == false
     * 
     * @param project
     * @param metadata
     * @param job
     * @param fileSource
     * @param reader
     * @param limit
     * @param options
     * @param exceptions
     */
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            Reader reader,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        throw new NotImplementedException();
    }

    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            InputStream inputStream,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        throw new NotImplementedException();
    }

    /**
     * @deprecated 2020-07-21 by tfmorris. This will become private in a future release.
     */
    @Deprecated
    protected static int addFilenameColumn(Project project, boolean archiveColumnAdded) {
        String fileNameColumnName = "File"; // TODO: Localize?
        int columnId = archiveColumnAdded ? 1 : 0;
        return addColumn(project, fileNameColumnName, columnId);
    }

    protected static int addArchiveColumn(Project project) {
        String columnName = "Archive"; // TODO: Localize?
        return addColumn(project, columnName, 0);
    }

    private static int addColumn(Project project, String columnName, int columnId) {
        if (project.columnModel.getColumnByName(columnName) == null) {
            try {
                project.columnModel.addColumn(
                        columnId, new Column(project.columnModel.allocateNewCellIndex(), columnName), false);
                return columnId;
            } catch (ModelException e) {
                // Shouldn't happen: We already checked for duplicate name.
                logger.error("ModelException adding Filename column", e);
            }
            return -1;
        } else {
            return columnId;
        }
    }

}
