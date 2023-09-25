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

function BrowsingEngine(div, facetConfigs) {
  this._div = div;
  this._mode = theProject.recordModel.hasRecords ? "record-based" : "row-based";

  this._facets = [];
  this._initializeUI();

  if (facetConfigs.length > 0) {
    for (var i = 0; i < facetConfigs.length; i++) {
      var facetConfig = facetConfigs[i];
      var type = facetConfig.c.type;

      var elmt = this._createFacetContainer();
      var facet;
      switch (type) {
      case "range":
        facet = RangeFacet.reconstruct(elmt, facetConfig);
        break;
      case "timerange":
        facet = TimeRangeFacet.reconstruct(elmt, facetConfig);
        break;
      case "scatterplot":
        facet = ScatterplotFacet.reconstruct(elmt, facetConfig);
        break;
      case "text":
        facet = TextSearchFacet.reconstruct(elmt, facetConfig);
        break;
      default:
        facet = ListFacet.reconstruct(elmt, facetConfig);
      }

      this._facets.push({ elmt: elmt, facet: facet });
    }
  }
}

BrowsingEngine.prototype.resize = function() {
  if (this._facets.length > 0) {
    var body = this._div.find(".facets-container");
    var bodyPaddings = body.outerHeight(true) - body.height();
    var height = 
      this._div.height() -
      this._div.find(".browsing-panel-header").outerHeight(true) -
      bodyPaddings;

    body.css("height", height + "px");

    this._elmts.facets.sortable("refresh");
  }
};

BrowsingEngine.prototype.getFacetUIStates = function() {
  var f = [];
  for (var i = 0; i < this._facets.length; i++) {
    var facet = this._facets[i];
    f.push(facet.facet.getUIState());
  }
  return f;
};

BrowsingEngine.prototype._initializeUI = function() {
  var self = this;

  this._div.html(
    '<div class="browsing-panel-help" bind="help">' +
    '<h1>'+$.i18n('core-project/use-facets')+'</h1>' +
    '<p>'+$.i18n('core-project/use-to-select')+'</p>' +
    '<p>'+$.i18n('core-project/not-sure')+'<br /><a href="https://github.com/OpenRefine/OpenRefine/wiki/Screencasts" target="_blank"><b>'+$.i18n('core-project/watch-cast')+'</b></a></p>' +
    '</div>' +
    '<div class="browsing-panel-header" bind="header">' +
    '<div class="browsing-panel-errors" bind="errors"></div>' +
    '<div class="browsing-panel-indicator" bind="indicator">' +
    '<img src="images/small-spinner.gif" /> '+$.i18n('core-project/refreshing-facet')+'' +
    '</div>' +
    '<div class="browsing-panel-controls" bind="controls">' +
    '<div class="browsing-panel-controls-refresh">' +
    '<a href="javascript:{}" bind="refreshLink" class="button" title="'+$.i18n('core-project/update-facets')+'">'+$.i18n('core-buttons/refresh')+'</a>' +
    '</div>' +              
    '<a href="javascript:{}" bind="resetLink" class="button button-pill-left" title="'+$.i18n('core-project/clear-selection')+'">'+$.i18n('core-buttons/reset-all')+'</a>' +
    '<a href="javascript:{}" bind="removeLink" class="button button-pill-right" title="'+$.i18n('core-project/remove-all')+'">'+$.i18n('core-buttons/remove-all')+'</a>' +
    '</div>' +
    '</div>' +
    '<ul bind="facets" class="facets-container"></ul>'
  );
  this._elmts = DOM.bind(this._div);
  this._elmts.facets.sortable({
    handle: '.facet-title',
    update: function(event, ui) {
      self._updateFacetOrder();
    }
  });

  this._elmts.refreshLink.on('click',function() { self.update(); });
  this._elmts.resetLink.on('click',function() { self.reset(); });
  this._elmts.removeLink.on('click',function() { self.remove(); });
};

BrowsingEngine.prototype._updateFacetOrder = function() {
  var elmts = this._elmts.facets.children();
  var newFacets = [];
  for (var e = 0; e < elmts.length; e++) {
    var elmt = elmts[e];
    for (var f = 0; f < this._facets.length; f++) {
      if (elmt === this._facets[f].elmt[0]) {
        newFacets.push(this._facets[f]);
        break;
      }
    }
  }
  this._facets = newFacets;
};

BrowsingEngine.prototype.getMode = function() {
  return this._mode;
};

BrowsingEngine.prototype.setMode = function(mode) {
  if (this._mode != mode) {
    this._mode = mode;
    Refine.update({ engineChanged: true });
  }
};

BrowsingEngine.prototype.getJSON = function(keepUnrestrictedFacets, except) {
  var a = {
      facets: [],
      mode: this._mode
  };
  for (var i = 0; i < this._facets.length; i++) {
    var facet = this._facets[i];
    if ((keepUnrestrictedFacets || facet.facet.hasSelection()) && (facet.facet !== except)) {
      a.facets.push(facet.facet.getJSON());
    }
  }
  return a;
};

BrowsingEngine.prototype.addFacet = function(type, config, options) {
  var elmt = this._createFacetContainer();
  var facet;
  switch (type) {
  case "range":
    facet = new RangeFacet(elmt, config, options);
    break;
  case "timerange":
    facet = new TimeRangeFacet(elmt, config, options);
    break;
  case "scatterplot":
    facet = new ScatterplotFacet(elmt, config, options);
    break;
  case "text":
    facet = new TextSearchFacet(elmt, config, options);
    break;
  default:
    facet = new ListFacet(elmt, config, options);
  }

  this._facets.push({ elmt: elmt, facet: facet });

  ui.leftPanelTabs.tabs();
  ui.leftPanelTabs.on( "tabsactivate", ( event, ui ) =>  {
     let activeTabId = ui.newTab.children('a').attr("href");
     if (activeTabId === '#refine-tabs-facets') {
       for (let facet of this._facets) {
         if (facet.facet.render) {
           facet.facet.render();
         } else if (facet.facet.checkInitialHeight) {
           facet.facet.checkInitialHeight();
         }
       }
     }
  });

  Refine.update({ engineChanged: true });
};

BrowsingEngine.prototype._createFacetContainer = function() {
  return $('<li></li>').addClass("facet-container").attr("id","facet-" + this._facets.length).hide().appendTo(this._elmts.facets);
};

BrowsingEngine.prototype.removeFacet = function(facet) {
  var update = facet.hasSelection();
  for (var i = this._facets.length - 1;i >= 0; i--) {
    var facetRecord = this._facets[i];
    if (facetRecord.facet === facet) {
      this._facets.splice(i, 1);

      facetRecord.facet.dispose();

      // This makes really big facet disappear right away. If you just call remove()
      // then it takes a while for all the event handlers to get unwired, and the UI
      // appear frozen.
      facetRecord.elmt.hide();
      window.setTimeout(function() {
        facetRecord.elmt.remove();
      }, 300);

      break;
    }
  }

  for (var i = 0; i < this._facets.length; i++) {
    if (typeof this._facets[i].facet.update == "function") {
      this._facets[i].facet.update();
    }
  }

  if (update) {
    Refine.update({ engineChanged: true });
  } else if (this._facets.length === 0) {
    this._elmts.help.show();
    this._elmts.header.hide();
  }
};

BrowsingEngine.prototype.update = function(onDone) {
  var self = this;

  this._elmts.help.hide();

  this._elmts.header.show();
  this._elmts.controls.css("display", "none");
  this._elmts.indicator.css("display", "block");

  $.post(
    "command/core/compute-facets?" + $.param({ project: theProject.id }),
    { engine: JSON.stringify(this.getJSON(true)) },
    function(data) {
      if(data.code === "error") {
        var clearErr = $('#err-text').remove();
        var err = $('<div id="err-text">')
                  .text(data.message)
                  .appendTo(self._elmts.errors);
         self._elmts.errors.css("display", "block");
        if (onDone) {
          onDone();
        }
        return;
      }
      var facetData = data.facets;

      for (var i = 0; i < facetData.length; i++) {
        self._facets[i].facet.updateState(facetData[i]);
      }

      self._elmts.indicator.css("display", "none");
      self._elmts.errors.css("display", "none");
      if (self._facets.length > 0) {
        self._elmts.header.show();
        self._elmts.controls.css("display", "block");

        self.resize();
      } else {
        self._elmts.help.show();
      }

      if (onDone) {
        onDone();
      }
    },
    "json"
  );
};

BrowsingEngine.prototype.reset = function() {
  for (var i = 0; i < this._facets.length; i++) {
    this._facets[i].facet.reset();
  }

  Refine.update({ engineChanged: true });
};

BrowsingEngine.prototype.remove = function() {
  var oldFacets = this._facets;

  this._facets = [];

  for (var i = 0; i < oldFacets.length; i++) {
    var facet = oldFacets[i];
    facet.facet.dispose();
    facet.elmt.hide();
  }
  window.setTimeout(function() {
    for (var i = 0; i < oldFacets.length; i++) {
      oldFacets[i].elmt.remove();
    }
  }, 300);

  Refine.update({ engineChanged: true });
};
