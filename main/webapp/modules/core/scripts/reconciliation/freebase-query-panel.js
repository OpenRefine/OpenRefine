/*

Copyright 2010, Google Inc.
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

function ReconFreebaseQueryPanel(column, service, container) {
  this._column = column;
  this._service = service;
  this._container = container;

  this._constructUI();
}

ReconFreebaseQueryPanel.prototype.activate = function() {
  this._panel.show();
};

ReconFreebaseQueryPanel.prototype.deactivate = function() {
  this._panel.hide();
};

ReconFreebaseQueryPanel.prototype.dispose = function() {
  this._panel.remove();
  this._panel = null;

  this._column = null;
  this._service = null;
  this._container = null;
};

ReconFreebaseQueryPanel.prototype._constructUI = function() {
  var self = this;
  this._panel = $(DOM.loadHTML("core", "scripts/reconciliation/freebase-query-panel.html")).appendTo(this._container);
  this._elmts = DOM.bind(this._panel);
  
  this._elmts.or_recon_contain.html($.i18n._('core-recon')["cell-contains"]);
  this._elmts.or_recon_fbId.html($.i18n._('core-recon')["fb-id"]);
  this._elmts.or_recon_fbGuid.html($.i18n._('core-recon')["fb-guid"]);
  this._elmts.or_recon_fbKey.html($.i18n._('core-recon')["fb-key"]);
  this._elmts.or_recon_fbEnNs.html($.i18n._('core-recon')["fb-en-ns"]);
  this._elmts.or_recon_thisNs.html($.i18n._('core-recon')["this-ns"]);
  
  this._wireEvents();
};

ReconFreebaseQueryPanel.prototype._wireEvents = function() {
  var self = this;
  this._elmts.strictNamespaceInput
  .suggest({ filter : '(all type:/type/namespace)' })
  .bind("fb-select", function(e, data) {
    self._panel.find('input[name="recon-dialog-strict-choice"][value="key"]').attr("checked", "true");
    self._panel.find('input[name="recon-dialog-strict-namespace-choice"][value="other"]').attr("checked", "true");
  });
};

ReconFreebaseQueryPanel.prototype.start = function() {
  var bodyParams;

  var match = $('input[name="recon-dialog-strict-choice"]:checked')[0].value;
  if (match == "key") {
    var namespaceChoice = $('input[name="recon-dialog-strict-namespace-choice"]:checked')[0];
    var namespace;

    if (namespaceChoice.value == "other") {
      var suggest = this._elmts.strictNamespaceInput.data("data.suggest");
      if (!suggest) {
        alert($.i18n._('core-recon')["specify-ns"]);
        return;
      }
      namespace = {
        id: suggest.id,
        name: suggest.name
      };
    } else {
      namespace = {
        id: namespaceChoice.value,
        name: namespaceChoice.getAttribute("nsName")
      };
    }

    bodyParams = {
      columnName: this._column.name,
      config: JSON.stringify({
        mode: "freebase/strict",
        match: "key",
        namespace: namespace
      })
    };
  } else if (match == "id") {
    bodyParams = {
      columnName: this._column.name,
      config: JSON.stringify({
        mode: "freebase/strict",
        match: "id"
      })
    };
  } else if (match == "guid") {
    bodyParams = {
      columnName: this._column.name,
      config: JSON.stringify({
        mode: "freebase/strict",
        match: "guid"
      })
    };
  }

  Refine.postCoreProcess(
    "reconcile",
    {},
    bodyParams,
    { cellsChanged: true, columnStatsChanged: true }
  );
};