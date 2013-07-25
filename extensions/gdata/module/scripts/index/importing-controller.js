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

//Internationalization init
var lang = navigator.language.split("-")[0]
		|| navigator.userLanguage.split("-")[0];
var dictionary = "";
$.ajax({
	url : "/command/gdata/load-language?",
	type : "POST",
	async : false,
	data : {
		lng : lang
	},
	success : function(data) {
		dictionary = data;
	}
});
$.i18n.setDictionary(dictionary);
// End internationalization

Refine.GDataImportingController = function(createProjectUI) {
  this._createProjectUI = createProjectUI;
  
  this._parsingPanel = createProjectUI.addCustomPanel();

  createProjectUI.addSourceSelectionUI({
    label: "Google Data",
    id: "gdata-source",
    ui: new Refine.GDataSourceUI(this)
  });
  
  $('#gdata-authorize').text($.i18n._('gdata-auth')["authorize-label"]); 
  $('#gdata-authorized').text($.i18n._('gdata-auth')["authorized-label"]);
};
Refine.CreateProjectUI.controllers.push(Refine.GDataImportingController);

Refine.GDataImportingController.prototype.startImportingDocument = function(doc) {
  var dismiss = DialogSystem.showBusy($.i18n._('gdata-import')["preparing"]);
  
  var self = this;
  $.post(
    "command/core/create-importing-job",
    null,
    function(data) {
      $.post(
        "command/core/importing-controller?" + $.param({
          "controller": "gdata/gdata-importing-controller",
          "subCommand": "initialize-parser-ui",
          "docUrl": doc.docSelfLink,
          "docType": doc.type
        }),
        null,
        function(data2) {
          dismiss();
          
          if (data2.status == 'ok') {
            self._doc = doc;
            self._jobID = data.jobID;
            self._options = data2.options;
            
            self._showParsingPanel();
          } else {
            alert(data2.message);
          }
        },
        "json"
      );
    },
    "json"
  );
};

Refine.GDataImportingController.prototype.getOptions = function() {
  var options = {
    docUrl: this._doc.docSelfLink,
    docType: this._doc.type,
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

  if (this._doc.type != 'table') {
    this._parsingPanelElmts.sheetRecordContainer.find('input').each(function() {
      if (this.checked) {
        options.sheetUrl = this.getAttribute('sheetUrl');
      }
    });

    if (this._parsingPanelElmts.ignoreCheckbox[0].checked) {
      options.ignoreLines = parseIntDefault(this._parsingPanelElmts.ignoreInput[0].value, -1);
    } else {
      options.ignoreLines = -1;
    }
    if (this._parsingPanelElmts.headerLinesCheckbox[0].checked) {
      options.headerLines = parseIntDefault(this._parsingPanelElmts.headerLinesInput[0].value, 0);
    } else {
      options.headerLines = 0;
    }
  }
  
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

  return options;
};

Refine.GDataImportingController.prototype._showParsingPanel = function() {
  var self = this;
  
  this._parsingPanel.unbind().empty().html(
      DOM.loadHTML("gdata",
        this._doc.type == 'table' ?
          'scripts/index/gdata-fusion-tables-parsing-panel.html' :
          'scripts/index/gdata-parsing-panel.html'));
  this._parsingPanelElmts = DOM.bind(this._parsingPanel);
  
  if(this._doc.type != 'table'){
	  this._parsingPanelElmts.gdata-worksheet.html($.i18n._('gdata-parsing')["worksheet"]); 
	  this._parsingPanelElmts.gdata_ignore_first.html($.i18n._('gdata-parsing')["ignore-first"]);
	  this._parsingPanelElmts.gdata_ignore.html($.i18n._('gdata-parsing')["ignore"]);
	  this._parsingPanelElmts.gdata_parse_next.html($.i18n._('gdata-parsing')["parse-next"]);
	  this._parsingPanelElmts.gdata_parse.html($.i18n._('gdata-parsing')["parse"]);
  }
  this._parsingPanelElmts.startOverButton.html($.i18n._('gdata-parsing')["start-over"]);
  this._parsingPanelElmts.gdata_conf_pars.html($.i18n._('gdata-parsing')["conf-pars"]);
  this._parsingPanelElmts.gdata_proj_name.html($.i18n._('gdata-parsing')["proj-name"]);
  this._parsingPanelElmts.createProjectButton.html($.i18n._('gdata-parsing')["create-proj"]);
  this._parsingPanelElmts.gdata-options.html($.i18n._('gdata-parsing')["option"]);
  this._parsingPanelElmts.previewButton.html($.i18n._('gdata-parsing')["preview-button"]);
  this._parsingPanelElmts.gdata-updating.html($.i18n._('gdata-parsing')["updating-preview"]);
  this._parsingPanelElmts.gdata_discard_next.html($.i18n._('gdata-parsing')["discard-next"]);
  this._parsingPanelElmts.gdata_discard.html($.i18n._('gdata-parsing')["discard"]);
  this._parsingPanelElmts.gdata_limit_next.html($.i18n._('gdata-parsing')["limit-next"]);
  this._parsingPanelElmts.gdata_limit.html($.i18n._('gdata-parsing')["limit"]);
  this._parsingPanelElmts.gdata_store_row.html($.i18n._('gdata-parsing')["store-row"]);
  this._parsingPanelElmts.gdata_store_cell.html($.i18n._('gdata-parsing')["store-cell"]);
  
  if (this._parsingPanelResizer) {
    $(window).unbind('resize', this._parsingPanelResizer);
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
  $(window).resize(this._parsingPanelResizer);
  this._parsingPanelResizer();
  
  this._parsingPanelElmts.startOverButton.click(function() {
    // explicitly cancel the import job
    Refine.CreateProjectUI.cancelImportingJob(self._jobID);
    
    delete self._doc;
    delete self._jobID;
    delete self._options;
    
    self._createProjectUI.showSourceSelectionPanel();
  });
  this._parsingPanelElmts.createProjectButton.click(function() { self._createProject(); });
  this._parsingPanelElmts.previewButton.click(function() { self._updatePreview(); });
  
  this._parsingPanelElmts.projectNameInput[0].value = this._doc.title;

  if (this._doc.type != 'table') {
    var sheetTable = this._parsingPanelElmts.sheetRecordContainer[0];
    $.each(this._options.worksheets, function(i, v) {
      var id = 'gdata-worksheet-' + Math.round(Math.random() * 1000000);
      
      var tr = sheetTable.insertRow(sheetTable.rows.length);
      var td0 = $(tr.insertCell(0)).attr('width', '1%');
      var checkbox = $('<input>')
      .attr('id', id)
      .attr('type', 'radio')
      .attr('name', 'gdata-importing-parsing-worksheet')
      .attr('sheetUrl', this.link)
      .appendTo(td0);
      if (i === 0) {
        checkbox.attr('checked', 'true');
      }
      
      $('<label>')
        .attr('for', id)
        .text(this.name)
        .appendTo(tr.insertCell(1));
      
      $('<label>')
        .attr('for', id)
        .text(this.rows + ' rows')
        .appendTo(tr.insertCell(2));
    });

    if (this._options.ignoreLines > 0) {
      this._parsingPanelElmts.ignoreCheckbox.attr("checked", "checked");
      this._parsingPanelElmts.ignoreInput[0].value = this._options.ignoreLines.toString();
    }
    if (this._options.headerLines > 0) {
      this._parsingPanelElmts.headerLinesCheckbox.attr("checked", "checked");
      this._parsingPanelElmts.headerLinesInput[0].value = this._options.headerLines.toString();
    }
  }
  
  if (this._options.limit > 0) {
    this._parsingPanelElmts.limitCheckbox.attr("checked", "checked");
    this._parsingPanelElmts.limitInput[0].value = this._options.limit.toString();
  }
  if (this._options.skipDataLines > 0) {
    this._parsingPanelElmts.skipCheckbox.attr("checked", "checked");
    this._parsingPanelElmts.skipInput.value[0].value = this._options.skipDataLines.toString();
  }
  if (this._options.storeBlankRows) {
    this._parsingPanelElmts.storeBlankRowsCheckbox.attr("checked", "checked");
  }
  if (this._options.storeBlankCellsAsNulls) {
    this._parsingPanelElmts.storeBlankCellsAsNullsCheckbox.attr("checked", "checked");
  }

  var onChange = function() {
    self._scheduleUpdatePreview();
  };
  this._parsingPanel.find("input").bind("change", onChange);
  this._parsingPanel.find("select").bind("change", onChange);
  
  this._createProjectUI.showCustomPanel(this._parsingPanel);
  this._updatePreview();
};

Refine.GDataImportingController.prototype._scheduleUpdatePreview = function() {
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

Refine.GDataImportingController.prototype._updatePreview = function() {
  var self = this;

  this._parsingPanelElmts.dataPanel.hide();
  this._parsingPanelElmts.progressPanel.show();

  $.post(
    "command/core/importing-controller?" + $.param({
      "controller": "gdata/gdata-importing-controller",
      "jobID": this._jobID,
      "subCommand": "parse-preview"
    }),
    {
      "options" : JSON.stringify(this.getOptions())
    },
    function(result) {
      if (result.status == "ok") {
        self._getPreviewData(function(projectData) {
          self._parsingPanelElmts.progressPanel.hide();
          self._parsingPanelElmts.dataPanel.show();

          new Refine.PreviewTable(projectData, self._parsingPanelElmts.dataPanel.unbind().empty());
        });
      } else {
        self._parsingPanelElmts.progressPanel.hide();
        alert('Errors:\n' + 
          (result.message) ? result.message : Refine.CreateProjectUI.composeErrorMessage(job));
      }
    },
    "json"
  );
};

Refine.GDataImportingController.prototype._getPreviewData = function(callback, numRows) {
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

Refine.GDataImportingController.prototype._createProject = function() {
  var projectName = $.trim(this._parsingPanelElmts.projectNameInput[0].value);
  if (projectName.length == 0) {
    window.alert("Please name the project.");
    this._parsingPanelElmts.projectNameInput.focus();
    return;
  }

  var self = this;
  var options = this.getOptions();
  options.projectName = projectName;
  $.post(
    "command/core/importing-controller?" + $.param({
      "controller": "gdata/gdata-importing-controller",
      "jobID": this._jobID,
      "subCommand": "create-project"
    }),
    {
      "options" : JSON.stringify(options)
    },
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
        self._createProjectUI.showImportProgressPanel($.i18n._('gdata-import')["creating"], function() {
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
};
