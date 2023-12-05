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

I18NUtil.init("gdata");

ExporterManager.MenuItems.push({});
ExporterManager.MenuItems.push(
  {
    "id": "export-to-google-drive",
    "label": $.i18n('gdata-exporter/export-to-google-drive'),
    "click": function () { ExporterManager.handlers.exportProjectToGoogleDrive(); }
  });
ExporterManager.MenuItems.push(
  {
    "id": "export-to-google-sheets",
    "label": $.i18n('gdata-exporter/google-sheets'),
    "click": function () { ExporterManager.handlers.exportProjectToGoogleSheets(); }
  });

(function() {
  var handleUpload = function(options, exportAllRows, onDone, prompt) {
    var doUpload = function() {
      var name = window.prompt(prompt, theProject.metadata.name);
      if (name) {
        var dismiss = DialogSystem.showBusy($.i18n('gdata-exporter/uploading'));
        Refine.postCSRF(
          "command/gdata/upload",
          {
            "project" : theProject.id,
            "engine" : exportAllRows ? '' : JSON.stringify(ui.browsingEngine.getJSON()),
            "name" : name,
            "format" : options.format,
            "options" : JSON.stringify(options)
          },
          function(o) {
            dismiss();

            if (o.url) {
              window.open(o.url, '_blank');
            } else {
              DialogSystem.alert($.i18n('gdata-exporter/upload-error') + o.message)
            }
            if (onDone) {
              onDone();
            }
          },
          "json"
        );
      }
    };

    if (GdataExtension.isAuthorized()) {
      doUpload();
    } else {
      GdataExtension.showAuthorizationDialog(doUpload);
    }
  };

  CustomTabularExporterDialog.uploadTargets.push({
    id: 'gdata/google-spreadsheet',
    label: $.i18n('gdata-exporter/new-spreadsheet'),
    handler: function(options, exportAllRows, onDone) {
      handleUpload(options, exportAllRows, onDone, $.i18n('gdata-exporter/enter-spreadsheet'));
    }
  });
})();

ExporterManager.handlers.exportProjectToGoogleDrive = function () {
  var doExportToGoogleDrive = function () {
    var name = window.prompt($.i18n('gdata-exporter/enter-filename'), theProject.metadata.name);
    if (name) {
      var dismiss = DialogSystem.showBusy($.i18n('gdata-exporter/uploading'));
      Refine.postCSRF(
        "command/gdata/upload",
        {
          "project": theProject.id,
          "name": name,
          "format": "raw/openrefine-project"
        },
        function (o) {
          dismiss();

          if (o.url) {
            DialogSystem.alert($.i18n('gdata-exporter/upload-google-drive-success'));
          } else {
            DialogSystem.alert($.i18n('gdata-exporter/upload-error') + o.message)
          }
        },
        "json"
      );
    }
  };

  if (GdataExtension.isAuthorized()) {
    doExportToGoogleDrive();
  } else {
    GdataExtension.showAuthorizationDialog(doExportToGoogleDrive);
  }
}

ExporterManager.handlers.exportProjectToGoogleSheets = function () {
  var doExportToGoogleSheets = function () {
    var name = window.prompt($.i18n('gdata-exporter/enter-spreadsheet'), theProject.metadata.name);
    if (name) {
      var dismiss = DialogSystem.showBusy($.i18n('gdata-exporter/uploading'));
      Refine.postCSRF(
        "command/gdata/upload",
        {
          "project": theProject.id,
          "name": name,
          "format": "gdata/google-spreadsheet"
        },
        function (o) {
          dismiss();

          if (o.url) {
            DialogSystem.alert($.i18n('gdata-exporter/upload-google-sheets-success'));
          } else {
            DialogSystem.alert($.i18n('gdata-exporter/upload-error') + o.message)
          }
        },
        "json"
      );
    }
  };

  if (GdataExtension.isAuthorized()) {
    doExportToGoogleSheets();
  } else {
    GdataExtension.showAuthorizationDialog(doExportToGoogleSheets);
  }
}
