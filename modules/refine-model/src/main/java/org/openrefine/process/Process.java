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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.openrefine.model.changes.ChangeDataId;

public abstract class Process {

    @JsonProperty("description")
    final protected String _description;
    @JsonIgnore
    protected ProcessManager _manager;
    @JsonIgnore
    protected ProgressingFuture<Void> _future;
    @JsonProperty("progress")
    protected int _progress; // out of 100
    @JsonIgnore
    protected boolean _canceled;
    @JsonIgnore
    protected ProgressReporter _reporter;

    public Process(String description) {
        _description = description;
        _reporter = new Reporter();
    }

    @JsonProperty("status")
    public String getStatus() {
        return _future == null ? "pending" : (!_future.isDone() ? "running" : "done");
    }

    public boolean isRunning() {
        return _future != null && !_future.isDone();
    }

    public boolean isDone() {
        return _future != null && _future.isDone();
    }

    public boolean isPaused() {
        return _future != null && _future.isPaused();
    }

    public boolean isCanceled() {
        return _canceled;
    }

    public void startPerforming(ProcessManager manager) {
        if (_future == null) {
            _manager = manager;

            _future = getFuture();
            FutureCallback<Void> callback = new FutureCallback<>() {

                @Override
                public void onSuccess(Void result) {
                    _manager.onDoneProcess(Process.this);
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t instanceof Exception) {
                        _manager.onFailedProcess(Process.this, (Exception) t);
                    }
                }
            };
            Futures.addCallback(_future, callback, _manager._executorService);
        }
    }

    public void cancel() {
        _canceled = true;
        if (_future != null) {
            _future.cancel(true);
        }
    }

    /**
     * Pauses this process. This may or may not be actually respected by the underlying future as support depends on the
     * runner.
     */
    public void pause() {
        if (_future != null) {
            _future.pause();
        }
    }

    /**
     * Resumes this process if it was paused.
     */
    public void resume() {
        if (_future != null) {
            _future.resume();
        }
    }

    @JsonProperty("id")
    public long getId() {
        return hashCode();
    }

    @JsonProperty("changeDataId")
    abstract public ChangeDataId getChangeDataId();

    abstract protected ProgressingFuture<Void> getFuture();

    protected class Reporter implements ProgressReporter {

        @Override
        public void reportProgress(int percentage) {
            _progress = percentage;
        }

    }
}
