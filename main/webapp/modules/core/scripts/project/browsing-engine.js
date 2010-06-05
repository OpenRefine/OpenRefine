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
            this._div.find(".browsing-panel-modes").outerHeight(true) -
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
            '<h1>Explore data ...</h1>' +
            '<p>by choosing a facet or filter method from the menus at the top of each column.</p>' +
            '<p>Not sure how to get started? <a href="http://vimeo.com/groups/gridworks/videos" target="_blank">Watch these screencasts</a>.</p>' +
        '</div>' +
        '<div class="browsing-panel-modes">' +
            'Browse by ' +
            '<span bind="modeSelectors">' + 
                '<input type="radio" id="browsing-panel-mode-row-based" name="browsing-panel-mode" value="row-based" /><label for="browsing-panel-mode-row-based">rows</label>' +
                '<input type="radio" id="browsing-panel-mode-record-based" name="browsing-panel-mode" value="record-based" /><label for="browsing-panel-mode-record-based">records</label>' +
            '</span>' +
        '</div>' +
        '<div class="browsing-panel-header" bind="header">' +
            '<div class="browsing-panel-indicator" bind="indicator">' +
                '<img src="images/small-spinner.gif" /> refreshing facets ...' +
            '</div>' +
            '<div class="browsing-panel-controls" bind="controls"><div class="grid-layout layout-tightest layout-full"><table>' +
                '<tr>' +
                    '<td>' +
                        '<a href="javascript:{}" bind="refreshLink" class="action" title="Make sure all facets are up-to-date">Refresh</a>' +
                    '</td>' +
                    '<td width="1%">' +
                        '<a href="javascript:{}" bind="resetLink" class="action" title="Clear selection in all facets">Reset&nbsp;All</a>' +
                    '</td>' +
                    '<td width="1%">' +
                        '<a href="javascript:{}" bind="removeLink" class="action" title="Remove all facets">Remove&nbsp;All</a>' +
                    '</td>' +
                '</tr>' +
            '</table></div></div>' +
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
    
    $("#browsing-panel-mode-" + 
        (theProject.recordModel.hasRecords ? 'record-based' : 'row-based')).attr("checked", "checked");
        
    this._elmts.modeSelectors.buttonset();
    this._elmts.modeSelectors.find("input").change(function() {
        Gridworks.update({ engineChanged: true });
    });
    
    this._elmts.refreshLink.click(function() { self.update(); });
    this._elmts.resetLink.click(function() { self.reset(); });
    this._elmts.removeLink.click(function() { self.remove(); });
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

BrowsingEngine.prototype.getJSON = function(keepUnrestrictedFacets, except) {
    var a = {
        facets: [],
        mode: this._elmts.modeSelectors.find("input:checked")[0].value
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
    
    Gridworks.update({ engineChanged: true });
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
        Gridworks.update({ engineChanged: true });
    }
};

BrowsingEngine.prototype.update = function(onDone) {
    var self = this;
    
    this._elmts.help.hide();
    
    this._elmts.header.show();
    this._elmts.controls.css("visibility", "hidden");
    this._elmts.indicator.css("visibility", "visible");
    
    $.post(
        "/command/compute-facets?" + $.param({ project: theProject.id }),
        { engine: JSON.stringify(this.getJSON(true)) },
        function(data) {
            var facetData = data.facets;
            
            for (var i = 0; i < facetData.length; i++) {
                self._facets[i].facet.updateState(facetData[i]);
            }
            
            self._elmts.indicator.css("visibility", "hidden");
            if (self._facets.length > 0) {
                self._elmts.header.show();
                self._elmts.controls.css("visibility", "visible");
                
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
    
    Gridworks.update({ engineChanged: true });
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
    
    Gridworks.update({ engineChanged: true });
};
