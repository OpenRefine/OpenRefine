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

package com.google.refine.process;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;

abstract public class QuickHistoryEntryProcess extends Process {
    final protected Project _project;
    final protected String _briefDescription;
    protected HistoryEntry _historyEntry;
    boolean _done = false;
    
    public QuickHistoryEntryProcess(Project project, String briefDescription) {
        _project = project;
        _briefDescription = briefDescription;
    }
    
    @Override
    public void cancel() {
        throw new RuntimeException("Not a long-running process");
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
    
    @Override
    public boolean isRunning() {
        throw new RuntimeException("Not a long-running process");
    }

    @Override
    public HistoryEntry performImmediate() throws Exception {
        if (_historyEntry == null) {
            _historyEntry = createHistoryEntry(HistoryEntry.allocateID());
        }
        _project.history.addEntry(_historyEntry);
        _done = true;
        
        return _historyEntry;
    }

    @Override
    public void startPerforming(ProcessManager manager) {
        throw new RuntimeException("Not a long-running process");
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("id"); writer.value(hashCode());
        writer.key("description"); writer.value(_historyEntry != null ? _historyEntry.description : _briefDescription);
        writer.key("immediate"); writer.value(true);
        writer.key("status"); writer.value(_done ? "done" : "pending");
        writer.endObject();
    }


    @Override
    public boolean isDone() {
        return _done;
    }
    
    abstract protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception;
}
