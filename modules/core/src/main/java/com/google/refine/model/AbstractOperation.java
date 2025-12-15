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

package com.google.refine.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.OperationResolver;
import com.google.refine.process.Process;
import com.google.refine.process.QuickHistoryEntryProcess;

/**
 * An operation can be applied to different but similar projects. Instances of this class store the metadata describing
 * the operation before it is carried out, such as the names of the columns it applies to, the configuration of any
 * facets to be observed by the operation, and so on. Any data obtained from executing the operation on the project
 * should be stored in a {@link Change} instead.
 * 
 * Operations need to be (de)serializable in JSON via Jackson, used for project persistence and in the extract/apply
 * dialog of the history panel. Validation of the operation's parameters should not happen during deserialization, but
 * in the {@link #validate()} method.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "op", visible = true)
@JsonTypeIdResolver(OperationResolver.class)
abstract public class AbstractOperation {

    /**
     * Checks whether the parameters of this operation are suitably filled. Those checks should not happen in the
     * deserialization constructor as it would risk rejecting JSON certain representations at project loading time.
     * 
     * @throws IllegalArgumentException
     *             if any parameter is missing or inconsistent
     */
    public void validate() throws IllegalArgumentException {

    }

    public Process createProcess(Project project, Properties options) throws Exception {
        return new QuickHistoryEntryProcess(project, getBriefDescription(null)) {

            @Override
            protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
                return AbstractOperation.this.createHistoryEntry(_project, historyEntryID);
            }
        };
    }

    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected String getBriefDescription(Project project) {
        throw new UnsupportedOperationException();
    }

    @JsonIgnore // the operation id is already added as "op" by the JsonTypeInfo annotation
    public String getOperationId() {
        return OperationRegistry.s_opClassToName.get(this.getClass());
    }

    @JsonProperty("description")
    public String getJsonDescription() {
        return getBriefDescription(null);
    }

    /**
     * A set of columns required by this operation to run. If present, the operation is guaranteed to be able to run on
     * any project which has at least those columns. If equal to {@link Optional#empty()} the operation could
     * potentially depend on any column.
     */
    @JsonIgnore
    public Optional<Set<String>> getColumnDependencies() {
        return Optional.empty();
    }

    /**
     * If the effect of the operation on the set of columns in the project is predictable, this effect can be exposed in
     * this method. Otherwise, {@link Optional#empty()} can be returned.
     */
    @JsonIgnore
    public Optional<ColumnsDiff> getColumnsDiff() {
        return Optional.empty();
    }

    /**
     * Compute a new version of this operation metadata, with renamed columns. This is a best-effort transformation:
     * some column references might fail to be updated, for instance if they are embedded into expressions that cannot
     * be fully analyzed. As a fall-back solution, the same operation can be returned.
     * 
     * @param newColumnNames
     *            a map from old to new column names
     * @return the updated operation metadata
     */
    public AbstractOperation renameColumns(Map<String, String> newColumnNames) {
        return this;
    }

    /**
     * Determine whether this operation relies on any of the input column names
     *
     * @param columnNames a list of column names
     * @return boolean
     */
    public boolean dependsOnAny(Set<String> columnNames) {
        Set<String> allDependencies = new HashSet<>();

        Optional<Set<String>> columnDependencies = this.getColumnDependencies();
        if (columnDependencies.isPresent()) {
            allDependencies.addAll(columnDependencies.get());
        }

        Optional<ColumnsDiff> columnsDiff = this.getColumnsDiff();
        if (columnsDiff.isPresent()) {
            allDependencies.addAll(columnsDiff.get().getImpliedDependencies());
        }

        allDependencies.retainAll(columnNames);
        return !allDependencies.isEmpty();
    }
}
