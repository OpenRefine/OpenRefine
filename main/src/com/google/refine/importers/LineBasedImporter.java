/*******************************************************************************
 * Copyright (C) 2018, 2022 OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.importers;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class LineBasedImporter extends TabularImportingParserBase {

    static final Logger logger = LoggerFactory.getLogger(LineBasedImporter.class);

    public LineBasedImporter() {
        super(false);
    }

    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job, List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        JSONUtilities.safePut(options, "separator", "\\r?\\n");

        JSONUtilities.safePut(options, "linesPerRow", 1);
        JSONUtilities.safePut(options, "headerLines", 0);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);

        return options;
    }

    @Override
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            Reader reader,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {
        String sepStr = JSONUtilities.getString(options, "separator", "\\r?\\n");
        if (sepStr == null || "".equals(sepStr)) {
            sepStr = "\\r?\\n";
        }
        sepStr = StringEscapeUtils.unescapeJava(sepStr);
        Pattern sep = Pattern.compile(sepStr);

        final int linesPerRow = JSONUtilities.getInt(options, "linesPerRow", 1);

        final List<Object> columnNames;
        if (options.has("columnNames")) {
            columnNames = new ArrayList<>(Arrays.asList(JSONUtilities.getStringArray(options, "columnNames")));
            JSONUtilities.safePut(options, "headerLines", 1);
        } else {
            columnNames = null;
            JSONUtilities.safePut(options, "headerLines", 0);
        }

        final Scanner lnReader = new Scanner(new BufferedReader(reader));
        lnReader.useDelimiter(sep);

        int skip = JSONUtilities.getInt(options, "ignoreLines", -1);
        while (skip > 0) {
            lnReader.next();
            skip--;
        }
        if (lnReader.ioException() != null) {
            logger.error("Error reading line-based file", lnReader.ioException());
        }
        JSONUtilities.safePut(options, "ignoreLines", -1);

        TableDataReader dataReader = new TableDataReader() {

            boolean usedColumnNames = false;

            @Override
            public List<Object> getNextRowOfCells() {
                if (columnNames != null && !usedColumnNames) {
                    usedColumnNames = true;
                    return columnNames;
                } else {
                    List<Object> cells = null;
                    for (int i = 0; i < linesPerRow; i++) {
                        if (lnReader.hasNext()) {
                            String line = lnReader.next();
                            if (i == 0) {
                                cells = new ArrayList<>(linesPerRow);
                            }
                            cells.add(line);
                        } else {
                            break;
                        }
                    }
                    return cells;
                }
            }
        };

        TabularImportingParserBase.readTable(project, job, dataReader, limit, options, exceptions);
    }
}
