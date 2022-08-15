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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.history.HistoryEntry;
import com.google.refine.history.HistoryProcess;

public class ProcessManager {

    @JsonProperty("processes")
    protected List<Process> _processes = Collections.synchronizedList(new LinkedList<Process>());
    @JsonIgnore
    protected List<Exception> _latestExceptions = null;

    public static class ExceptionMessage {

        @JsonProperty("message")
        public final String message;

        public ExceptionMessage(Exception e) {
            message = e.getLocalizedMessage();
        }
    }

    public ProcessManager() {

    }

    @JsonProperty("exceptions")
    @JsonInclude(Include.NON_NULL)
    public List<ExceptionMessage> getJsonExceptions() {
        if (_latestExceptions != null) {
            return _latestExceptions.stream()
                    .map(e -> new ExceptionMessage(e))
                    .collect(Collectors.toList());
        }
        return null;
    }

    public HistoryEntry queueProcess(Process process) throws Exception {
        if (process.isImmediate() && _processes.size() == 0) {
            _latestExceptions = null;
            return process.performImmediate();
        } else {
            _processes.add(process);

            update();
        }
        return null;
    }

    public boolean queueProcess(HistoryProcess process) throws Exception {
        if (process.isImmediate() && _processes.size() == 0) {
            _latestExceptions = null;
            return process.performImmediate() != null;
        } else {
            _processes.add(process);

            update();
        }
        return false;
    }

    public boolean hasPending() {
        return _processes.size() > 0;
    }

    public void onDoneProcess(Process p) {
        _processes.remove(p);
        update();
    }

    public void onFailedProcess(Process p, Exception exception) {
        List<Exception> exceptions = new LinkedList<Exception>();
        exceptions.add(exception);
        onFailedProcess(p, exceptions);
    }

    public void onFailedProcess(Process p, List<Exception> exceptions) {
        _latestExceptions = exceptions;
        _processes.remove(p);
        // Do not call update(); Just pause?
    }

    public void cancelAll() {
        for (Process p : _processes) {
            if (!p.isImmediate() && p.isRunning()) {
                p.cancel();
            }
        }
        _processes.clear();
        _latestExceptions = null;
    }

    protected void update() {
        while (_processes.size() > 0) {
            Process p = _processes.get(0);
            if (p.isImmediate()) {
                _latestExceptions = null;
                try {
                    p.performImmediate();
                } catch (Exception e) {
                    // TODO: Not sure what to do yet
                    e.printStackTrace();
                }
                _processes.remove(0);
            } else if (p.isDone()) {
                _processes.remove(0);
            } else {
                if (!p.isRunning()) {
                    _latestExceptions = null;
                    p.startPerforming(this);
                }
                break;
            }
        }
    }
}
