function BrowsingEngine(div, facetConfigs) {
    this._div = div;
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
                case "text":
                    facet = TextSearchFacet.reconstruct(elmt, facetConfig);
                    break;
                default:
                    facet = ListFacet.reconstruct(elmt, facetConfig);
            }
            
            this._facets.push({ elmt: elmt, facet: facet });
        }
        this.update();
    }
}

BrowsingEngine.prototype.getFacetUIStates = function() {
    var f = [];
    for (var i = 0; i < this._facets.length; i++) {
        var facet = this._facets[i];
        f.push(facet.facet.getUIState());
    }
    return f;
}

BrowsingEngine.prototype._initializeUI = function() {
    var self = this;
    
    this._div.html(
        '<div class="browsing-panel-indicator" bind="indicator"><img src="images/small-spinner.gif" /> refreshing facets ...</div>' +
        '<div class="browsing-panel-controls" bind="controls"><a href="javascript:{}" bind="refreshLink">refresh facets</a></div>' +
        '<ul bind="facets" class="facets-container"></ul>'
    );
    this._elmts = DOM.bind(this._div);
    this._elmts.facets.sortable({
        handle: '.facet-title',
        update: function(event, ui) {
            self._updateFacetOrder();
        }
    });
    this._elmts.facets.disableSelection();
    
    this._elmts.refreshLink.click(function() { self.update(); });
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

BrowsingEngine.prototype.getJSON = function(keepUnrestrictedFacets) {
    var a = { facets: [] };
    for (var i = 0; i < this._facets.length; i++) {
        var facet = this._facets[i];
        if (keepUnrestrictedFacets || facet.facet.hasSelection()) {
            a.facets.push(this._facets[i].facet.getJSON());
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
        case "text":
            facet = new TextSearchFacet(elmt, config, options);
            break;
        default:
            facet = new ListFacet(elmt, config, options);
    }
    
    this._facets.push({ elmt: elmt, facet: facet });
    this.update();
};

BrowsingEngine.prototype._createFacetContainer = function() {
    return $('<li></li>').addClass("facet-container").appendTo(this._elmts.facets);
};

BrowsingEngine.prototype.removeFacet = function(facet) {
    var update = facet.hasSelection();
    for (var i = this._facets.length - 1;i >= 0; i--) {
        if (this._facets[i].facet === facet) {
            this._facets[i].elmt.remove();
            this._facets.splice(i, 1);console.log("removed");
            break;
        }
    }
    
    if (update) {
        Gridworks.update({ engineChanged: true });
    }
};

BrowsingEngine.prototype.update = function(onDone) {
    var self = this;
    
    this._elmts.controls.hide();
    this._elmts.indicator.show();
    
    $.post(
        "/command/compute-facets?" + $.param({ project: theProject.id }),
        { engine: JSON.stringify(this.getJSON(true)) },
        function(data) {
            var facetData = data.facets;
            
            for (var i = 0; i < facetData.length; i++) {
                self._facets[i].facet.updateState(facetData[i]);
            }
            
            self._elmts.indicator.hide();
            if (self._facets.length > 0) {
                self._elmts.controls.show();
            }
            
            if (onDone) {
                onDone();
            }
        },
        "json"
    );
};
