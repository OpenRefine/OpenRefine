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

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.util.ExpressionNominalValueGrouper;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

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
                return new EvalError(EvalErrorMessage.no_such_column_with_name(columnName));
            }

            String key = "nominal-bin:" + facetExpression;
            ExpressionNominalValueGrouper grouper = (ExpressionNominalValueGrouper) column.getPrecompute(key);
            if (grouper == null) {
                try {
                    Evaluable eval = MetaParser.parse(facetExpression);
                    Engine engine = new Engine(project);

                    grouper = new ExpressionNominalValueGrouper(eval, columnName, column.getCellIndex());
                    engine.getAllRows().accept(project, grouper);

                    column.setPrecompute(key, grouper);
                } catch (ParsingException e) {
                    return new EvalError(EvalErrorMessage.fun_facet_count_error_parsing_facet(facetExpression));
                }
            }

            return grouper.getChoiceValueCountMultiple(choiceValue);
        }
        // " expects a choice value, an expression as a string, and a column name");
        return new EvalError(EvalErrorMessage.fun_facet_expects_value_expression_column(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.fun_facet_count();
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
