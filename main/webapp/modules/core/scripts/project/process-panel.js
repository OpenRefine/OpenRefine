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
  
  this._div.html(DOM.loadHTML("core", "scripts/project/progress-panel.html"));
  this._elmts = DOM.bind(this._div);

  this._elmts.undoLink.html($.i18n('core-project/undo'));
  
  var self = this;
  $(window).on('keypress',function(evt) {
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
      "command/core/get-processes?" + $.param({ project: theProject.id }), null,
      function(data) {
        self._latestHistoryEntry = null;
        self._render(data);
      }
  );
};

ProcessPanel.prototype.showUndo = function(historyEntry) {
  var self = this;

  this._latestHistoryEntry = historyEntry;

  truncDescription = historyEntry.description.length > 250 ?
  	historyEntry.description.substring(0, 250) + " ..." : historyEntry.description  

  this._div.stop(true, false);
  this._elmts.progressDiv.hide();
  this._elmts.undoDiv.show();
  this._elmts.undoDescription.text( truncDescription );
  this._elmts.undoLink.off().on('click',function() { self.undo(); });
  
  this._div
    .fadeIn(200)
    .delay(10000)
    .fadeOut(200);
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
  Refine.postCSRF(
    "command/core/cancel-processes?" + $.param({ project: theProject.id }), 
    { },
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
  var processes = newData.processes;

  this._div.stop(true, false);

  if (!processes.length) {
    Refine.setTitle();
    this._div.fadeOut(200);
  } else {
    this._elmts.undoDiv.hide();
    this._elmts.progressDiv.show();
    
    for (var i = 0; i < processes.length; i++) {
      var process = processes[i];
      if (process.status != "pending") {
        // TODO: We should be using formatting, not string concatenation here
        Refine.setTitle($.i18n('core-project/percent-complete', process.progress));
        this._elmts.progressDescription.text(process.description);
        this._elmts.progressSpan.text($.i18n('core-project/percent-complete', process.progress));
      }
      if ("onDone" in process) {
        newProcessMap[process.id] = process;
      }
    }
    
    if (processes.length > 1) {
      var pending = processes.length - 1;
      this._elmts.countSpan.text($.i18n('core-project/other-processes', pending));
    } else {
      this._elmts.countSpan.empty();
    }
    this._elmts.cancelLink
      .off()
      .text($.i18n('core-project/cancel-all', processes.length))
      .on('click',function() {
        self._cancelAll();
        $(this).text($.i18n('core-project/canceling')).off();
      });
    
    this._div.fadeIn(200);
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
  
  if (this._data.exceptions) {
    var messages = $.map(this._data.exceptions, function(e) {
      return e.message;
    }).join('\n');
    
    if (this._data.processes.length == 0) {
      window.alert($.i18n('core-project/last-op-er')+'\n' + messages);
    } else {
      if (window.confirm($.i18n('core-project/last-op-er')+'\n' + messages +
            '\n\n'+$.i18n('core-project/continue-remaining'))) {
        Refine.postCSRF(
          "command/core/apply-operations?" + $.param({ project: theProject.id }), 
          { operations: '[]' },
          function(o) {},
          "json"
        );
      } else {
        self._cancelAll();
      }
    }
  }
  
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
    } else if (job.action == "open") {
      var url = 'http://' + window.location.host + ModuleWirings[job.module] + job.path + '?' + job.params;
      window.open(url, 'new');
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
