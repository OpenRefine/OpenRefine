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

import com.google.refine.LookupCacheManager.ProjectLookup;
import com.google.refine.ProjectManager;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.WrappedCell;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.GetProjectIDException;
import com.google.refine.util.LookupException;

public class Cross implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3) {
            // 1st argument can take either value or cell(for backward compatibility)
            Object v = args[0];
            Object targetProjectName = args[1];
            Object targetColumnName = args[2];
            long targetProjectID;
            ProjectLookup lookup;

            if (v != null && targetProjectName instanceof String && targetColumnName instanceof String) {
                try {
                    targetProjectID = ProjectManager.singleton.getProjectID((String) targetProjectName);
                } catch (GetProjectIDException e) {
                    return new EvalError(e.getMessage());
                }

                try {
                    lookup = ProjectManager.singleton.getLookupCacheManager().getLookup(targetProjectID, (String) targetColumnName);
                } catch (LookupException e) {
                    return new EvalError(e.getMessage());
                }

                if (v instanceof WrappedCell) {
                    return lookup.getRows(((WrappedCell) v).cell.value);
                } else {
                    return lookup.getRows(v);
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a cell or value, a project name to look up, and a column name in that project");
    }

    @Override
    public String getDescription() {
        return "Looks up the given value in the target column of the target project, returns an array of matched rows, cell will be interpreted as cell.value. Two values match if and only if they have the same string representation";
    }

    @Override
    public String getParams() {
        return "cell or value, string projectName, string columnName";
    }

    @Override
    public String getReturns() {
        return "array";
    }
}
