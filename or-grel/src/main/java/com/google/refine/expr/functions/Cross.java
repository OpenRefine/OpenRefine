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

import com.google.refine.InterProjectModel.ProjectJoin;
import com.google.refine.ProjectManager;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.WrappedCell;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.model.Project;
import com.google.refine.util.GetProjectIDException;
import com.google.refine.util.JoinException;

public class Cross implements Function {
    
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3) {
            // 1st argument can take either value or cell(for backward compatibility)
            Object v = args[0];
            Object toProjectName = args[1];
            Object toColumnName = args[2]; 
            Long toProjectID;
            ProjectJoin join;           
            
            if (v != null && 
                ( v instanceof String || v instanceof WrappedCell ) &&
                toProjectName != null && toProjectName instanceof String &&
                toColumnName != null && toColumnName instanceof String) {
                try {
                    toProjectID = ProjectManager.singleton.getProjectID((String) toProjectName);
                } catch (GetProjectIDException e){
                    return new EvalError(e.getMessage());
                }
                // add a try/catch here - error should bubble up from getInterProjectModel.computeJoin once that's modified
                try {
                    join = ProjectManager.singleton.getInterProjectModel().getJoin(
                            // getJoin(Long fromProject, String fromColumn, Long toProject, String toColumn) {
                            // source project name 
                            (Long) ((Project) bindings.get("project")).id,
                            // source column name
                            (String) bindings.get("columnName"), 
                            // target project name
                            toProjectID,
                            // target column name
                            (String) toColumnName
                            );
                } catch (JoinException e) {
                    return new EvalError(e.getMessage());
                }
                if(v instanceof String) {
                    return join.getRows(v);
                } else {
                    return join.getRows(((WrappedCell) v).cell.value);
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a string or cell, a project name to join with, and a column name in that project");
    }
    
    @Override
    public String getDescription() {
        return "join with another project by column";
    }
    
    @Override
    public String getParams() {
        return "cell c or string value, string projectName, string columnName";
    }
    
    @Override
    public String getReturns() {
        return "array";
    }
}
