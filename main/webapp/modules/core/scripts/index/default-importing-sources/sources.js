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

function ThisComputerImportingSourceUI(controller) {
  this._controller = controller;
}
Refine.DefaultImportingController.sources.push({
  "label": "This Computer",
  "id": "upload",
  "uiClass": ThisComputerImportingSourceUI
});

ThisComputerImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-computer-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  this._elmts.nextButton.click(function(evt) {
    if (self._elmts.fileInput[0].files.length === 0) {
      window.alert("You must specify a data file to import.");
    } else {
      self._controller.startImportJob(self._elmts.form, "Uploading data ...");
    }
  });
};

ThisComputerImportingSourceUI.prototype.focus = function() {
};

function UrlImportingSourceUI(controller) {
  this._controller = controller;
}
Refine.DefaultImportingController.sources.push({
  "label": "Web Addresses (URLs)",
  "id": "download",
  "uiClass": UrlImportingSourceUI
});

UrlImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-web-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  this._elmts.nextButton.click(function(evt) {
    if ($.trim(self._elmts.urlInput[0].value).length === 0) {
      window.alert("You must specify a web address (URL) to import.");
    } else {
      self._controller.startImportJob(self._elmts.form, "Downloading data ...");
    }
  });
  this._elmts.addButton.click(function(evt) {
    self._elmts.buttons.before(self._elmts.urlRow.clone());
  });
};

UrlImportingSourceUI.prototype.focus = function() {
  this._elmts.urlInput.focus();
};

function ClipboardImportingSourceUI(controller) {
  this._controller = controller;
}
Refine.DefaultImportingController.sources.push({
  "label": "Clipboard",
  "id": "clipboard",
  "uiClass": ClipboardImportingSourceUI
});

ClipboardImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-clipboard-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  this._elmts.nextButton.click(function(evt) {
    if ($.trim(self._elmts.textInput[0].value).length === 0) {
      window.alert("You must paste some data to import.");
    } else {
      self._controller.startImportJob(self._elmts.form, "Uploading pasted data ...");
    }
  });
};

ClipboardImportingSourceUI.prototype.focus = function() {
  this._elmts.textInput.focus();
};