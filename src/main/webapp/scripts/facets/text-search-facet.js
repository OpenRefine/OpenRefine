function TextSearchFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._query = config.query || null;
    this._timerID = null;
    
    this._initializeUI();
}

TextSearchFacet.reconstruct = function(div, uiState) {
    return new TextSearchFacet(div, uiState.c, uiState.o);
};

TextSearchFacet.prototype.dispose = function() {
};

TextSearchFacet.prototype.reset = function() {
    this._query = null;
    this._div.find(".input-container input").each(function() { this.value = ""; });
};

TextSearchFacet.prototype.getUIState = function() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };
    
    return json;
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
    return this._query !== null;
};

TextSearchFacet.prototype._initializeUI = function() {
    var self = this;
    this._div.empty().html(
        '<div class="facet-title">' + 
            '<img bind="removeButton" src="images/close.png" title="Remove this facet" class="facet-choice-link" />' +
            '<span>' + this._config.name + '</span>' +
        '</div>' +
        '<div class="facet-text-body"><div class="grid-layout layout-tightest layout-full"><table>' +
            '<tr><td colspan="2"><div class="input-container"><input bind="input" /></div></td></tr>' +
            '<tr><td width="1%"><input type="checkbox" bind="caseSensitiveCheckbox" /></td><td>case sensitive</td></tr>' +
            '<tr><td width="1%"><input type="checkbox" bind="regexCheckbox" /></td><td>regular expression</td></tr>' +
        '</table></div></div>'
    );
    
    var elmts = DOM.bind(this._div);
    
    if (this._config.caseSensitive) {
        elmts.caseSensitiveCheckbox.attr("checked", "true");
    }
    if (this._config.mode == "regex") {
        elmts.regexCheckbox.attr("checked", "true");
    }
    
    elmts.removeButton.click(function() { self._remove(); });
    
    elmts.caseSensitiveCheckbox.bind("change", function() {
        self._config.caseSensitive = this.checked;
        if (self._query !== null && self._query.length > 0) {
            self._scheduleUpdate();
        }
    });
    elmts.regexCheckbox.bind("change", function() {
        self._config.mode = this.checked ? "regex" : "text";
        if (self._query !== null && self._query.length > 0) {
            self._scheduleUpdate();
        }
    });
    
    elmts.input.keyup(function(evt) {
        self._query = this.value;
        self._scheduleUpdate();
    }).focus();
};

TextSearchFacet.prototype.updateState = function(data) {
};

TextSearchFacet.prototype.render = function() {
    this._setRangeIndicators();
};

TextSearchFacet.prototype._reset = function() {
    this._query = null;
    this._updateRest();
};

TextSearchFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
    this._options = null;
};

TextSearchFacet.prototype._scheduleUpdate = function() {
    if (!this._timerID) {
        var self = this;
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self._updateRest();
        }, 500);
    }
};

TextSearchFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};
