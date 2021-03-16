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

class Facet {
  constructor(div, config, options) {
  	this._div = div;
  	this._config = config;
  	
  	if(this._config.id == null || this._config.id == "") {
  	  this.id = 0;
  	} else {
  	  this.id = this._config.id;
  	}
  	delete this._config.id;
  	
  	if(Facet.references[this.id].snfcn) {
  	  this.nameAtCreation = $.i18n(Facet.references[this.id].label);
  	} else {
  	  this.nameAtCreation = this._config.columnName;
  	}
  	
  	this.source = Facet.references[this.id].source;
  	this._buildToolTipText();

  	this._options = options || {};
  	this._minimizeState = false;
  };
  
  _buildToolTipText() {
    var sourceText = this.source ? this.source : $.i18n('core-facets/column-tooltip', this._config.columnName);
  	
    this.facetToolTipText = $.i18n('core-facets/edit-facet-title', sourceText);
    
    if(!this._config.expression && this._config.name == this.nameAtCreation)
      return;
    
    this.facetToolTipText += "\n";
    
    if(this._config.expression) 
      this.facetToolTipText += "\n" + $.i18n('core-facets/expr-tooltip', this._config.expression);    
    
    if(this._config.name != this.nameAtCreation)
      this.facetToolTipText += "\n" + $.i18n('core-facets/original-name-tooltip', this.nameAtCreation);
  }
  
  getJSONByID() {
    var o = this.getJSON();
    
    o.id = this.id;
    
    return o
  }
  
  _minimize() {
  	if(!this._minimizeState) {
  		this._div.addClass("facet-state-minimize");
  	} else {
  		this._div.removeClass("facet-state-minimize");
  	}
  	
  	this._minimizeState = !this._minimizeState;
  };

  _editTitle() {
    var promptText = $.i18n('core-facets/facet-current-title', this._config.name);    
    var newFacetTitle = prompt(promptText, this._config.name);

    if (newFacetTitle == null || newFacetTitle == "") 
      newFacetTitle = this._config.nameAtCreation;
    
    this._config.name = newFacetTitle;
    this._buildToolTipText();
    this._elmts.titleSpan.attr("title", this.facetToolTipText);
    this._elmts.titleSpan.text(newFacetTitle);
  };

  _remove() {
  	ui.browsingEngine.removeFacet(this);

  	this._div = null;
  	this._config = null;

  	this._selection = null;
  	this._blankChoice = null;
  	this._errorChoice = null;
  	this._data = null;
  	this._options = null;
  };

  dispose() {
  };
};

Facet.references = {
   0: { source: "<unknown>",           label: 'core-views/unknown-facet-id',     snfcn: true,  type: '' },
   2: { source: "<stars>",             label: 'core-views/starred-rows',         snfcn: true,  type: 'list' },
   3: { source: "<flags>",             label: 'core-views/flagged-rows',         snfcn: true,  type: 'list' },
   4: { source: "<blank-rows>",        label: 'core-views/blank-rows',           snfcn: true,  type: 'list' },
   5: { source: "<blank-values>",      label: 'core-views/blank-values',         snfcn: true,  type: 'list' },
   6: { source: "<blank-records>",     label: 'core-views/blank-records',        snfcn: true,  type: 'list' },
   7: { source: "<non-blank-values>",  label: 'core-views/non-blank-values',     snfcn: true,  type: 'list' },
   8: { source: "<non-blank-records>", label: 'core-views/non-blank-records',    snfcn: true,  type: 'list' },
   9: { source: "",                    label: 'core-views/custom-facet',         snfcn: false, type: 'list' },
  10: { source: "",                    label: 'core-views/custom-numeric-label', snfcn: false, type: '' },
  11: { source: "",                    label: 'core-views/text-facet',           snfcn: false, type: 'list' },
  12: { source: "",                    label: 'core-views/numeric-facet',        snfcn: false, type: 'range' },
  13: { source: "",                    label: 'core-views/timeline-facet',       snfcn: false, type: 'list' },
  14: { source: "",                    label: 'core-views/word-facet',           snfcn: false, type: 'list' },
  15: { source: "",                    label: 'core-views/duplicates-facet',     snfcn: false, type: 'list' },
  16: { source: "",                    label: 'core-views/numeric-log-facet',    snfcn: false, type: 'range' },
  17: { source: "",                    label: 'core-views/bounded-log-facet',    snfcn: false, type: 'range' },
  18: { source: "",                    label: 'core-views/text-length-facet',    snfcn: false, type: 'range' },
  19: { source: "",                    label: 'core-views/log-length-facet',     snfcn: false, type: 'range' },
  20: { source: "",                    label: 'core-views/unicode-facet',        snfcn: false, type: 'range' },
  21: { source: "",                    label: 'core-views/facet-error',          snfcn: false, type: 'list' },
  22: { source: "",                    label: 'core-views/facet-null',           snfcn: false, type: 'list' },
  23: { source: "",                    label: 'core-views/facet-empty-string',   snfcn: false, type: 'list' },
  24: { source: "",                    label: 'core-views/facet-blank',          snfcn: false, type: 'list' },
  25: { source: "",                    label: 'core-views/text-filter',          snfcn: false, type: 'list' },
  26: { source: "",                    label: 'core-views/facet-by-count',       snfcn: false, type: 'range' },
  27: { source: "",                    label: 'core-views/scatterplot-facet',    snfcn: false, type: 'scatterplot' }
};
