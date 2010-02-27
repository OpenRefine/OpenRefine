function BrowsingEngine(div, facetConfigs) {
    this._div = div;
    this._facets = [];
    
    this._initializeUI();
    
    if (facetConfigs.length > 0) {
        for (var i = 0; i < facetConfigs.length; i++) {
            var facetConfig = facetConfigs[i];
            var type = facetConfig.c.type;
            
            var div = $('<div></div>').addClass("facet-container").appendTo(this._div);
            var facet;
            switch (type) {
                case "range":
                    facet = RangeFacet.reconstruct(div, facetConfig);
                    break;
                case "text":
                    facet = TextSearchFacet.reconstruct(div, facetConfig);
                    break;
                default:
                    facet = ListFacet.reconstruct(div, facetConfig);
            }
            
            this._facets.push({ elmt: div, facet: facet });
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
    var container = this._div.empty();
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
    var div = $('<div></div>').addClass("facet-container").appendTo(this._div);
    var facet;
    switch (type) {
        case "range":
            facet = new RangeFacet(div, config, options);
            break;
        case "text":
            facet = new TextSearchFacet(div, config, options);
            break;
        default:
            facet = new ListFacet(div, config, options);
    }
    
    this._facets.push({ elmt: div, facet: facet });
    this.update();
};

BrowsingEngine.prototype.removeFacet = function(facet) {
    var update = facet.hasSelection();
    for (var i = this._facets.length - 1;i >= 0; i--) {
        if (this._facets[i].facet === facet) {
            this._facets[i].elmt.remove();
            this._facets.splice(i, 1);
            break;
        }
    }
    
    if (update) {
        Gridworks.update({ engineChanged: true }, onFinallyDone);
    }
};

BrowsingEngine.prototype.update = function(onDone) {
    var self = this;
    
    $.post(
        "/command/compute-facets?" + $.param({ project: theProject.id }),
        { engine: JSON.stringify(this.getJSON(true)) },
        function(data) {
            var facetData = data.facets;
            
            for (var i = 0; i < facetData.length; i++) {
                self._facets[i].facet.updateState(facetData[i]);
            }
            
            if (onDone) {
                onDone();
            }
        },
        "json"
    );
};
