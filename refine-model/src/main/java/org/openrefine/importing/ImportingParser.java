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

package org.openrefine.importing;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openrefine.ProjectMetadata;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;

public interface ImportingParser {

    /**
     * Create data sufficient for the parser UI on the client side to do its work. For example, an XML parser UI would
     * need to know some sample elements so it can let the user pick which the path to the record elements.
     * 
     * @param runner
     * @param job
     * @param fileRecords
     * @param format
     * 
     * @return ObjectNode options
     */
    public ObjectNode createParserUIInitializationData(
            DatamodelRunner runner,
            ImportingJob job,
            List<ImportingFileRecord> fileRecords,
            String format);

    /**
     * Main method of the parser: parse a grid out of input files
     *
     * @param runner
     *            the datamodel runner to use to create the grid
     * @param metadata
     *            the project metadata, to which the importer can contribute
     * @param job
     *            the importing job, useful to report importing progress
     * @param fileRecords
     *            the files to import
     * @param format
     *            the identifier for the format
     * @param limit
     *            maximum number of rows to create
     * @param options
     *            custom options put together by the UI corresponding to this parser, which the parser should understand
     */
    public GridState parse(
            DatamodelRunner runner,
            ProjectMetadata metadata,
            ImportingJob job,
            List<ImportingFileRecord> fileRecords,
            String format,
            long limit,
            ObjectNode options) throws Exception;
}
