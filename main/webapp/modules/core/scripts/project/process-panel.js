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

function ProcessPanel(div) {
    this._div = div;
    this._timerID = null;
    this._processCount = 0;
    
    this._updateOptions = {};
    this._onDones = [];
    this._latestHistoryEntry = null;
    
    var self = this;
    $(window).keypress(function(evt) {
        if (evt.charCode == 26 || (evt.charCode == 122 && (evt.ctrlKey || evt.metaKey))) { // ctrl-z or meta-z
            var t = evt.target;
            if (t) {
                var tagName = t.tagName.toLowerCase();
                if (tagName == "textarea" || tagName == "input") {
                    return;
                }
            }
            self.undo();
        }
    });
    
    this.update({});
}

ProcessPanel.prototype.resize = function() {
};

ProcessPanel.prototype.update = function(updateOptions, onDone) {
    this._latestHistoryEntry = null;
    
    for (var n in updateOptions) {
        if (updateOptions.hasOwnProperty(n)) {
            this._updateOptions[n] = updateOptions[n];
        }
    }
    if (onDone) {
        this._onDones.push(onDone);
    }
    
    if (this._timerID !== null) {
        return;
    }
    
    var self = this;
    Ajax.chainGetJSON(
        "/command/core/get-processes?" + $.param({ project: theProject.id }), null,
        function(data) {
            self._latestHistoryEntry = null;
            self._render(data);
        }
    );
};

ProcessPanel.prototype.showUndo = function(historyEntry) {
    var self = this;
    
    this._latestHistoryEntry = historyEntry;

    this._div.stop(true,false)
    .empty()
    .html('<div id="notification"><span bind="description"></span> <span class="notification-action"><a href="javascript:{}" bind="undo">Undo</a></span></div>')
    .fadeIn(200)
    .delay(7500)
    .fadeOut(200);
    var elmts = DOM.bind(this._div);
        
    elmts.description.text(historyEntry.description);
    elmts.undo.click(function() { self.undo(); });
};

ProcessPanel.prototype.undo = function() {
    if (this._latestHistoryEntry !== null) {
        Refine.postCoreProcess(
            "undo-redo",
            { undoID: this._latestHistoryEntry.id },
            null,
            { everythingChanged: true }
        );
    }
};

ProcessPanel.prototype._cancelAll = function() {
    var self = this;
    $.post(
        "/command/core/cancel-processes?" + $.param({ project: theProject.id }), 
        null,
        function(o) {
            self._data = null;
            self._runOnDones();
        },
        "json"
    );
};

ProcessPanel.prototype._render = function(newData) {
    var self = this;
    var newProcessMap = {};
    
    this._div.stop(true, false).empty();
    
    if (!newData.processes.length) {
        Refine.setTitle();
        this._div.fadeOut(200);
    } else {
        this._div.fadeIn(200);
        var cancelmessage = "Cancel"
        var noticeDiv = $('<div id="notification"></div>').appendTo(this._div);
        var descriptionSpan = $('<span></span>').appendTo(noticeDiv);
        var statusDiv = $('<div></div>').appendTo(noticeDiv);
        $('<img src="images/small-spinner.gif" />')
            .addClass("notification-loader")
            .appendTo(statusDiv);
        var progressSpan = $('<span></span>').appendTo(statusDiv);
        var countSpan = $('<span></span>').appendTo(statusDiv);
        var renderProcess = function(process) {
          if (process.status != "pending") {
            Refine.setTitle(process.progress + "% complete");
            descriptionSpan.text(process.description);
            progressSpan.text(process.progress  + '% complete');
          }
        };
        var renderProcessCount = function(count) {
          var pendingcount = count - 1;
          countSpan.text(', ' + pendingcount + ' pending processes');
        };
        var processes = newData.processes;
        if (processes.length >> 1) {
          cancelmessage = "Cancel All";
          renderProcessCount(processes.length);
        }
        for (var i = 0; i < processes.length; i++) {
            var process = processes[i];
            renderProcess(process);
            if ("onDone" in process) {
                newProcessMap[process.id] = process;
            }
        }
        $('<a href="javascript:{}"></a>')
          .addClass("notification-action")
          .text(cancelmessage)
          .click(function() {
              self._cancelAll();
              $(this).text("Canceling...").unbind();
          })
          .appendTo(statusDiv);
    }
    
    if ((this._data) && this._data.processes.length > 0) {
        var oldProcesses = this._data.processes;
        for (var i = 0; i < oldProcesses.length; i++) {
            var process = oldProcesses[i];
            if ("onDone" in process && !(process.id in newProcessMap)) {
                this._perform(process.onDone);
            }
        }
    }
    this._data = newData;
    
    if (this._data.processes.length && !this._timerID) {
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self.update();
        }, 500);
    } else {
        this._runOnDones();
    }
};

ProcessPanel.prototype._perform = function(jobs) {
    for (var i = 0; i < jobs.length; i++) {
        var job = jobs[i];
        if (job.action == "createFacet") {
            try {
                ui.browsingEngine.addFacet(
                    job.facetType,
                    job.facetConfig,
                    job.facetOptions
                );
            } catch (e) {
                //
            }
        }
    }
};

ProcessPanel.prototype._runOnDones = function() {
    var updateOptions = this._updateOptions;
    var onDones = this._onDones;
    
    this._updateOptions = {};
    this._onDones = [];
    
    Refine.update(updateOptions, function() {
        for (var i = 0; i < onDones.length; i++) {
            try {
                onDones[i]();
            } catch (e) {
                Refine.reportException(e);
            }
        }
    });
};
