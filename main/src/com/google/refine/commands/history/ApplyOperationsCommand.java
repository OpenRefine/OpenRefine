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

package com.google.refine.commands.history;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang.Validate;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.UnknownOperation;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class ApplyOperationsCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFToken(request)) {
            respondCSRFError(response);
            return;
        }
        try {
            Project project = getProject(request);
            String jsonString = request.getParameter("operations");

            List<AbstractOperation> operations = ParsingUtilities.mapper.readValue(jsonString,
                    new TypeReference<List<AbstractOperation>>() {
                    });

            // Validate all operations first
            for (AbstractOperation operation : operations) {
                Validate.notNull(operation);
                Validate.isTrue(!(operation instanceof UnknownOperation), "Unknown operation type: " + operation.getOperationId());
                operation.validate();
            }

            // check all required columns are present
            Set<String> requiredColumns = computeRequiredColumns(operations);
            for (String columnName : requiredColumns) {
                if (project.columnModel.getColumnByName(columnName) == null) {
                    // TODO: present the user with a dialog to match all missing columns to ones that are present
                    throw new IllegalArgumentException(
                            "Column '" + columnName + "' is referenced in the list of operations but is absent from the project");
                }
            }

            // Run all operations in sequence
            for (AbstractOperation operation : operations) {
                Process process = operation.createProcess(project, new Properties());
                project.processManager.queueProcess(process);
            }

            if (project.processManager.hasPending()) {
                respond(response, "{ \"code\" : \"pending\" }");
            } else {
                respond(response, "{ \"code\" : \"ok\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    /**
     * Computes which columns are required to be present in the project before applying a list of operations. The set
     * that is returned is an under-approximation: if certain operations in the list fail to analyze their dependencies
     * or their impact on the set of columns, then some required columns will be missed by this method, resulting in an
     * error that will only be detected when the list of operations is applied.
     * 
     * @param operations
     *            the operations to analyze. If any of them is null or of an unknown type (see
     *            {@link UnknownOperation}), then {@link IllegalArgumentException} is thrown.
     * @return a set of required column names
     */
    protected static Set<String> computeRequiredColumns(List<AbstractOperation> operations) {
        // columnNames represents the current set of column names in the project,
        // after having applied the operations scanned so far. If it is empty, then
        // that means we lost track of which columns are present.
        Optional<Set<String>> currentColumnNames = Optional.of(new HashSet<>());
        // keeps track of which columns are required to exist in the project before
        // the first operation is run.
        Set<String> requiredColumnNames = new HashSet<>();

        for (AbstractOperation op : operations) {
            if (op == null) {
                throw new IllegalArgumentException("The list of operations contains 'null'");
            }
            if (op instanceof UnknownOperation) {
                throw new IllegalArgumentException("Unknown operation id: " + op.getOperationId());
            }
            op.validate();

            Optional<Set<String>> columnDependencies = op.getColumnDependencies();
            if (columnDependencies.isPresent() && currentColumnNames.isPresent()) {
                for (String columnName : columnDependencies.get()) {
                    if (!currentColumnNames.get().contains(columnName)) {
                        if (requiredColumnNames.contains(columnName)) {
                            // if this column has already been required before,
                            // but is no longer part of the current columns,
                            // that means it has been deleted since.
                            throw new IllegalArgumentException(
                                    "Inconsistent list of operations: column '" + columnName + "' used after being deleted or renamed");
                        } else {
                            requiredColumnNames.add(columnName);
                        }
                    }
                }
            }

            Optional<ColumnsDiff> columnsDiff = op.getColumnsDiff();
            if (columnsDiff.isEmpty()) {
                currentColumnNames = Optional.empty();
            } else if (currentColumnNames.isPresent()) {
                currentColumnNames.get().removeAll(columnsDiff.get().getDeletedColumns());
                currentColumnNames.get().addAll(columnsDiff.get().getAddedColumnNames());
            }
        }
        return requiredColumnNames;
    }
}
