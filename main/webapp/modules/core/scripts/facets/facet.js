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
    
    this._buildToolTipText();

    this._options = options || {};
    this._minimizeState = false;
    
    Refine.showLeftPanel();
  };

  _minimize() {
    if(!this._minimizeState) {
      this._div.addClass("facet-state-minimize");
    } else {
      this._div.removeClass("facet-state-minimize");
    }
    
    this._minimizeState = !this._minimizeState;
  };
  
  _buildToolTipText() {
    var curExpr = this._config.expression;
    
    var newFacetToolTipText = "";
    
    if(Facet.references[curExpr]) {
      var sourceText = Facet.references[curExpr].source;
      sourceText = "\n" + $.i18n('core-facets/source-tooltip', sourceText);
    }
    
    if(this._config.columnName) {
      var columnText = "\n" + $.i18n('core-facets/column-tooltip', this._config.columnName);
    }
    
    if(this._config.expression) {
      var exprText = "\n" + $.i18n('core-facets/expr-tooltip', this._config.expression);    
    }
    
    if(sourceText || columnText || exprText)   newFacetToolTipText += "\n";
    
    if(sourceText != null && sourceText != "") newFacetToolTipText += sourceText;
    if(columnText != null && columnText != "") newFacetToolTipText += columnText;
    if(exprText   != null && exprText   != "") newFacetToolTipText += exprText;
    
    this.facetToolTipText = $.i18n('core-facets/edit-facet-title') + newFacetToolTipText;
  }

  _editTitle() {
    var promptText = $.i18n('core-facets/facet-current-title', this._config.name);    
    var newFacetTitle = prompt(promptText, this._config.name);

    if (newFacetTitle == null || newFacetTitle == "") return;
    
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
  "row.starred": 
    { source: "<stars>", label: 'core-views/starred-rows' },
  "row.flagged": 
    { source: "<flags>", label: 'core-views/flagged-rows' },
  "(filter(row.columnNames,cn,isNonBlank(cells[cn].value)).length()==0).toString()": 
    { source: "<blank-rows>", label: 'core-views/blank-rows' },
  "filter(row.columnNames,cn,isBlank(cells[cn].value))": 
    { source: "<blank-values>", label: 'core-views/blank-values' },
  "filter(row.columnNames,cn,isBlank(if(row.record.fromRowIndex==row.index,row.record.cells[cn].value.join(\"\"),true)))": 
    { source: "<blank-records>", label: 'core-views/blank-records' },
  "filter(row.columnNames,cn,isNonBlank(cells[cn].value))": 
    { source: "<non-blank-values>", label: 'core-views/non-blank-values' },
  "filter(row.columnNames,cn,isNonBlank(if(row.record.fromRowIndex==row.index,row.record.cells[cn].value.join(\"\"),null)))": 
    { source: "<non-blank-records>", label: 'core-views/non-blank-records' }
}
