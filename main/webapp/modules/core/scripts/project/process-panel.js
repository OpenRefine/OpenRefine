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

function ProcessPanel(notificationsContainer, processPanel, tabHeader) {
  this._notificationsContainer = notificationsContainer;
  this._processPanel = processPanel;
  this._tabHeader = tabHeader;
  this._timerID = null;
  this._processCount = 0;

  this._latestHistoryEntry = null;
  
  this._notificationsContainer.html(DOM.loadHTML("core", "scripts/project/notifications-area.html"));
  this._processPanel.html(DOM.loadHTML("core", "scripts/project/process-panel.html"));
  this._elmts = DOM.bind(this._notificationsContainer);
  this._panelElmts = DOM.bind(this._processPanel);

  this._elmts.undoLink.html($.i18n('core-project/undo'));
  $('<p></p>').text($.i18n('core-processes/no-process'))
        .appendTo(this._panelElmts.noProcessDiv);
  
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

  this.update();
}

ProcessPanel.prototype.resize = function() {
};

ProcessPanel.prototype._renderPanel = function(newData) {
  var self = this;

  // mark processes as stale to keep track of which ones we are updating
  self._panelElmts.processes.find('li').addClass('stale-process');
  let oldProcessFound = self._panelElmts.processes.find('li').length > 0;
  let newProcessFound = false;

  if (newData.processes && newData.processes.length > 0) {
    self._panelElmts.noProcessDiv.hide();
    for (let process of newData.processes) {
      var li = $('#process-' + process.id);
      li.removeClass('stale-process');
      if (!li.length) {
        // this process is new, we create the UI for it
        newProcessFound = true;
        li = $('<li></li>')
            .attr('id', 'process-' + process.id)
            .appendTo(self._panelElmts.processes);
        var title = $('<div></div>')
            .addClass('process-header')
            .text(process.description)
            .appendTo(li);
        var processBody = $('<div></div>')
            .addClass('process-body')
            .appendTo(li);
        var progressContainer = $('<div></div>')
            .addClass('process-progress-container')
            .appendTo(processBody);
        var spinner = $('<img />')
            .addClass('notification-loader')
            .attr('src', 'images/small-spinner.gif')
            .attr('alt', $.i18n('core-processes/spinner-alt-text'))
            .appendTo(progressContainer);
        var progressPercent = $('<span></span>')
            .addClass('progress-text')
            .appendTo(progressContainer);
        var progressBar = $('<div></div>')
            .addClass('process-progress-bar')
            .appendTo(progressContainer);
        var progressBarInner = $('<div></div>')
            .addClass('process-progress-bar-inner')
            .appendTo(progressBar);
                var buttonsContainer = $('<div></div>')
            .addClass('process-buttons-container')
            .appendTo(processBody);
        var pauseButton = $('<button></button>')
            .addClass('button')
            .addClass('pause-button')
            .attr('type', 'button')
            .appendTo(buttonsContainer);
        var cancelButton = $('<button></button>')
            .addClass('button')
            .attr('type', 'button')
            .text($.i18n('core-buttons/cancel'))
            .appendTo(buttonsContainer);

        pauseButton.on('click',
          function (evt) {
            let clicked = $(this);
            clicked
                .attr('disabled', true)
                .addClass('disabled');
            var paused = clicked.data('paused');
            Refine.postCSRF(
              paused ? "command/core/resume-process" : "command/core/pause-process",
              {
                project: theProject.id,
                id: process.id
              },
              function(response) { 
                clicked.attr('disabled', false).removeClass('disabled');
                if (response.code === 'ok') {
                  clicked
                      .data('paused', !process.paused)
                      .text(!process.paused ? $.i18n('core-processes/resume') : $.i18n('core-processes/pause'));
                }
              },
              "json");
        });

        cancelButton.on('click',
          function (evt) {
            cancelButton
                .attr('disabled', true)
                .addClass('disabled');
            Refine.postCSRF(
              "command/core/cancel-process",
              {
                project: theProject.id,
                id: process.id
              },
              function(response) { 
                cancelButton.attr('disabled', false).removeClass('disabled');
                if (response.code === 'ok') {
                  li.remove();
                }
                if (response.newHistoryEntryId !== undefined) {
                  Refine.update({ everythingChanged: true });
                }
              },
              "json");
        });
      }

      li.find('.process-progress-container span')
          .text($.i18n('core-project/percent-complete', process.progress));
      li.find('.pause-button')
          .data('paused', process.paused)
          .text(process.paused ? $.i18n('core-processes/resume') : $.i18n('core-processes/pause'));
      li.find('.process-progress-bar-inner')
          .width(process.progress + '%');
      if (process.running) {
        li.find('.pause-button').show();
      } else {
        li.find('.pause-button').hide();
      }
      let spinnerElement = li.find('.notification-loader');
      if (!process.paused && process.running) {
        spinnerElement.show();
      } else {
        spinnerElement.hide();
      }

    }
  } else {
    self._panelElmts.noProcessDiv.show();
    self._panelElmts.processes.empty();
    // if there was at least a process before and there aren't any, and there are active facets, switch to the facets tab
    if (self._panelElmts.noProcessDiv.is(':visible') && ui.browsingEngine.getFacetUIStates().length > 0 && oldProcessFound) {
      Refine.activateLeftPanelTab('facets');
    }
  }

  // clean up any existing processes which do not exist anymore
  self._panelElmts.processes.find('li.stale-process').remove();

  // update the tab header
  self._tabHeader.empty();
  self._tabHeader.text($.i18n('core-project/processes')+' ');
  if (newData.processes.length) {
    $('<span></span>')
        .text(newData.processes.length)
        .addClass('count')
        .appendTo(self._tabHeader);
  }

  // if some processes are new, we focus the view to this tab
  if (newProcessFound) {
    Refine.activateLeftPanelTab('process');
  }
};

ProcessPanel.prototype.update = function(onDone, onError) {
  if (this._timerID !== null) {
    if (onDone) {
      onDone();
    }
    return;
  }

  var self = this;
  $.ajax({
    dataType: 'json',
    url: "command/core/get-processes?" + $.param({ project: theProject.id }),
    data: null,
  }).done(function(data) {
      self._render(data);
      if (onDone) {
        onDone();
      }
    }
  ).fail(function (xhr, reqStatus, httpStatus) {
    if (onError) {
      onError('failed to refresh the process panel');
    }
  });
};

ProcessPanel.prototype.showUndo = function(historyEntry) {
  var self = this;

  this._latestHistoryEntry = historyEntry;

  truncDescription = historyEntry.description.length > 250 ?
  	historyEntry.description.substring(0, 250) + " ..." : historyEntry.description  

  this._notificationsContainer.stop(true, false);
  this._elmts.progressDiv.hide();
  this._elmts.undoDiv.show();
  this._elmts.undoDescription.text( truncDescription );
  this._elmts.undoLink.off().on('click',function() { self.undo(); });
  
  this._notificationsContainer
    .fadeIn(200)
    .delay(10000)
    .fadeOut(200);
};

ProcessPanel.prototype.undo = function() {
  if (this._latestHistoryEntry !== null) {
    var updateOptions = { everythingChanged: true, warnAgainstHistoryErasure: false };
    Refine.postCoreProcess(
        "undo-redo",
        { undoID: this._latestHistoryEntry.id },
        null,
        updateOptions,
        { onDone: function(o) {
          updateOptions.rowIdsPreserved = o.gridPreservation !== 'no-row-preservation';
          updateOptions.recordIdsPreserved = o.gridPreservation === 'preserves-records';
        }}
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
    },
    "json"
  );
};

ProcessPanel.prototype._render = function(newData) {
  var self = this;
  self._renderPanel(newData);
  self._data = newData;

  // schedule next update
  if (this._data.processes.length && !this._timerID) {
    this._timerID = window.setTimeout(function() {
      self._timerID = null;
      self.update();
    }, 500);
  }
};

