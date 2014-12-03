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

var dictionary = "";
$.ajax({
	url : "command/core/load-language?",
	type : "POST",
	async : false,
	data : {
	  module : "gdata",
//		lang : lang
	},
	success : function(data) {
		dictionary = data;
	}
});
$.i18n.setDictionary(dictionary);
// End internationalization

(function() {
  var handleUpload = function(options, exportAllRows, onDone, prompt) {
    var doUpload = function() {
      var name = window.prompt(prompt, theProject.metadata.name);
      if (name) {
        var dismiss = DialogSystem.showBusy($.i18n._('gdata-exporter')["uploading"]);
        $.post(
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
                alert($.i18n._('gdata-exporter')["upload-error"] + o.message)
            }
            onDone();
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
    label: $.i18n._('gdata-exporter')["new-spreadsheet"],
    handler: function(options, exportAllRows, onDone) {
      handleUpload(options, exportAllRows, onDone, $.i18n._('gdata-exporter')["enter-spreadsheet"]);
    }
  });
  CustomTabularExporterDialog.uploadTargets.push({
    id: 'gdata/fusion-table',
    label: $.i18n._('gdata-exporter')["new-fusion"],
    handler: function(options, exportAllRows, onDone) {
      handleUpload(options, exportAllRows, onDone, $.i18n._('gdata-exporter')["enter-fusion"]);
    }
  });
})();
