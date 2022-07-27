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

// Function to check if the URL getting entered is valid or not 
function isUrlValid(url) {
  // regex for a valid URL pattern
  // Derived from the jquery-validation repository https://github.com/jquery-validation/jquery-validation/blob/master/src/additional/url2.js
  return /^(https?|s?ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(url);
}
Refine.DefaultImportingController.sources.push({
  "label": $.i18n('core-index-import/this-computer'),
  "id": "upload",
  "uiClass": ThisComputerImportingSourceUI
});

ThisComputerImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-computer-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  
  $('#or-import-locate-files').text($.i18n('core-index-import/locate-files'));
  this._elmts.nextButton.html($.i18n('core-buttons/next'));
  
  this._elmts.nextButton.on('click',function(evt) {
    if (self._elmts.fileInput[0].files.length === 0) {
      window.alert($.i18n('core-index-import/warning-data-file'));
    } else {
      self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/uploading-data'));
    }
  });
};

ThisComputerImportingSourceUI.prototype.focus = function() {
};

function UrlImportingSourceUI(controller) {
  this._controller = controller;
}
Refine.DefaultImportingController.sources.push({
  "label": $.i18n('core-index-import/web-address'),
  "id": "download",
  "uiClass": UrlImportingSourceUI
});

UrlImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-web-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  
  $('#or-import-enterurl').text($.i18n('core-index-import/enter-url'));
  this._elmts.addButton.html($.i18n('core-buttons/add-url'));
  this._elmts.nextButton.html($.i18n('core-buttons/next'));

  this._elmts.form.on('submit',function(evt) {
    evt.preventDefault();
    let errorString = '';
    $(self._elmts.form).find('input:text').each(function () {
      let url = this.value.trim();
      if (url.length === 0) {
        errorString += $.i18n('core-index-import/blank-url')+'\n';
      } else if(!isUrlValid(url)) {
        errorString += $.i18n('core-index-import/invalid-url')+' '+url+'\n';
      }
    });
    if (errorString) {
      window.alert($.i18n('core-index-import/warning-web-address')+"\n"+errorString);
    } else {
      self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/downloading-data'));
    }
  });
  this._elmts.addButton.on('click',function(evt) {
    let newRow = self._elmts.urlRow.clone();
    let trashButton = $('<a style="margin-left:0.2em;" href=""><img style="height:16px;" src="images/close.png"></a>');
    trashButton.attr("title",$.i18n("core-index-import/remove-row"));
    newRow.find('td').append(trashButton);
    trashButton.on('click',function (e) {
      e.preventDefault();
      $(this).parent().parent().remove();
    })
    self._elmts.buttons.before(newRow);
  });
};

UrlImportingSourceUI.prototype.focus = function() {
  this._elmts.urlInput.trigger('focus');
};

function ClipboardImportingSourceUI(controller) {
  this._controller = controller;
}
Refine.DefaultImportingController.sources.push({
  "label": $.i18n('core-index-import/clipboard'),
  "id": "clipboard",
  "uiClass": ClipboardImportingSourceUI
});

ClipboardImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-clipboard-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  
  $('#or-import-clipboard').text($.i18n('core-index-import/clipboard-label'));
  this._elmts.nextButton.html($.i18n('core-buttons/next'));
  
  this._elmts.nextButton.on('click',function(evt) {
    if (jQueryTrim(self._elmts.textInput[0].value).length === 0) {
      window.alert($.i18n('core-index-import/warning-clipboard'));
    } else {
      self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/uploading-pasted-data'));
    }
  });
};

ClipboardImportingSourceUI.prototype.focus = function() {
  this._elmts.textInput.trigger('focus');
};

