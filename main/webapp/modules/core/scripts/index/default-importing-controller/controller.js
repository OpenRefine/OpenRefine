/*

Copyright 2011, Google Inc.
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

Refine.DefaultImportingController = function(createProjectUI) {
  this._createProjectUI = createProjectUI;

  this._fileSelectionPanel = createProjectUI.addCustomPanel();
  this._parsingPanel = createProjectUI.addCustomPanel();

  for (var i = 0; i < Refine.DefaultImportingController.sources.length; i++) {
    var sourceSelectionUI = Refine.DefaultImportingController.sources[i];
    sourceSelectionUI.ui = new sourceSelectionUI.uiClass(this);

    createProjectUI.addSourceSelectionUI(sourceSelectionUI);
  }
};
Refine.CreateProjectUI.controllers.push(Refine.DefaultImportingController);

Refine.DefaultImportingController.sources = [];
Refine.DefaultImportingController.parserUIs = {};

Refine.DefaultImportingController.prototype._startOver = function() {
  if (this._jobID) {
    Refine.CreateProjectUI.cancelImportingJob(this._jobID);
  }
  
  this._disposeFileSelectionPanel();
  this._disposeFileSelectionPanel();

  delete this._fileSelectionPanelElmts;
  delete this._parsingPanelElmts;

  delete this._jobID;
  delete this._job;
  delete this._extensions;

  delete this._format;
  delete this._parserOptions;
  delete this._projectName;

  this._createProjectUI.showSourceSelectionPanel();
};

Refine.DefaultImportingController.prototype.startImportJob = function(form, progressMessage, callback) {
  var self = this;
  
  $(form).find('input:text').filter(function() { 
		return this.value === ""; 
  }).attr("disabled", "disabled");
  
  $.post(
      "command/core/create-importing-job",
      null,
      function(data) {
        var jobID = self._jobID = data.jobID;

        form.attr("method", "post")
        .attr("enctype", "multipart/form-data")
        .attr("accept-charset", "UTF-8")
        .attr("target", "create-project-iframe")
        .attr("action", "command/core/importing-controller?" + $.param({
          "controller": "core/default-importing-controller",
          "jobID": jobID,
          "subCommand": "load-raw-data"
        }));
        form[0].submit();

        var start = new Date();
        var timerID = window.setInterval(
          function() {
            self._createProjectUI.pollImportJob(
              start, jobID, timerID,
              function(job) {
                return job.config.hasData;
              },
              function(jobID, job) {
                self._job = job;
                self._onImportJobReady();
                if (callback) {
                  callback(jobID, job);
                }
              },
              function(job) {
                alert(job.config.error + '\n' + job.config.errorDetails);
                self._startOver();
              }
            );
          },
          1000
        );
        self._createProjectUI.showImportProgressPanel(progressMessage, function() {
          // stop the iframe
          $('#create-project-iframe')[0].contentWindow.stop();

          // stop the timed polling
          window.clearInterval(timerID);

          // explicitly cancel the import job
          Refine.CreateProjectUI.cancelImportingJob(jobID);

          self._createProjectUI.showSourceSelectionPanel();
        });
      },
      "json"
  );
};

Refine.DefaultImportingController.prototype._onImportJobReady = function() {
  this._prepareData();
  if (this._job.config.retrievalRecord.files.length > 1) {
    this._showFileSelectionPanel();
  } else {
    this._showParsingPanel(false);
  }
};

Refine.DefaultImportingController.prototype._prepareData = function() {
  var extensionMap = {};
  var extensionList = [];

  var files = this._job.config.retrievalRecord.files;
  var fileSelection = this._job.config.fileSelection;
  for (var i = 0; i < files.length; i++) {
    var file = files[i];
    file.selected = false;

    var slash = file.fileName.lastIndexOf('/');
    var dot = file.fileName.lastIndexOf('.');
    if (dot > slash) {
      var extension = file.fileName.substring(dot);
      if (extension in extensionMap) {
        extensionMap[extension].count++;
      } else {
        extensionMap[extension] = { extension: extension, count: 1 };
        extensionList.push(extensionMap[extension]);
      }
    }
  }
  for (var i = 0; i < fileSelection.length; i++) {
    files[fileSelection[i]].selected = true;
  }

  extensionList.sort(function(a, b) {
    return b.count - a.count;
  });
  this._extensions = extensionList;
};

Refine.DefaultImportingController.prototype._ensureFormatParserUIHasInitializationData = function(format, onDone) {
  if (!(format in this._parserOptions)) {
    var self = this;
    var dismissBusy = DialogSystem.showBusy($.i18n._('core-index-import')["inspecting"]);
    $.post(
      "command/core/importing-controller?" + $.param({
        "controller": "core/default-importing-controller",
        "jobID": this._jobID,
        "subCommand": "initialize-parser-ui",
        "format": format
      }),
      null,
      function(data) {
        dismissBusy();

        if (data.options) {
          self._parserOptions[format] = data.options;
          onDone();
        }
      },
      "json"
    )
    .fail(function() {
    	dismissBusy();
    	alert($.i18n._('core-views')["check-format"]);
    });
  } else {
    onDone();
  }
};

Refine.DefaultImportingController.prototype.updateFormatAndOptions = function(options, callback, finallyCallBack) {
  var self = this;
  $.post(
    "command/core/importing-controller?" + $.param({
      "controller": "core/default-importing-controller",
      "jobID": this._jobID,
      "subCommand": "update-format-and-options"
    }),
    {
      "format" : this._format,
      "options" : JSON.stringify(options)
    },
    function(o) {
      if (o.status == 'error') {
        if (o.message) {
          alert(o.message);					
        } else {
          var messages = [];
          $.each(o.errors, function() { messages.push(this.message); });
          alert(messages.join('\n\n'));
        }
        finallyCallBack();
      } else {
        callback(o);
      }
    },
    "json"
  );
};

Refine.DefaultImportingController.prototype.getPreviewData = function(callback, numRows) {
  var self = this;
  var result = {};

  $.post(
    "command/core/get-models?" + $.param({ "importingJobID" : this._jobID }),
    null,
    function(data) {
      for (var n in data) {
        if (data.hasOwnProperty(n)) {
          result[n] = data[n];
        }
      }

      $.post(
        "command/core/get-rows?" + $.param({
          "importingJobID" : self._jobID,
          "start" : 0,
          "limit" : numRows || 100 // More than we parse for preview anyway
        }),
        null,
        function(data) {
          result.rowModel = data;
          callback(result);
        },
        "jsonp"
      );
    },
    "json"
  );
};

Refine.DefaultImportingController.prototype._createProject = function() {
  if ((this._formatParserUI) && this._formatParserUI.confirmReadyToCreateProject()) {
    var projectName = $.trim(this._parsingPanelElmts.projectNameInput[0].value);
    if (projectName.length === 0) {
      window.alert($.i18n._('core-index-import')["warning-name"]);
      this._parsingPanelElmts.projectNameInput.focus();
      return;
    }

    var self = this;
    var options = this._formatParserUI.getOptions();
    options.projectName = projectName;
    $.post(
      "command/core/importing-controller?" + $.param({
        "controller": "core/default-importing-controller",
        "jobID": this._jobID,
        "subCommand": "create-project"
      }),
      {
        "format" : this._format,
        "options" : JSON.stringify(options)
      },
      function(o) {
        if (o.status == 'error') {
          alert(o.message);
          return;
        }
        
        var start = new Date();
        var timerID = window.setInterval(
          function() {
            self._createProjectUI.pollImportJob(
                start,
                self._jobID,
                timerID,
                function(job) {
                  return "projectID" in job.config;
                },
                function(jobID, job) {
                  Refine.CreateProjectUI.cancelImportingJob(jobID);
                  document.location = "project?project=" + job.config.projectID;
                },
                function(job) {
                  alert($.i18n._('core-index-import')["errors"]+'\n' + Refine.CreateProjectUI.composeErrorMessage(job));
                  self._onImportJobReady();
                }
            );
          },
          1000
        );
        self._createProjectUI.showImportProgressPanel($.i18n._('core-index-import')["creating-proj"], function() {
          // stop the timed polling
          window.clearInterval(timerID);

          // explicitly cancel the import job
          Refine.CreateProjectUI.cancelImportingJob(self._jobID);

          self._createProjectUI.showSourceSelectionPanel();
        });
      },
      "json"
    );
  }
};
