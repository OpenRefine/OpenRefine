function BrowsingEngine(div) {
    this._div = div;
    this._facets = [];
    
    this._initializeUI();
}

BrowsingEngine.prototype._initializeUI = function() {
    var self = this;
    var container = this._div.empty();
};

BrowsingEngine.prototype.getJSON = function() {
    var a = { facets: [] };
    for (var i = 0; i < this._facets.length; i++) {
        a.facets.push(this._facets[i].facet.getJSON());
    }
    return a;
};

BrowsingEngine.prototype.addFacet = function(type, config) {
    var div = $('<div></div>').addClass("facet-container").appendTo(this._div);
    var facet;
    switch (type) {
        default:
            facet = new ListFacet(div, config, {});
    }
    
    this._facets.push({ elmt: div, facet: facet });
    this.update();
};

BrowsingEngine.prototype.update = function() {
    var self = this;
    
    $.post(
        "/command/compute-facets?" + $.param({ project: theProject.id }),
        { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
        function(data) {
            var facetData = data.facets;
            
            for (var i = 0; i < facetData.length; i++) {
                self._facets[i].facet.updateState(facetData[i]);
            }
        },
        "json"
    );
};
