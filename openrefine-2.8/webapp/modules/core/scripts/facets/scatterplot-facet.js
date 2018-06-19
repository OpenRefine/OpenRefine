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

function ScatterplotFacet(div, config, options) {
  this._div = div;
  this._config = config;
  this._options = options;

  this._error = false;
  this._initializedUI = false;
}

ScatterplotFacet.prototype.update = function() {
  this._plotAreaSelector.update();
};

ScatterplotFacet.prototype.reset = function() {
  delete this._config.from_x;
  delete this._config.from_y;
  delete this._config.to_x;
  delete this._config.to_y;
  this._plotAreaSelector.setOptions({ hide : true });
  this._plotAreaSelector.update();
};

ScatterplotFacet.reconstruct = function(div, uiState) {
  return new ScatterplotFacet(div, uiState.c, uiState.o);
};

ScatterplotFacet.prototype.dispose = function() {
  this._elmts.plotImg.imgAreaSelect({ hide : true });
};

ScatterplotFacet.prototype.getUIState = function() {
  var json = {
      c: this.getJSON(),
      o: this._options
  };

  return json;
};

ScatterplotFacet.prototype.getJSON = function() {
  this._config.type = "scatterplot";
  var dot = this._config.dot;
  if (typeof dot == 'number') this._config.dot.toFixed(2);
  return this._config;
};

ScatterplotFacet.prototype.hasSelection = function() {
  return  ("from_x" in this._config && this._config.from_x !== 0) ||
  ("from_y" in this._config && this._config.from_y !== 0) ||
  ("to_x" in this._config && this._config.to_x !== 1) ||
  ("to_y" in this._config && this._config.to_y !== 1);
};

ScatterplotFacet.prototype._initializeUI = function() {
  var self = this;
  var container = this._div.empty().show();

  var facet_id = container.attr("id");

  this._div.empty().show().html(
      '<div class="facet-title">' +
      '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
      '<td width="1%"><a href="javascript:{}" title="'+$.i18n._('core-facets')["remove-facet"]+'" class="facet-title-remove" bind="removeButton">&nbsp;</a></td>' +
      '<td>' +
      '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">'+$.i18n._('core-facets')["reset"]+'</a>' +
      '<span bind="titleSpan"></span>' +
      '</td>' +
      '</tr></table></div>' +
      '</div>' +
      '<div class="facet-scatterplot-body" bind="bodyDiv">' +
      '<div class="facet-scatterplot-message" bind="messageDiv">'+$.i18n._('core-facets')["loading"]+'</div>' +
      '<table width="100%"><tr>' + 
      '<td>' +
      '<div class="facet-scatterplot-plot-container">' +
      '<div class="facet-scatterplot-plot" bind="plotDiv">' +
      '<img class="facet-scatterplot-image" bind="plotBaseImg" />' +
      '<img class="facet-scatterplot-image" bind="plotImg" />' +
      '</div>' +
      '</div>' +
      '</td>' +
      '<td class="facet-scatterplot-selectors-container" width="100%">' +
      '<div class="scatterplot-selectors" bind="selectors">' +
      '<div class="buttonset scatterplot-dim-selector">' +
      '<input type="radio" id="' + facet_id + '-dim-lin" name="' + facet_id + '-dim" value="lin"/><label class="dim-lin-label" for="' + facet_id + '-dim-lin" title="'+$.i18n._('core-facets')["linear-plot"]+'">'+$.i18n._('core-facets')["linear-plot-abbr"]+'</label>' +
      '<input type="radio" id="' + facet_id + '-dim-log" name="' + facet_id + '-dim" value="log"/><label class="dim-log-label" for="' + facet_id + '-dim-log" title="'+$.i18n._('core-facets')["logar-plot"]+'">'+$.i18n._('core-facets')["logar-plot-abbr"]+'</label>' +
      '</div>' + 
      '<div class="buttonset scatterplot-rot-selector">' +
      '<input type="radio" id="' + facet_id + '-rot-ccw"  name="' + facet_id + '-rot" value="ccw"/><label class="rot-ccw-label" for="' + facet_id + '-rot-ccw" title="'+$.i18n._('core-facets')["rotated-counter-clock"]+'">&nbsp;</label>' +
      '<input type="radio" id="' + facet_id + '-rot-none" name="' + facet_id + '-rot" value="none"/><label class="rot-none-label" for="' + facet_id + '-rot-none" title="'+$.i18n._('core-facets')["no-rotation"]+'">&nbsp;</label>' +
      '<input type="radio" id="' + facet_id + '-rot-cw"   name="' + facet_id + '-rot" value="cw"/><label class="rot-cw-label" for="' + facet_id + '-rot-cw" title="'+$.i18n._('core-facets')["rotated-clock"]+'">&nbsp;</label>' +
      '</div>' +
      '<div class="buttonset scatterplot-dot-selector">' +
      '<input type="radio" id="' + facet_id + '-dot-small"   name="' + facet_id + '-dot" value="small"/><label class="dot-small-label" for="' + facet_id + '-dot-small" title="'+$.i18n._('core-facets')["small-dot"]+'">&nbsp;</label>' +
      '<input type="radio" id="' + facet_id + '-dot-regular" name="' + facet_id + '-dot" value="regular"/><label class="dot-regular-label" for="' + facet_id + '-dot-regular" title="'+$.i18n._('core-facets')["regular-dot"]+'">&nbsp;</label>' +
      '<input type="radio" id="' + facet_id + '-dot-big"     name="' + facet_id + '-dot" value="big"/><label class="dot-big-label" for="' + facet_id + '-dot-big" title="'+$.i18n._('core-facets')["big-dot"]+'">&nbsp;</label>' +
      '</div>' +
      '<div class="scatterplot-export-plot"><a bind="exportPlotLink" class="action" target="_blank">'+$.i18n._('core-facets')["export-plot"]+'</a></div>' +
      '</div>' +
      '</td>' + 
      '</tr></table>' +
      '<div class="facet-scatterplot-status" bind="statusDiv"></div>' +
      '</div>'
  );
  this._elmts = DOM.bind(this._div);

  this._elmts.titleSpan.text(this._config.name);
  this._elmts.removeButton.click(function() { self._remove(); });
  this._elmts.resetButton.click(function() {
    self.reset();
    self._updateRest();
  });

  this._elmts.plotDiv.width(this._config.l + "px").height(this._config.l + "px");
  this._elmts.plotBaseImg.attr("src", this._formulateBaseImageUrl())
  .attr("width", this._config.l)
  .attr("height", this._config.l);
  this._elmts.plotImg.attr("src", this._formulateCurrentImageUrl())
  .attr("width", this._config.l)
  .attr("height", this._config.l);

  var ops = {
      instance: true,        
      handles: false,
      parent: this._elmts.plotDiv,
      fadeSpeed: 70,
      onSelectEnd: function(elmt, selection) {
        self._putSelectionOptions(selection);
        self._updateRest();
      }
  };

  this._fillSelectionOptions(ops);
  this._plotAreaSelector = this._elmts.plotImg.imgAreaSelect(ops);

  if (this._config.dim_x == 'lin' && this._config.dim_y == 'lin') {
    this._elmts.selectors.find("#" + facet_id + "-dim-lin").attr('checked','checked');
  } else if (this._config.dim_x == 'log' && this._config.dim_y == 'log') {
    this._elmts.selectors.find("#" + facet_id + "-dim-log").attr('checked','checked');
  }

  if (this._config.r == 'cw') {
    this._elmts.selectors.find("#" + facet_id + "-rot-cw").attr('checked','checked');
  } else if (this._config.r == 'ccw') {
    this._elmts.selectors.find("#" + facet_id + "-rot-ccw").attr('checked','checked');
  } else {
    this._elmts.selectors.find("#" + facet_id + "-rot-none").attr('checked','checked');
  }

  if (this._config.dot >= 1.2) {
    this._elmts.selectors.find("#" + facet_id + "-dot-big").attr('checked','checked');
  } else if (this._config.dot <= 0.4) {
    this._elmts.selectors.find("#" + facet_id + "-dot-small").attr('checked','checked');
  } else {
    this._elmts.selectors.find("#" + facet_id + "-dot-regular").attr('checked','checked');
  }

  this._elmts.selectors.find(".scatterplot-dim-selector").change(function() {
    var dim = $(this).find("input:checked").val();
    self._config.dim_x = dim;
    self._config.dim_y = dim;
    self.reset();
    self._updateRest();
    self.changePlot();
  });

  this._elmts.selectors.find(".scatterplot-rot-selector").change(function() {
    self._config.r = $(this).find("input:checked").val();
    self.reset();
    self._updateRest();
    self.changePlot();        
  });

  this._elmts.selectors.find(".scatterplot-dot-selector").change(function() {
    var dot_size = $(this).find("input:checked").val();
    if (dot_size == "small") {
      self._config.dot = 0.4;
    } else if (dot_size == "big") {
      self._config.dot = 1.4;
    } else {
      self._config.dot = 0.8;
    }
    self.changePlot();
  });

  this._elmts.selectors.find(".buttonset").buttonset();
};

ScatterplotFacet.prototype._fillSelectionOptions = function(ops) {
  if (this.hasSelection()) {
    ops.x1 = this._config.l * this._config.from_x;
    ops.x2 = this._config.l * this._config.to_x;

    ops.y1 = this._config.l - (this._config.l * this._config.to_y);
    ops.y2 = this._config.l - (this._config.l * this._config.from_y);
  } else {
    ops.x1 = ops.y1 = 0;
    ops.x2 = ops.y2 = this._config.l;
    ops.hide = true;
  }
};

ScatterplotFacet.prototype._putSelectionOptions = function(selection) {
  if (selection.height === 0 || selection.width === 0) {
    this.reset();
  } else {
    this._config.from_x = selection.x1 / this._config.l;
    this._config.to_x = selection.x2 / this._config.l;

    this._config.from_y = (this._config.l - selection.y2) / this._config.l;
    this._config.to_y = (this._config.l - selection.y1) / this._config.l;
  }
};

ScatterplotFacet.prototype._formulateCurrentImageUrl = function() {
  return this._formulateImageUrl(ui.browsingEngine.getJSON(false, this), { color: "ff6a00" });
};

ScatterplotFacet.prototype._formulateBaseImageUrl = function() {
  return this._formulateImageUrl({},{ color: "888888", dot : this._config.dot * 0.9 });
};

ScatterplotFacet.prototype._formulateExportImageUrl = function() {
  return this._formulateImageUrl(ui.browsingEngine.getJSON(false, this), { dot : this._config.dot * 5, l: 500, base_color: "888888" });
};

ScatterplotFacet.prototype._formulateImageUrl = function(engineConfig, conf) {
  var options = {};
  for (var p in this._config) {
    if (this._config.hasOwnProperty(p)) {        
      options[p] = this._config[p];
    }
  }
  for (var p in conf) {
    if (conf.hasOwnProperty(p)) {        
      options[p] = conf[p];
    }
  }
  var params = {
      project: theProject.id,
      engine: JSON.stringify(engineConfig), 
      plotter: JSON.stringify(options) 
  };
  return "command/core/get-scatterplot?" + $.param(params);
};

ScatterplotFacet.prototype.updateState = function(data) {
  if ("error" in data) {
    this._error = true;
    this._errorMessage = "error" in data ? data.error : $.i18n._('core-facets')["unknown-error"]+".";
  } else {
    this._error = false;

    // These are in 0 - 1 coordinates
    if ("from_x" in data) {
      this._config.from_x = Math.min(Math.max(data.from_x, 0), 1);
    } else {
      this._config.from_x = 0;
    }
    if ("to_x" in data) {
      this._config.to_x = Math.min(Math.max(data.to_x, data.from_x), 1);
    } else {
      this._config.to_x = 1;
    }

    if ("from_y" in data) {
      this._config.from_y = Math.min(Math.max(data.from_y, 0), 1);
    } else {
      this._config.from_y = 0;
    }
    if ("to_y" in data) {
      this._config.to_y = Math.min(Math.max(data.to_y, data.from_y), this._config.l);
    } else {
      this._config.to_y = 1;
    }

    if (this._plotAreaSelector) {
      var ops = {};
      this._fillSelectionOptions(ops);
      this._plotAreaSelector.setOptions(ops);
      this._plotAreaSelector.update();
    }
  }

  this.render();
};

ScatterplotFacet.prototype.changePlot = function() {
  this._elmts.plotBaseImg.attr("src", this._formulateBaseImageUrl());
  this._elmts.plotImg.attr("src", this._formulateCurrentImageUrl());
  this._elmts.exportPlotLink.attr("href", this._formulateExportImageUrl());
};

ScatterplotFacet.prototype.render = function() {
  if (!this._initializedUI) {
    this._initializeUI();
    this._initializedUI = true;
  }

  if (this._error) {
    this._elmts.messageDiv.text(this._errorMessage).show();
    this._elmts.plotDiv.hide();
    this._elmts.statusDiv.hide();
    return;
  }

  this._elmts.messageDiv.hide();
  this._elmts.plotDiv.show();
  this._elmts.statusDiv.show();

  this._elmts.plotImg.attr("src", this._formulateCurrentImageUrl());
  this._elmts.exportPlotLink.attr("href", this._formulateExportImageUrl());
};

ScatterplotFacet.prototype._remove = function() {
  ui.browsingEngine.removeFacet(this);

  this._div = null;
  this._config = null;
};

ScatterplotFacet.prototype._updateRest = function() {
  Refine.update({ engineChanged: true });
};
