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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.openrefine.model.changes.ChangeDataId;
import org.openrefine.process.Process.State;
import org.openrefine.util.NamingThreadFactory;

public class ProcessManager {

    @JsonIgnore
    protected List<Process> _processes = Collections.synchronizedList(new LinkedList<>());
    @JsonIgnore
    private ListeningExecutorService _executorService = MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool(new NamingThreadFactory("ProcessManager")));

    public static class ExceptionMessage {

        @JsonProperty("message")
        public final String message;

        public ExceptionMessage(Exception e) {
            message = e.getLocalizedMessage();
        }
    }

    public void shutdown() {
        _executorService.shutdown();
    }

    @JsonProperty("processes")
    public List<Process> getProcesses() {
        return _processes;
    }

    public void queueProcess(Process process) {
        _processes.add(process);

        update();
    }

    /**
     * Is there any process in the queue (potentially running)?
     */
    public boolean hasPending() {
        return _processes.size() > 0;
    }

    public void onDoneProcess(Process p) {
        _processes.remove(p);
        update();
    }

    public void cancelAll() {
        for (Process p : _processes) {
            if (p.getState() == State.RUNNING || p.getState() == State.PAUSED) {
                p.cancel();
            }
        }
        _processes.clear();
    }

    /**
     * Gets the process with the given process id.
     * 
     * @throws IllegalArgumentException
     *             if the process cannot be found
     */
    public Process getProcess(int processId) {
        Optional<Process> processOptional = _processes.stream()
                .filter(process -> process.getId() == processId)
                .findAny();
        if (processOptional.isEmpty()) {
            throw new IllegalArgumentException(String.format("Process %d not found", processId));
        }
        return processOptional.get();
    }

    /**
     * Gets any process that is fetching the supplied change data.
     * 
     * @return null if no such process can be found
     */
    public Process getProcess(ChangeDataId changeDataId) {
        return _processes.stream()
                .filter(process -> changeDataId.equals(process.getChangeDataId()))
                .findAny()
                .orElse(null);
    }

    protected void update() {
        while (_processes.size() > 0) {
            Process p = _processes.get(0);
            State state = p.getState();
            if (state == State.DONE || state == State.CANCELED) {
                _processes.remove(0);
            } else {
                if (state == State.PENDING) {
                    p.startPerforming(this);
                }
                break;
            }
        }
    }

    protected ListeningExecutorService getExecutorService() {
        return _executorService;
    }
}
