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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.openrefine.model.changes.ChangeDataId;
import org.openrefine.operations.exceptions.ChangeDataFetchingException;

public abstract class Process {

    public enum State {
        @JsonProperty("pending")
        PENDING, @JsonProperty("running")
        RUNNING, @JsonProperty("done")
        DONE, @JsonProperty("failed")
        FAILED, @JsonProperty("paused")
        PAUSED, @JsonProperty("canceled")
        CANCELED
    };

    @JsonIgnore
    final protected String _description;
    @JsonIgnore
    protected ProcessManager _manager;
    @JsonIgnore
    protected ProgressingFuture<Void> _future;
    @JsonIgnore
    protected int _progress; // out of 100
    @JsonIgnore
    protected boolean _canceled;
    @JsonIgnore
    protected Exception _exception;
    @JsonIgnore
    protected ProgressReporter _reporter;

    public Process(String description) {
        _description = description;
        _reporter = new Reporter();
    }

    @JsonProperty("state")
    public State getState() {
        if (_canceled) {
            return State.CANCELED;
        } else if (_exception != null) {
            return State.FAILED;
        } else if (_future == null) {
            return State.PENDING;
        } else if (_future.isDone()) {
            return State.DONE;
        } else if (_future.isPaused()) {
            return State.PAUSED;
        } else {
            return State.RUNNING;
        }
    }

    @JsonIgnore
    public boolean isCanceled() {
        return _canceled;
    }

    public void startPerforming(ProcessManager manager) {
        if (_future == null) {
            _manager = manager;
            _exception = null;

            try {
                _future = getFuture();
                FutureCallback<Void> callback = new FutureCallback<>() {

                    @Override
                    public void onSuccess(Void result) {
                        _manager.onDoneProcess(Process.this);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (t instanceof Exception) {
                            t.printStackTrace();
                            _exception = (Exception) t;
                        }
                    }
                };
                Futures.addCallback(_future, callback, _manager.getExecutorService());
            } catch (ChangeDataFetchingException exception) {
                _exception = exception;
            }
        }
    }

    @JsonProperty("errorMessage")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getErrorMessage() {
        if (_exception != null) {
            if (_exception instanceof ChangeDataFetchingException) {
                return _exception.getMessage();
            } else {
                return _exception.toString();
            }
        }
        return null;
    }

    @JsonProperty("description")
    public String getDescription() {
        return _description;
    }

    @JsonProperty("progress")
    public int getProgress() {
        return _progress;
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
