function BrowsingEngine(div) {
    this._div = div;
    this.render();
    
    this._facets = [];
}

BrowsingEngine.prototype.render = function() {
    var self = this;
    var container = this._div.empty();
};

BrowsingEngine.prototype.getJSON = function() {
    var a = { facets: [] };
    for (var i = 0; i < this._facets.length; i++) {
        a.facets.push(this._facets[i].getJSON());
    }
    return a;
};
