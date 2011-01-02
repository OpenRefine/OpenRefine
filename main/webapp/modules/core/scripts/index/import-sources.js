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

var ImportSources = [];

function ThisComputerImportSourceUI(bodyDiv) {
  bodyDiv.html(DOM.loadHTML("core", "scripts/index/import-from-computer-form.html"));
  
  var elmts = DOM.bind(bodyDiv);
  elmts.nextButton.click(function(evt) {
    if (elmts.fileInput[0].files.length === 0) {
      window.alert("You must specify a data file to import.");
    } else {
      elmts.nameInput[0].value = elmts.fileInput[0].files[0].fileName
        .replace(/\.\w+/, "").replace(/[_-]/g, " ");
      
      startImportJob("file-upload", elmts.form, "Uploading data file ...");
    }
  });
}
ImportSources.push({
  "label" : "This Computer",
  "ui" : ThisComputerImportSourceUI
});

ThisComputerImportSourceUI.prototype.focus = function() {
  
}

function UrlImportSourceUI(bodyDiv) {
  
}
ImportSources.push({
  "label" : "Web Address (URL)",
  "ui" : UrlImportSourceUI
});

UrlImportSourceUI.prototype.focus = function() {
  
}

function ClipboardImportSourceUI(bodyDiv) {
  
}
ImportSources.push({
  "label" : "Clipboard",
  "ui" : ClipboardImportSourceUI
});

ClipboardImportSourceUI.prototype.focus = function() {
  
}