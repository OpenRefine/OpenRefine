/*

Copyright 2010, Google Inc.
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

package com.google.refine.commands.column;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.cell.TextTransformOperation;
import com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation;
import com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation.HttpHeader;

public class AddColumnByFetchingURLsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, EngineConfig engineConfig) throws Exception {

        String baseColumnName = request.getParameter("baseColumnName");
        String urlExpression = request.getParameter("urlExpression");
        String newColumnName = request.getParameter("newColumnName");
        int columnInsertIndex = Integer.parseInt(request.getParameter("columnInsertIndex"));
        int delay = Integer.parseInt(request.getParameter("delay"));
        String onError = request.getParameter("onError");
        boolean cacheResponses = Boolean.parseBoolean(request.getParameter("cacheResponses"));
        ObjectMapper mapper = new ObjectMapper();
        List<HttpHeader> headers = Arrays.asList(mapper.readValue(request.getParameter("httpHeaders"), HttpHeader[].class));

        return new ColumnAdditionByFetchingURLsOperation(
                engineConfig,
                baseColumnName,
                urlExpression,
                TextTransformOperation.stringToOnError(onError),
                newColumnName,
                columnInsertIndex,
                delay,
                cacheResponses,
                headers);
    }

}
