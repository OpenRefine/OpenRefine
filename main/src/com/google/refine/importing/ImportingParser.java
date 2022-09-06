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

package com.google.refine.importing;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;

public interface ImportingParser {

    /**
     * Create data sufficient for the parser UI on the client side to do its work. For example, an XML parser UI would
     * need to know some sample elements so it can let the user pick which the path to the record elements.
     * 
     * @param job
     * @param fileRecords
     * @param format
     * @return ObjectNode options
     */
    public ObjectNode createParserUIInitializationData(
            ImportingJob job,
            List<ObjectNode> fileRecords,
            String format);

    /**
     * 
     * @param project
     * @param metadata
     * @param job
     * @param fileRecords
     * @param format
     * @param limit
     *            maximum number of rows to create
     * @param options
     *            custom options put together by the UI corresponding to this parser, which the parser should understand
     * @param exceptions
     *            list of exceptions thrown during the parse. Expects an empty List as input to which it can append new
     *            Exceptions thrown
     */
    public void parse(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            List<ObjectNode> fileRecords,
            String format,
            int limit,
            ObjectNode options,
            List<Exception> exceptions);
}
