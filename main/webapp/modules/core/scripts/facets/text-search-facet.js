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

class TextSearchFacet extends Facet {
  constructor(div, config, options) {
    super(div, config, options);
    if (!("invert" in this._config)) {
      this._config.invert = false;
    }

    this._query = config.query || null;
    this._timerID = null;

    this._initializeUI();
    this._update();
  };

  static textSearchFacetCounterForLabels = 0;

  reset() {
    this._query = null;
    this._div.find(".input-container input").each(function() { this.value = ""; });
  };

  getUIState() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };

    return json;
  };

  getJSON() {
    var o = {
        type: "text",
        name: this._config.name,
        columnName: this._config.columnName,
        mode: this._config.mode,
        caseSensitive: this._config.caseSensitive,
        invert: this._config.invert,
        query: this._query
    };
    return o;
  };

  hasSelection() {
    return this._query !== null;
  };

  _initializeUI() {
    var self = this;
    var counter = this._uniqueIdForLabels();
    this._div.empty().show().html(
      '<div class="facet-title" bind="facetTitle">' + 
        '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/remove-facet')+'" class="facet-title-remove" bind="removeButton">&nbsp;</a>' +
          '</td>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/minimize-facet')+'" class="facet-title-minimize" bind="minimizeButton">&nbsp;</a>' +
          '</td>' +
          '<td>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">'+$.i18n('core-facets/reset')+'</a>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="invertButton">'+$.i18n('core-facets/invert')+'</a>' +
            '<span bind="titleSpan"></span>' +
          '</td>' +
        '</tr></table></div>' +
      '</div>' +
      '<div class="facet-text-body"><div class="grid-layout layout-tightest layout-full"><table>' +
        '<tr><td colspan="4"><div class="input-container"><input bind="input" /></div></td></tr>' +
        '<tr>' +
          '<td width="1%"><input type="checkbox" bind="caseSensitiveCheckbox" id="caseSensitiveCheckbox'+counter+'" /></td>' +
          '<td><label for="caseSensitiveCheckbox'+counter+'">'+$.i18n('core-facets/case-sensitive')+'</label></td>' +
          '<td width="1%"><input type="checkbox" bind="regexCheckbox" id="regexCheckbox'+counter+'" /></td>' +
          '<td><label for="regexCheckbox'+counter+'">'+$.i18n('core-facets/regular-exp')+'</label></td>' +
        '</tr>' +
      '</table></div>'
    );

    this._elmts = DOM.bind(this._div);

    this._elmts.titleSpan.text(this._config.name);
    if (this._config.caseSensitive) {
      this._elmts.caseSensitiveCheckbox.prop('checked', true);
    }
    if (this._config.mode === "regex") {
      this._elmts.regexCheckbox.prop('checked', true);
    }

    this._elmts.removeButton.on('click',function() { self._remove(); });
    this._elmts.minimizeButton.on('click',function() { self._minimize(); });
    this._elmts.resetButton.on('click',function() { self._reset(); });
    this._elmts.invertButton.on('click',function() { self._invert(); });

    this._elmts.caseSensitiveCheckbox.on("change", function() {
      self._config.caseSensitive = this.checked;
      if (self._query !== null && self._query.length > 0) {
        self._scheduleUpdate();
      }
    });
    this._elmts.regexCheckbox.on("change", function() {
      self._config.mode = this.checked ? "regex" : "text";
      if (self._query !== null && self._query.length > 0) {
        self._scheduleUpdate();
      }
    });

    if (this._query) {
      this._elmts.input[0].value = this._query;
    }
    
    this._elmts.input.on("keyup change input",function(evt) {
      // Ignore events which don't change our input value
      if(this.value === self._query || this.value === '' && !self._query) {
        return;
      }
      self._query = this.value;
      self._scheduleUpdate();
    }).trigger('focus');

  };

  updateState(data) {
    this._update();
  };

  render() {
    this._setRangeIndicators();
  };

  _reset() {
    this._query = null;
    this._config.mode = "text";
    this._config.caseSensitive = false;
    this._elmts.input.val([]);
    this._elmts.caseSensitiveCheckbox.prop("checked", false);
    this._elmts.regexCheckbox.prop("checked", false);
    this._config.invert = false;

    this._updateRest();
  };

  _invert() {
    this._config.invert = !this._config.invert;

    this._updateRest();
  };

  _update() {
    var invert = this._config.invert;
    if (invert) {
      this._elmts.facetTitle.addClass("facet-title-inverted");
      this._elmts.invertButton.addClass("facet-mode-inverted");
    } else {
      this._elmts.facetTitle.removeClass("facet-title-inverted");
      this._elmts.invertButton.removeClass("facet-mode-inverted");
    }
  };

  _scheduleUpdate() {
    if (!this._timerID) {
      var self = this;
      this._timerID = window.setTimeout(function() {
        self._timerID = null;
        self._updateRest();
      }, self._config.mode === 'regex' ? 1500 : 500);
    }
  };

  _updateRest() {
    Refine.update({ engineChanged: true });
  };

  _uniqueIdForLabels() {
    return TextSearchFacet.textSearchFacetCounterForLabels++;
  };
}


TextSearchFacet.reconstruct = function(div, uiState) {
  return new TextSearchFacet(div, uiState.c, uiState.o);
};
