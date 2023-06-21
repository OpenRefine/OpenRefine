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

Refine.ImportProjectUI = function(elmt) {
  elmt.html(DOM.loadHTML("core", "scripts/index/import-project-ui.html"));

  Refine.wrapCSRF(function(token) {
     $('#project-upload-form').attr('action', "command/core/import-project?" + $.param({ csrf_token: token}));
  });
  
  this._elmt = elmt;
  this._elmts = DOM.bind(elmt);
  let fileInput = document.getElementById("project-tar-file-input")
  this._elmts.projectTarDelete.on('click', function () {
    fileInput.value = ""
  })

  this._elmts.projectButton.on('click', function (e) {
    let urlInput = document.getElementById("project-url-input")

    if (fileInput.value === "" && urlInput.value === "") {
      alert($.i18n('core-index-import/warning-import-input'));
    } else if (fileInput.value.length > 0 && urlInput.value.length > 0) {
      alert($.i18n('core-index-import/warning-import-two-input'));
    } else if (urlInput.value !== "" && !URLUtil.looksLikeUrl(urlInput.value)) {
      alert($.i18n('core-index-import/warning-import-url'));
    } else {
      document.getElementById("import-project-button").type = "submit";
    }
  });

  $('#or-import-locate').text($.i18n('core-index-import/locate'));
  $('#or-import-file').text($.i18n('core-index-import/file'));
  $('#project-tar-file-delete').val($.i18n('core-index-import/delete-import-file'));
  $('#or-import-url').text($.i18n('core-index-import/or'));
  $('#or-import-rename').text($.i18n('core-index-import/rename'));
  $('#import-project-button').val($.i18n('core-buttons/import-proj'));
};

Refine.actionAreas.push({
  id: "import-project",
  label: $.i18n('core-index-import/import-proj'),
  uiClass: Refine.ImportProjectUI
});
