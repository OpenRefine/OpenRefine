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

package com.google.refine.operations;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.util.NotImplementedException;

public final class ExpressionRenamingUtils {

    private ExpressionRenamingUtils() {
    }

    public static Optional<String> renameExpressionIfNeeded(
            String expression,
            Optional<String> baseColumnName,
            Map<String, String> newColumnNames) {
        try {
            Evaluable evaluable = MetaParser.parse(expression);
            if (!requiresExpressionRename(evaluable, baseColumnName, newColumnNames)) {
                return Optional.of(expression);
            }
            return Optional.of(evaluable.renameColumnDependencies(newColumnNames).getFullSource());
        } catch (ParsingException | NotImplementedException e) {
            return Optional.empty();
        }
    }

    private static boolean requiresExpressionRename(
            Evaluable evaluable,
            Optional<String> baseColumnName,
            Map<String, String> newColumnNames) throws ParsingException {
        Optional<Set<String>> dependencies = evaluable.getColumnDependencies(baseColumnName);
        return dependencies.isPresent() && dependencies.get().stream()
                .filter(columnName -> baseColumnName.map(base -> !base.equals(columnName)).orElse(true))
                .anyMatch(columnName -> {
                    String newName = newColumnNames.get(columnName);
                    return newName != null && !newName.equals(columnName);
                });
    }
}
