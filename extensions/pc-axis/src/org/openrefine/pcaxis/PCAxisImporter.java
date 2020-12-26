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

package org.openrefine.pcaxis;

import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.ProjectMetadata;
import org.openrefine.importers.ReaderImporter;
import org.openrefine.importers.TabularParserHelper;
import org.openrefine.importers.TabularParserHelper.TableDataReader;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

public class PCAxisImporter extends ReaderImporter {

    static final Logger logger = LoggerFactory.getLogger(PCAxisImporter.class);

    public PCAxisImporter(DatamodelRunner runner) {
        super(runner);
    }

    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job,
            List<ImportingFileRecord> fileRecords,
            String format) {
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "includeFileSources", fileRecords.size() > 1);
        JSONUtilities.safePut(options, "skipDataLines", 0);
        JSONUtilities.safePut(options, "limit", -1);
        return options;
    }

    @Override
    public GridState parseOneFile(
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            String archiveFileName,
            Reader reader,
            long limit, ObjectNode options) throws Exception {
        LineNumberReader lnReader = new LineNumberReader(reader);
        List<Exception> exceptions = new ArrayList<>();
        TableDataReader dataReader = new PCAxisTableDataReader(lnReader, exceptions);

        // Stuff these settings to get TabularImportingParserBase.readTable
        // to behave as we want.
        JSONUtilities.safePut(options, "ignoreLines", -1);
        JSONUtilities.safePut(options, "headerLines", 1);
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);

        TabularParserHelper tabularParsingHelper = new TabularParserHelper(runner);
        GridState grid = tabularParsingHelper.parseOneFile(
                metadata, job, fileSource, "", dataReader,
                limit, options);
        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }
        return grid;
    }
}
