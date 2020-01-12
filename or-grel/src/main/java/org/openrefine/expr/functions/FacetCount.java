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

package org.openrefine.expr.functions;

import java.util.Properties;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.util.ExpressionNominalValueGrouper;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.ControlFunctionRegistry;
import org.openrefine.grel.Function;
import org.openrefine.model.Column;
import org.openrefine.model.Project;

public class FacetCount implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 3 && args[1] instanceof String && args[2] instanceof String) {
            Object choiceValue = args[0]; // choice value to look up
            String facetExpression = (String) args[1];
            String columnName = (String) args[2];

            Project project = (Project) bindings.get("project");
            Column column = project.columnModel.getColumnByName(columnName);
            if (column == null) {
                return new EvalError("No such column named " + columnName);
            }

            String key = "nominal-bin:" + facetExpression;
            ExpressionNominalValueGrouper grouper = null;
            // TODO to migrate
            if (grouper == null) {
                try {
                    Evaluable eval = MetaParser.parse(facetExpression);
                    Engine engine = new Engine(project);

                    grouper = new ExpressionNominalValueGrouper(eval, columnName, column.getCellIndex());
                    engine.getAllRows().accept(project, grouper);
                } catch (ParsingException e) {
                    return new EvalError("Error parsing facet expression " + facetExpression);
                }
            }

            return grouper.getChoiceValueCountMultiple(choiceValue);
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) +
                " expects a choice value, an expression as a string, and a column name");
    }

    @Override
    public String getDescription() {
        return "Returns the facet count corresponding to the given choice value";
    }

    @Override
    public String getParams() {
        return "choiceValue, string facetExpression, string columnName";
    }

    @Override
    public String getReturns() {
        return "number";
    }
}
