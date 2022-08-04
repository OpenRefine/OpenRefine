/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * Neither the name of Google nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

I18NUtil.init("database");

Refine.DatabaseImportController = function(createProjectUI) {
  this._createProjectUI = createProjectUI;

  this._parsingPanel = createProjectUI.addCustomPanel();

  createProjectUI.addSourceSelectionUI({
    label: $.i18n('database-import/importer-name'),
    id: "database-source",
    ui: new Refine.DatabaseSourceUI(this)
  });

};
Refine.CreateProjectUI.controllers.push(Refine.DatabaseImportController);

Refine.DatabaseImportController.prototype.startImportingDocument = function(queryInfo) {
    var dismiss = DialogSystem.showBusy($.i18n('database-import/preparing'));
    //alert(queryInfo.query);
    var self = this;

    Refine.postCSRF(
      "command/core/create-importing-job",
      null,
      function(data) {
        Refine.wrapCSRF(function(token) {
            $.post(
            "command/core/importing-controller?" + $.param({
                "controller": "database/database-import-controller",
                "subCommand": "initialize-parser-ui",
                "csrf_token": token
            }),
            queryInfo,

            function(data2) {
                dismiss();

                if (data2.status == 'ok') {
                self._queryInfo = queryInfo;
                self._jobID = data.jobID;
                self._options = data2.options;

                self._showParsingPanel();

                } else {
                alert(data2.message);
                }
            },
            "json"
            );
        });
      },
      "json"
    );
};

Refine.DatabaseImportController.prototype.getOptions = function() {
   var options = {

   };

    var parseIntDefault = function(s, def) {
      try {
        var n = parseInt(s);
        if (!isNaN(n)) {
          return n;
        }
      } catch (e) {
        // Ignore
      }
      return def;
    };


    if (this._parsingPanelElmts.skipCheckbox[0].checked) {
      options.skipDataLines = parseIntDefault(this._parsingPanelElmts.skipInput[0].value, 0);
    } else {
      options.skipDataLines = 0;
    }
    if (this._parsingPanelElmts.limitCheckbox[0].checked) {
      options.limit = parseIntDefault(this._parsingPanelElmts.limitInput[0].value, -1);
    } else {
      options.limit = -1;
    }
    options.storeBlankRows = this._parsingPanelElmts.storeBlankRowsCheckbox[0].checked;
    options.storeBlankCellsAsNulls = this._parsingPanelElmts.storeBlankCellsAsNullsCheckbox[0].checked;

    options.disableAutoPreview = this._parsingPanelElmts.disableAutoPreviewCheckbox[0].checked;

    return options;
};

Refine.DatabaseImportController.prototype._showParsingPanel = function() {
    var self = this;

    this._parsingPanel.off().empty().html(
        DOM.loadHTML("database",'scripts/index/database-parsing-panel.html'));

    this._parsingPanelElmts = DOM.bind(this._parsingPanel);

    this._parsingPanelElmts.startOverButton.html($.i18n('database-parsing/start-over'));
    this._parsingPanelElmts.database_conf_pars.html($.i18n('database-parsing/conf-pars'));
    this._parsingPanelElmts.database_proj_name.html($.i18n('database-parsing/proj-name'));
    this._parsingPanelElmts.createProjectButton.html($.i18n('database-parsing/create-proj'));
    this._parsingPanelElmts.database_options.html($.i18n('database-parsing/option'));
    this._parsingPanelElmts.previewButton.html($.i18n('database-parsing/preview-button'));
    this._parsingPanelElmts.database_updating.html($.i18n('database-parsing/updating-preview'));
    this._parsingPanelElmts.database_discard_next.html($.i18n('database-parsing/discard-next'));
    this._parsingPanelElmts.database_discard.html($.i18n('database-parsing/discard'));
    this._parsingPanelElmts.database_limit_next.html($.i18n('database-parsing/limit-next'));
    this._parsingPanelElmts.database_limit.html($.i18n('database-parsing/limit'));
    this._parsingPanelElmts.database_store_row.html($.i18n('database-parsing/store-row'));
    this._parsingPanelElmts.database_store_cell.html($.i18n('database-parsing/store-cell'));
    this._parsingPanelElmts.database_disable_auto_preview.text($.i18n('database-parsing/disable-auto-preview'));

    if (this._parsingPanelResizer) {
      $(window).off('resize', this._parsingPanelResizer);
    }

    this._parsingPanelResizer = function() {
      var elmts = self._parsingPanelElmts;
      var width = self._parsingPanel.width();
      var height = self._parsingPanel.height();
      var headerHeight = elmts.wizardHeader.outerHeight(true);
      var controlPanelHeight = 250;

      elmts.dataPanel
      .css("left", "0px")
      .css("top", headerHeight + "px")
      .css("width", (width - DOM.getHPaddings(elmts.dataPanel)) + "px")
      .css("height", (height - headerHeight - controlPanelHeight - DOM.getVPaddings(elmts.dataPanel)) + "px");
      elmts.progressPanel
      .css("left", "0px")
      .css("top", headerHeight + "px")
      .css("width", (width - DOM.getHPaddings(elmts.progressPanel)) + "px")
      .css("height", (height - headerHeight - controlPanelHeight - DOM.getVPaddings(elmts.progressPanel)) + "px");

      elmts.controlPanel
      .css("left", "0px")
      .css("top", (height - controlPanelHeight) + "px")
      .css("width", (width - DOM.getHPaddings(elmts.controlPanel)) + "px")
      .css("height", (controlPanelHeight - DOM.getVPaddings(elmts.controlPanel)) + "px");
    };

    $(window).on('resize',this._parsingPanelResizer);
    this._parsingPanelResizer();

    this._parsingPanelElmts.startOverButton.on('click',function() {
      // explicitly cancel the import job
      Refine.CreateProjectUI.cancelImportingJob(self._jobID);

      delete self._jobID;
      delete self._options;

      self._createProjectUI.showSourceSelectionPanel();
    });

    this._parsingPanelElmts.createProjectButton.on('click',function() { self._createProject(); });
    this._parsingPanelElmts.previewButton.on('click',function() { self._updatePreview(); });
    //alert("datetime::" + $.now());
    //this._parsingPanelElmts.projectNameInput[0].value = this._queryInfo.connectionName + "_" + this._queryInfo.databaseUser + "_" + $.now();
    this._parsingPanelElmts.projectNameInput[0].value = this._queryInfo.databaseServer +  "_" + this._queryInfo.initialDatabase + "_" + $.now();


    if (this._options.limit > 0) {
      this._parsingPanelElmts.limitCheckbox.prop("checked", true);
      this._parsingPanelElmts.limitInput[0].value = this._options.limit.toString();
    }
    if (this._options.skipDataLines > 0) {
      this._parsingPanelElmts.skipCheckbox.prop("checked", true);
      this._parsingPanelElmts.skipInput.value[0].value = this._options.skipDataLines.toString();
    }
    if (this._options.storeBlankRows) {
      this._parsingPanelElmts.storeBlankRowsCheckbox.prop("checked", true);
    }
    if (this._options.storeBlankCellsAsNulls) {
      this._parsingPanelElmts.storeBlankCellsAsNullsCheckbox.prop("checked", true);
    }

    if (this._options.disableAutoPreview) {
      this._parsingPanelElmts.disableAutoPreviewCheckbox.prop('checked', true);
    }

    // If disableAutoPreviewCheckbox is not checked, we will schedule an automatic update
    var onChange = function() {
      if (!self._parsingPanelElmts.disableAutoPreviewCheckbox[0].checked)
      {
        self._scheduleUpdatePreview();
      }
    };
    this._parsingPanel.find("input").on("change", onChange);
    this._parsingPanel.find("select").on("change", onChange);

    this._createProjectUI.showCustomPanel(this._parsingPanel);
    this._updatePreview();
};

Refine.DatabaseImportController.prototype._scheduleUpdatePreview = function() {
    if (this._timerID != null) {
      window.clearTimeout(this._timerID);
      this._timerID = null;
    }

    var self = this;
    this._timerID = window.setTimeout(function() {
      self._timerID = null;
      self._updatePreview();
    }, 500); // 0.5 second
  };

Refine.DatabaseImportController.prototype._updatePreview = function() {
    var self = this;
   // alert("query::" + this._queryInfo.query);
    this._parsingPanelElmts.dataPanel.hide();
    this._parsingPanelElmts.progressPanel.show();
    this._queryInfo.options = JSON.stringify(this.getOptions());
    //alert("options:" + this._queryInfo.options);

    Refine.wrapCSRF(function(token) {
        $.post(
        "command/core/importing-controller?" + $.param({
            "controller": "database/database-import-controller",
            "jobID": self._jobID,
            "subCommand": "parse-preview",
            "csrf_token": token
        }),

        self._queryInfo,

        function(result) {
            if (result.status == "ok") {
                self._getPreviewData(function(projectData) {
                self._parsingPanelElmts.progressPanel.hide();
                self._parsingPanelElmts.dataPanel.show();

                new Refine.PreviewTable(projectData, self._parsingPanelElmts.dataPanel.off().empty());
            });
            } else {

            alert('Errors:\n' +  (result.message) ? result.message : Refine.CreateProjectUI.composeErrorMessage(job));
            self._parsingPanelElmts.progressPanel.hide();

            Refine.CreateProjectUI.cancelImportingJob(self._jobID);

            delete self._jobID;
            delete self._options;

            self._createProjectUI.showSourceSelectionPanel();


            }
        },
        "json"
        );
    });
  };

Refine.DatabaseImportController.prototype._getPreviewData = function(callback, numRows) {
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

Refine.DatabaseImportController.prototype._createProject = function() {
    var projectName = jQueryTrim(this._parsingPanelElmts.projectNameInput[0].value);
    if (projectName.length == 0) {
      window.alert("Please name the project.");
      this._parsingPanelElmts.projectNameInput.focus();
      return;
    }

    var self = this;
    var options = this.getOptions();
    options.projectName = projectName;

    this._queryInfo.options = JSON.stringify(options);
    Refine.wrapCSRF(function(token) {
        $.post(
        "command/core/importing-controller?" + $.param({
            "controller": "database/database-import-controller",
            "jobID": self._jobID,
            "subCommand": "create-project",
            "csrf_token": token
        }),
        self._queryInfo,
        function(o) {
            if (o.status == 'error') {
            alert(o.message);
            } else {
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
                        //alert("jobID::" + jobID + " job :" + job);
                        window.clearInterval(timerID);
                        Refine.CreateProjectUI.cancelImportingJob(jobID);
                        document.location = "project?project=" + job.config.projectID;
                    },
                    function(job) {
                        alert(Refine.CreateProjectUI.composeErrorMessage(job));
                    }
                );
                },
                1000
            );
            self._createProjectUI.showImportProgressPanel($.i18n('database-import/creating'), function() {
                // stop the timed polling
                window.clearInterval(timerID);

                // explicitly cancel the import job
                Refine.CreateProjectUI.cancelImportingJob(jobID);

                self._createProjectUI.showSourceSelectionPanel();
            });
            }
        },
        "json"
        );
    });
};
