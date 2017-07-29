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

package com.google.refine.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.InterProjectModel.ProjectJoin;
import com.google.refine.ProjectManager;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.WrappedCell;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.model.Project;

public class Cross implements Function {
    
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3) {
            // 1st argument can take either value or cell(for backward compatibility)
            Object v = args[0];
            Object toProjectName = args[1];
            Object toColumnName = args[2];
            
            if (v != null && 
                ( v instanceof String || v instanceof WrappedCell ) &&
                toProjectName != null && toProjectName instanceof String &&
                toColumnName != null && toColumnName instanceof String) {
                
                ProjectJoin join = ProjectManager.singleton.getInterProjectModel().getJoin(
                        ProjectManager.singleton.getProjectMetadata(((Project) bindings.get("project")).id).getName(),
                        (String) bindings.get("columnName"), 
                        (String) toProjectName, 
                        (String) toColumnName
                        );
                
                String srcValue = v instanceof String ? (String)v : (String)((WrappedCell) v).cell.value;
                        
                return join.getRows(srcValue);
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string or cell, a project name to join with, and a column name in that project");
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("join with another project by column");
        writer.key("params"); writer.value("cell c or string value, string projectName, string columnName");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
