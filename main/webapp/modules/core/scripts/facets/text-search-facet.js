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

function TextSearchFacet(div, config, options) {
  this._div = div;
  this._config = config;
  this._options = options;

  this._query = config.query || null;
  this._timerID = null;

  this._initializeUI();
}

TextSearchFacet.reconstruct = function(div, uiState) {
  return new TextSearchFacet(div, uiState.c, uiState.o);
};

TextSearchFacet.prototype.dispose = function() {
};

TextSearchFacet.prototype.reset = function() {
  this._query = null;
  this._div.find(".input-container input").each(function() { this.value = ""; });
};

TextSearchFacet.prototype.getUIState = function() {
  var json = {
      c: this.getJSON(),
      o: this._options
  };

  return json;
};

TextSearchFacet.prototype.getJSON = function() {
  var o = {
      type: "text",
      name: this._config.name,
      columnName: this._config.columnName,
      mode: this._config.mode,
      caseSensitive: this._config.caseSensitive,
      query: this._query
  };
  return o;
};

TextSearchFacet.prototype.hasSelection = function() {
  return this._query !== null;
};

TextSearchFacet.prototype._initializeUI = function() {
  var self = this;
  this._div.empty().show().html(
      '<div class="facet-title">' + 
      '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
      '<td width="1%"><a href="javascript:{}" title="'+$.i18n._('core-facets')["remove-facet"]+'" class="facet-title-remove" bind="removeButton">&nbsp;</a></td>' +
      '<td>' +
      '<span>' + this._config.name + '</span>' +
      '</td>' +
      '</tr></table></div>' +
      '</div>' +
      '<div class="facet-text-body"><div class="grid-layout layout-tightest layout-full"><table>' +
      '<tr><td colspan="4"><div class="input-container"><input bind="input" /></div></td></tr>' +
      '<tr>' +
      '<td width="1%"><input type="checkbox" bind="caseSensitiveCheckbox" id="caseSensitiveCheckbox" /></td><td><label for="caseSensitiveCheckbox">'+$.i18n._('core-facets')["case-sensitive"]+'</label></td>' +
      '<td width="1%"><input type="checkbox" bind="regexCheckbox" id="regexCheckbox" /></td><td><label for="regexCheckbox">'+$.i18n._('core-facets')["regular-exp"]+'</label></td>' +
      '</tr>' +
      '</table></div></div>'
  );

  var elmts = DOM.bind(this._div);

  if (this._config.caseSensitive) {
    elmts.caseSensitiveCheckbox.attr("checked", "true");
  }
  if (this._config.mode == "regex") {
    elmts.regexCheckbox.attr("checked", "true");
  }

  elmts.removeButton.click(function() { self._remove(); });

  elmts.caseSensitiveCheckbox.bind("change", function() {
    self._config.caseSensitive = this.checked;
    if (self._query !== null && self._query.length > 0) {
      self._scheduleUpdate();
    }
  });
  elmts.regexCheckbox.bind("change", function() {
    self._config.mode = this.checked ? "regex" : "text";
    if (self._query !== null && self._query.length > 0) {
      self._scheduleUpdate();
    }
  });

  if (this._query) {
    elmts.input[0].value = this._query;
  }
  
  elmts.input.bind("keyup change input",function(evt) {
    // Ignore non-character keyup changes
    if(evt.type === "keyup" && (this.value === self._query || this.value === '' && !self._query)) {
      return;
    }
    self._query = this.value;
    self._scheduleUpdate();
  }).focus();

};

TextSearchFacet.prototype.updateState = function(data) {
};

TextSearchFacet.prototype.render = function() {
  this._setRangeIndicators();
};

TextSearchFacet.prototype._reset = function() {
  this._query = null;
  this._updateRest();
};

TextSearchFacet.prototype._remove = function() {
  ui.browsingEngine.removeFacet(this);

  this._div = null;
  this._config = null;
  this._options = null;
};

TextSearchFacet.prototype._scheduleUpdate = function() {
  if (!this._timerID) {
    var self = this;
    this._timerID = window.setTimeout(function() {
      self._timerID = null;
      self._updateRest();
    }, 500);
  }
};

TextSearchFacet.prototype._updateRest = function() {
  Refine.update({ engineChanged: true });
};
