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

package com.google.refine.commands.cell;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.cell.MultiValuedCellSplitOperation;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.process.Process;

public class SplitMultiValueCellsCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String columnName = request.getParameter("columnName");
            String keyColumnName = request.getParameter("keyColumnName");
            String separator = request.getParameter("separator");
            String mode = request.getParameter("mode");
            Boolean regex = Boolean.parseBoolean(request.getParameter("regex"));

            if ("separator".equals(mode)) {
                AbstractOperation op = new MultiValuedCellSplitOperation(columnName, 
                                                                         keyColumnName,
                                                                         separator, 
                                                                         regex);
                Process process = op.createProcess(project, new Properties());
                
                performProcessAndRespond(request, response, project, process);
            } else {
                String s = request.getParameter("fieldLengths");
                
                JSONArray a = ParsingUtilities.evaluateJsonStringToArray(s);
                int[] fieldLengths = new int[a.length()];
                
                for (int i = 0; i < fieldLengths.length; i++) {
                    fieldLengths[i] = a.getInt(i);
                }
                
                AbstractOperation op = new MultiValuedCellSplitOperation(columnName,
                                                                         keyColumnName,
                                                                         fieldLengths);
                Process process = op.createProcess(project, new Properties());
                
                performProcessAndRespond(request, response, project, process);
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
