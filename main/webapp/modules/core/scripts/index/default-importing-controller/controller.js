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
    
    this._progressPanel = createProjectUI.addCustomPanel();
    this._progressPanel.html(DOM.loadHTML("core", "scripts/index/default-importing-controller/progress-panel.html"));
    
    this._errorPanel = createProjectUI.addCustomPanel();
    this._errorPanel.html(DOM.loadHTML("core", "scripts/index/default-importing-controller/error-panel.html"));
    
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
    $.post(
        "/command/core/create-importing-job",
        null,
        function(data) {
            var jobID = self._jobID = data.jobID;

            form.attr("method", "post")
                .attr("enctype", "multipart/form-data")
                .attr("accept-charset", "UTF-8")
                .attr("target", "default-importing-iframe")
                .attr("action", "/command/core/importing-controller?" + $.param({
                    "controller": "core/default-importing-controller",
                    "jobID": jobID,
                    "subCommand": "load-raw-data"
                }));
            form[0].submit();

            var start = new Date();
            var timerID = window.setInterval(
                function() {
                    self._pollImportJob(
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
                        }
                    );
                },
                1000
            );
            self._initializeImportProgressPanel(progressMessage, function() {
                // stop the iframe
                $('#default-importing-iframe')[0].contentWindow.stop();

                // stop the timed polling
                window.clearInterval(timerID);

                // explicitly cancel the import job
                $.post("/command/core/cancel-importing-job?" + $.param({ "jobID": jobID }));
                
                self._createProjectUI.showSourceSelectionPanel();
            });
        },
        "json"
    );
};

Refine.DefaultImportingController.prototype._initializeImportProgressPanel = function(progressMessage, onCancel) {
    var self = this;
    
    this._createProjectUI.showCustomPanel(this._progressPanel);
    
    $('#default-importing-progress-message').text(progressMessage);
    $('#default-importing-progress-bar-body').css("width", "0%");
    $('#default-importing-progress-message-left').text('Starting');
    $('#default-importing-progress-message-center').empty();
    $('#default-importing-progress-message-right').empty();
    $('#default-importing-progress-timing').empty();

    $('#default-importing-progress-cancel-button').unbind().click(onCancel);
};

Refine.DefaultImportingController.prototype._pollImportJob = function(start, jobID, timerID, checkDone, callback) {
    var self = this;
    $.post(
        "/command/core/get-importing-job-status?" + $.param({ "jobID": jobID }),
        null,
        function(data) {
            if (!(data)) {
                self._showImportJobError("Unknown error");
                window.clearInterval(timerID);
                return;
            } else if (data.code == "error" || !("job" in data)) {
                self._showImportJobError(data.message || "Unknown error");
                window.clearInterval(timerID);
                return;
            }
            
            var job = data.job;
            if (checkDone(job)) {
                $('#default-importing-progress-message').text('Done.');
                
                window.clearInterval(timerID);
                if (callback) {
                    callback(jobID, job);
                }
            } else {
                var progress = job.config.progress;
                if (progress.percent > 0) {
                    var secondsSpent = (new Date().getTime() - start.getTime()) / 1000;
                    var secondsRemaining = (100 / progress.percent) * secondsSpent - secondsSpent;
                    
                    $('#default-importing-progress-bar-body')
                        .removeClass('indefinite')
                        .css("width", progress.percent + "%");
                    
                    if (secondsRemaining > 1) {
                        if (secondsRemaining > 60) {
                            $('#default-importing-progress-timing').text(
                                Math.ceil(secondsRemaining / 60) + " minutes remaining");
                        } else {
                            $('#default-importing-progress-timing').text(
                                Math.ceil(secondsRemaining) + " seconds remaining");
                        }
                    } else {
                        $('#default-importing-progress-timing').text('almost done ...');
                    }
                } else {
                    $('#default-importing-progress-bar-body').addClass('indefinite');
                    $('#default-importing-progress-timing').empty();
                }
                $('#default-importing-progress-message').text(progress.message);
            }
        },
        "json"
    );
};

Refine.DefaultImportingController.prototype._showImportJobError = function(message, stack) {
    var self = this;
    
    $('#default-importing-error-message').text(message);
    $('#default-importing-error-stack').text(stack || 'No technical details.');

    this._createProjectUI.showCustomPanel(this._errorPanel);
    $('#default-importing-error-ok-button').unbind().click(function() {
        self._createProjectUI.showSourceSelectionPanel();
    });
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
        if (dot > slash + 1) {
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
        var dismissBusy = DialogSystem.showBusy("Inspecting selected files ...");
        $.post(
            "/command/core/importing-controller?" + $.param({
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
        );
    } else {
        onDone();
    }
};

Refine.DefaultImportingController.prototype.updateFormatAndOptions = function(options, callback) {
    var self = this;
    $.post(
        "/command/core/importing-controller?" + $.param({
            "controller": "core/default-importing-controller",
            "jobID": this._jobID,
            "subCommand": "update-format-and-options"
        }),
        {
            "format" : this._format,
            "options" : JSON.stringify(options)
        },
        callback,
        "json"
    );
};

Refine.DefaultImportingController.prototype.getPreviewData = function(callback, numRows) {
    var self = this;
    var result = {};
    
    $.post(
        "/command/core/get-models?" + $.param({ "importingJobID" : this._jobID }),
        null,
        function(data) {
            for (var n in data) {
                if (data.hasOwnProperty(n)) {
                    result[n] = data[n];
                }
            }
            
            $.post(
                "/command/core/get-rows?" + $.param({
                    "importingJobID" : self._jobID,
                    "start" : 0,
                    "limit" : numRows || 100 // More than we parse for preview anyway
                }),
                null,
                function(data) {
                    // Un-pool objects
                    for (var r = 0; r < data.rows.length; r++) {
                        var row = data.rows[r];
                        for (var c = 0; c < row.cells.length; c++) {
                            var cell = row.cells[c];
                            if ((cell) && ("r" in cell)) {
                                cell.r = data.pool.recons[cell.r];
                            }
                        }
                    }
                    
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
    if (projectName.length == 0) {
      window.alert("Please name the project.");
      this._parsingPanelElmts.focus();
      return;
    }
    
    var self = this;
    var options = this._formatParserUI.getOptions();
    options.projectName = projectName;
    $.post(
        "/command/core/importing-controller?" + $.param({
            "controller": "core/default-importing-controller",
            "jobID": this._jobID,
            "subCommand": "create-project"
        }),
        {
            "format" : this._format,
            "options" : JSON.stringify(options)
        },
        function() {
          var start = new Date();
          var timerID = window.setInterval(
              function() {
                  self._pollImportJob(
                      start,
                      self._jobID,
                      timerID,
                      function(job) {
                          return "projectID" in job.config;
                      },
                      function(jobID, job) {
                          document.location = "project?project=" + job.config.projectID;
                      }
                  );
              },
              1000
          );
          self._initializeImportProgressPanel("Creating project ...", function() {
              // stop the timed polling
              window.clearInterval(timerID);

              // explicitly cancel the import job
              $.post("/command/core/cancel-importing-job?" + $.param({ "jobID": jobID }));
              
              self._createProjectUI.showSourceSelectionPanel();
          });
        },
        "json"
    );
  }
};
