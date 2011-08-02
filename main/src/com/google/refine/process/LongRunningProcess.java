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

abstract public class LongRunningProcess extends Process {
    final protected String       _description;
    protected ProcessManager     _manager;
    protected Thread             _thread;
    protected int                _progress; // out of 100
    protected boolean            _canceled;
    
    protected LongRunningProcess(String description) {
        _description = description;
    }

    @Override
    public void cancel() {
        _canceled = true;
        if (_thread != null && _thread.isAlive()) {
            _thread.interrupt();
        }
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("id"); writer.value(hashCode());
        writer.key("description"); writer.value(_description);
        writer.key("immediate"); writer.value(false);
        writer.key("status"); writer.value(_thread == null ? "pending" : (_thread.isAlive() ? "running" : "done"));
        writer.key("progress"); writer.value(_progress);
        writer.endObject();
    }

    @Override
    public boolean isImmediate() {
        return false;
    }
    
    @Override
    public boolean isRunning() {
        return _thread != null && _thread.isAlive();
    }
    
    @Override
    public boolean isDone() {
        return _thread != null && !_thread.isAlive();
    }

    @Override
    public HistoryEntry performImmediate() {
        throw new RuntimeException("Not an immediate process");
    }

    @Override
    public void startPerforming(ProcessManager manager) {
        if (_thread == null) {
            _manager = manager;
            
            _thread = new Thread(getRunnable());
            _thread.start();
        }
    }
    
    abstract protected Runnable getRunnable();
}
