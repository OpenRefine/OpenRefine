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

class GenericFacet {
  constructor(div, config, options, selection) {
    this._div = div;
    this._config = config;
    if (!("invert" in this._config)) {
      this._config.invert = false;
    }

    this.facetId = this._config.savedFacetId ? 
      parseInt(this._config.savedFacetId) : Math.round(Math.random() * 1000000);

    this._options = options || {};
    if (!("sort" in this._options)) {
      this._options.sort = "name";
    }

    this._selection = selection || [];

    this._blankChoice = (config.selectBlank) ? { s : true, c : 0 } : null;
    this._errorChoice = (config.selectError) ? { s : true, c : 0 } : null;

    this._data = null;
  }

  reconstruct(div, uiState) {
    throw "GenericFacet.reconstruct(div, uiState) has not been subclassed." 
  };

  dispose() {
    throw "GenericFacet.dispose() has not been subclassed."
  };
  
  reset() {
    this._selection = [];
    this._blankChoice = null;
    this._errorChoice = null;
  };

  getUIState() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };

    json.s = json.c.selection;
    delete json.c.selection;

    return json;
  };

  hasSelection() {
    return this._selection.length > 0 || 
    (this._blankChoice !== null && this._blankChoice.s) || 
    (this._errorChoice !== null && this._errorChoice.s);
  };
  
  _initializeUI() {
    var self = this;
    
    this._elmts = DOM.bind(this._div);

    this._elmts.titleSpan.text(this._config.name);
    this._elmts.changeButton.attr("title",$.i18n('core-facets/current-exp')+": " + this._config.expression).click(function() {
      self._elmts.expressionDiv.slideToggle(100, function() {
        if (self._elmts.expressionDiv.css("display") != "none") {
          self._editExpression();
        }
      });
    });
  
    this._elmts.expressionDiv.text(this._config.expression).hide().click(function() { self._editExpression(); });
    this._elmts.removeButton.click(function() { self._remove(); });
    this._elmts.saveConfigButton.click(function() { self._saveConfig(); });
    this._elmts.minimizeButton.click(function() { self._minimize(); });
    this._elmts.resetButton.click(function() { self._reset(); });    
  }
  
  _reset() {
    this._selection = [];
    this._blankChoice = null;
    this._errorChoice = null;
    this._config.invert = false;

    this._updateRest();
  };

  _invert() {
    this._config.invert = !this._config.invert;

    this._updateRest();
  };

  _remove() {
    var facetPersistance = JSON.parse(localStorage.getItem('facets-'+ theProject.id));
    if(facetPersistance == null || typeof facetPersistance != "object") { facetPersistance = {}; }
    
    delete facetPersistance[this.facetId];
    localStorage.setItem('facets-'+ theProject.id, JSON.stringify(facetPersistance));

    ui.browsingEngine.removeFacet(this);

    this._div = null;
    this._config = null;

    this._selection = null;
    this._blankChoice = null;
    this._errorChoice = null;
    this._data = null;
  };

  _minimize() {
    this._div.toggleClass("facet-state-minimize");
  };

  _saveConfig() {
    var facetPersistance = JSON.parse(localStorage.getItem('facets-'+ theProject.id));
    if(facetPersistance == null || typeof facetPersistance != "object") { facetPersistance = {}; }
    
    facetPersistance[this.facetId] = { c: this._config, o: this._options, s: this._selection };
    localStorage.setItem('facets-'+ theProject.id, JSON.stringify(facetPersistance));
  };

  _updateRest() {
    Refine.update({ engineChanged: true });
  };
  
  static getFacetConfigs() {
    var savedFacetConfigs = JSON.parse(localStorage.getItem('facets-'+ theProject.id));
    var resultFacetConfigs = [];
    
    if(savedFacetConfigs == null) return resultFacetConfigs;
    
    // @ToDo check the stored facet configurations, make sure they aren't corrupted
    var facetsIds = Object.keys(savedFacetConfigs);
    
    for (var i = 0; i < facetsIds.length; i++) {
      var loadedFacetConfig = {};
      var savedFacetConfig = savedFacetConfigs[facetsIds[i]];
      
      loadedFacetConfig = savedFacetConfig;
      loadedFacetConfig.c.savedFacetId = facetsIds[i];
      
      resultFacetConfigs.push(loadedFacetConfig);
    }
    
    return resultFacetConfigs;
  }
}
