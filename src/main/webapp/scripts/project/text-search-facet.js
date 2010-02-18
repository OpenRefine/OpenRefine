function TextSearchFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._setDefaults();
    this._timerID = null;
    
    this._initializeUI();
}

TextSearchFacet.prototype._setDefaults = function() {
    this._query = null;
};

TextSearchFacet.prototype.getJSON = function() {
    var o = {
        type: "text",
        name: this._config.name,
        columnName: this._config.columnName,
        mode: this._config.mode,
        caseSensitive: this._config.caseSensitive,
        query: this._query
    };
    return o;
};

TextSearchFacet.prototype.hasSelection = function() {
    return this._query != null;
};

TextSearchFacet.prototype._initializeUI = function() {
    var self = this;
    var container = this._div.empty();
    
    var headerDiv = $('<div></div>').addClass("facet-title").appendTo(container);
    $('<span></span>').text(this._config.name).appendTo(headerDiv);
    
    var removeButton = $('<a href="javascript:{}"></a>').addClass("facet-choice-link").text("remove").click(function() {
        self._remove();
    }).prependTo(headerDiv);
    
    var bodyDiv = $('<div></div>').addClass("facet-text-body").appendTo(container);
    
    var input = $('<input />').appendTo(bodyDiv);
    input.keypress(function(evt) {
        self._query = this.value;
        self._scheduleUpdate();
    });
    input[0].focus();
};

TextSearchFacet.prototype.updateState = function(data) {
};

TextSearchFacet.prototype.render = function() {
    this._setRangeIndicators();
};

TextSearchFacet.prototype._reset = function() {
    this._setDefaults();
    this._updateRest();
};

TextSearchFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
    this._options = null;
};

TextSearchFacet.prototype._scheduleUpdate = function() {
    if (this._timerID == null) {
        var self = this;
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self._updateRest();
        }, 500);
    }
};

TextSearchFacet.prototype._updateRest = function() {
    ui.browsingEngine.update();
    ui.dataTableView.update(true);
};
