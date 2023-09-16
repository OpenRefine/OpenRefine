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

package org.openrefine.process;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.History;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.changes.Change;
import org.openrefine.operations.Operation;

public class QuickHistoryEntryProcess extends Process {

    final protected History _history;
    final protected String _briefDescription;
    final protected Operation _operation;
    final protected Change _change;
    protected HistoryEntry _historyEntry;
    boolean _done = false;

    public QuickHistoryEntryProcess(History history, String briefDescription, Operation operation, Change change) {
        _history = history;
        _operation = operation;
        _briefDescription = briefDescription;
        _change = change;
    }

    @Override
    public void cancel() {
        throw new RuntimeException("Not a long-running process");
    }

    @Override
    @JsonProperty("immediate")
    public boolean isImmediate() {
        return true;
    }

    @Override
    public boolean isRunning() {
        throw new RuntimeException("Not a long-running process");
    }

    @Override
    public HistoryEntry performImmediate() throws Exception {
        _historyEntry = _history.addEntry(HistoryEntry.allocateID(), _briefDescription, _operation, _change);
        _done = true;

        return _historyEntry;
    }

    @Override
    public void startPerforming(ProcessManager manager) {
        throw new RuntimeException("Not a long-running process");
    }

    @JsonProperty("status")
    public String getStatus() {
        return _done ? "done" : "pending";
    }

    @JsonProperty("description")
    public String getDescription() {
        return _historyEntry != null ? _historyEntry.getDescription() : _briefDescription;
    }

    @Override
    public boolean isDone() {
        return _done;
    }
}
