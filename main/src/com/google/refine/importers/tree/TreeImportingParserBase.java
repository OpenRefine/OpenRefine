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

package com.google.refine.importers.tree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import com.google.refine.model.Cell;
import com.google.refine.model.Row;
import org.apache.commons.lang.NotImplementedException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.ImporterUtilities;
import com.google.refine.importers.ImporterUtilities.MultiFileReadingProgress;
import com.google.refine.importers.ImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

/**
 * Abstract class for importer parsers which handle tree-shaped data (currently XML &amp; JSON).
 */
abstract public class TreeImportingParserBase extends ImportingParserBase {

    protected TreeImportingParserBase(final boolean useInputStream) {
        super(useInputStream);
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        JSONUtilities.safePut(options, "trimStrings", false);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        JSONUtilities.safePut(options, "storeEmptyStrings", true);
        return options;
    }

    @Override
    public void parse(Project project, ProjectMetadata metadata,
            ImportingJob job, List<ObjectNode> fileRecords, String format,
            int limit, ObjectNode options, List<Exception> exceptions) {

        MultiFileReadingProgress progress = ImporterUtilities.createMultiFileReadingProgress(job, fileRecords);
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();

        for (ObjectNode fileRecord : fileRecords) {
            try {
                parseOneFile(project, metadata, job, fileRecord, rootColumnGroup, limit, options, exceptions, progress);
            } catch (IOException e) {
                exceptions.add(e);
            }

            if (limit > 0 && project.rows.size() >= limit) {
                break;
            }
        }

        rootColumnGroup.tabulate();
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.columnModel.update();
    }

    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            ObjectNode fileRecord,
            ImportColumnGroup rootColumnGroup,
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
                    parseOneFile(project, metadata, job, fileSource, inputStream,
                            rootColumnGroup, limit, options, exceptions);
                } else {
                    String commonEncoding = JSONUtilities.getString(options, "encoding", null);
                    if (commonEncoding != null && commonEncoding.isEmpty()) {
                        commonEncoding = null;
                    }

                    Reader reader = ImportingUtilities.getFileReader(file, fileRecord, commonEncoding);
                    parseOneFile(project, metadata, job, fileSource, reader,
                            rootColumnGroup, limit, options, exceptions);
                }

//                 Fill in filename and archive name column for all rows added from this file
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
     * Parse a single file from a Reader.
     *
     * The default implementation just throws a NotImplementedException. Override in subclasses to implement.
     */
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            Reader reader,
            ImportColumnGroup rootColumnGroup,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        throw new NotImplementedException();
    }

    /**
     * Parse a single file from an InputStream.
     *
     * The default implementation just throws a NotImplementedException. Override in subclasses to implement.
     */
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            InputStream inputStream,
            ImportColumnGroup rootColumnGroup,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        throw new NotImplementedException();
    }

    /**
     * Parse a single file from a TreeReader.
     *
     */
    protected void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            TreeReader treeParser,
            ImportColumnGroup rootColumnGroup,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        String[] recordPath = JSONUtilities.getStringArray(options, "recordPath");
        int limit2 = JSONUtilities.getInt(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }
        if (limit2 == 0) { // shouldn't really happen, but be sure since 0 is stop signal
            limit2 = -1;
        }

        // NOTE: these defaults are solely to preserve historical behavior.
        // All new code should override them to keep input data from being modified
        boolean trimStrings = JSONUtilities.getBoolean(options, "trimStrings", true);
        boolean storeEmptyStrings = JSONUtilities.getBoolean(options, "storeEmptyStrings", false);
        boolean guessCellValueTypes = JSONUtilities.getBoolean(options, "guessCellValueTypes", true);

        try {
            XmlImportUtilities.importTreeData(treeParser, project, recordPath, rootColumnGroup, limit2,
                    trimStrings, storeEmptyStrings, guessCellValueTypes);
        } catch (Exception e) {
            exceptions.add(e);
        }
    }
}
