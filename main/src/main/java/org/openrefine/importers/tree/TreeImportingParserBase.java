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

package org.openrefine.importers.tree;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.openrefine.ProjectMetadata;
import org.openrefine.importers.ImporterUtilities;
import org.openrefine.importers.ImporterUtilities.MultiFileReadingProgress;
import org.openrefine.importers.ImportingParserBase;
import org.openrefine.importers.tree.TreeImportUtilities.ColumnIndexAllocator;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.util.JSONUtilities;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Abstract class for importer parsers which handle tree-shaped data
 * (currently XML & JSON).
 */
abstract public class TreeImportingParserBase extends ImportingParserBase {

    protected TreeImportingParserBase(DatamodelRunner runner) {
        super(runner);
    }
    
    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);
        
        JSONUtilities.safePut(options, "trimStrings", false);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        JSONUtilities.safePut(options, "storeEmptyStrings", true);
        return options;
    }

    
    @Override
    public GridState parse(ProjectMetadata metadata,
            final ImportingJob job, List<ImportingFileRecord> fileRecords, String format,
            long limit, ObjectNode options) throws Exception {
        MultiFileReadingProgress progress = ImporterUtilities.createMultiFileReadingProgress(job, fileRecords, runner.getFileSystem());
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        
        List<Row> rows = new ArrayList<>();
        ColumnIndexAllocator allocator = new ColumnIndexAllocator();
        for (ImportingFileRecord fileRecord : fileRecords) {
            parseOneFile(allocator, rows, metadata, job, fileRecord, rootColumnGroup, limit, options, progress);
            
            if (limit > 0 && rows.size() >= limit) {
                break;
            }
        }
        
        rootColumnGroup.tabulate();
        List<Integer> columnIndexTranslation = new ArrayList<>();
        ColumnModel columnModel = new ColumnModel(Collections.emptyList());
        boolean includeFileSources = JSONUtilities.getBoolean(options, "includeFileSources", false);
        if (includeFileSources) {
        	columnModel.appendUnduplicatedColumn(new ColumnMetadata("File"));
        }
        columnModel = XmlImportUtilities.createColumnsFromImport(columnModel, rootColumnGroup, columnIndexTranslation);
        List<Row> reordered = new ArrayList<>(rows.size());
        for (int i = 0; i != rows.size(); i++) {
        	Row row = rows.get(i);
        	List<Cell> cells = columnIndexTranslation.stream().map(idx -> row.getCell(idx)).collect(Collectors.toList());
        	reordered.add(new Row(cells));
        }
        return runner.create(columnModel, reordered, Collections.emptyMap());
    }
    
    public void parseOneFile(
        ColumnIndexAllocator allocator,
        List<Row> rows,
        ProjectMetadata metadata,
        ImportingJob job,
        ImportingFileRecord fileRecord,
        ImportColumnGroup rootColumnGroup,
        long limit,
        ObjectNode options,
        final MultiFileReadingProgress progress
    ) throws Exception {
        final File file = fileRecord.getFile(job.getRawDataDir());
        final String fileSource = fileRecord.getFileSource();
        
        progress.startFile(fileSource);
        try {
            InputStream inputStream = ImporterUtilities.openAndTrackFile(fileSource, file, progress);
            try {
                parseOneFile(allocator, rows, metadata, job, fileSource, inputStream,
                        rootColumnGroup, limit, options);
            } finally {
                inputStream.close();
            }
        } finally {
            progress.endFile(fileSource, file.length());
        }
    }
    
    /**
     * Parse a single file from an InputStream.
     * 
     * The default implementation just throws a NotImplementedException.
     * Override in subclasses to implement.
     */
    public void parseOneFile(
        ColumnIndexAllocator allocator,
        List<Row> rows,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        InputStream inputStream,
        ImportColumnGroup rootColumnGroup,
        long limit,
        ObjectNode options
    ) throws Exception {
        throw new NotImplementedException();
    }
    
    /**
     * Parse a single file from a TreeReader.
     * 
     */
    protected void parseOneFile(
        ColumnIndexAllocator allocator,
        List<Row> rows,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        TreeReader treeParser,
        ImportColumnGroup rootColumnGroup,
        long limit,
        ObjectNode options
    ) throws Exception {
        String[] recordPath = JSONUtilities.getStringArray(options, "recordPath");
        long limit2 = JSONUtilities.getLong(options, "limit", -1);
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
        
        boolean includeFileSources = JSONUtilities.getBoolean(options, "includeFileSources", false);
        if (includeFileSources) {
        	allocator.allocateColumnIndex();
        }
        
        XmlImportUtilities.importTreeData(treeParser, allocator, rows, recordPath, rootColumnGroup, limit2, 
                new ImportParameters(trimStrings, storeEmptyStrings, guessCellValueTypes, includeFileSources,
                        fileSource));
    }
}
