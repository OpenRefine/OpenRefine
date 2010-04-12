function ScatterplotFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._from_x = ("from_x" in this._config) ? this._config.from_x : null;
    this._to_x = ("to_x" in this._config) ? this._config.to_x : null;
    this._from_y = ("from_y" in this._config) ? this._config.from_y : null;
    this._to_y = ("to_y" in this._config) ? this._config.to_y : null;
    
    this._error = false;
    this._initializedUI = false;
}

ScatterplotFacet.prototype.reset = function() {
    // TODO
};

ScatterplotFacet.reconstruct = function(div, uiState) {
    return new ScatterplotFacet(div, uiState.c, uiState.o);
};

ScatterplotFacet.prototype.getUIState = function() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };
    
    return json;
};


ScatterplotFacet.prototype.getJSON = function() {
    var o = {
        type: "scatterplot",
        name: this._config.name,
        mode: this._config.mode,
        expression: this._config.expression,
        x_column : this._config.x_column,
        y_column : this._config.y_column,
    };
        
    return o;
};

ScatterplotFacet.prototype.hasSelection = function() {
    // TODO
};

ScatterplotFacet.prototype._initializeUI = function() {
    var self = this;
    var container = this._div.empty();
    
    var headerDiv = $('<div></div>').addClass("facet-title").appendTo(container);
    $('<span></span>').text(this._config.name).appendTo(headerDiv);
    
    var resetButton = $('<a href="javascript:{}"></a>').addClass("facet-choice-link").text("reset").click(function() {
        self.reset();
        self._updateRest();
    }).prependTo(headerDiv);
    
    var removeButton = $('<img>')
        .attr("src", "images/close.png")
        .attr("title", "Remove this facet")
        .addClass("facet-choice-link")
        .click(function() {
            self._remove();
        }).prependTo(headerDiv);
        
    var bodyDiv = $('<div></div>').addClass("facet-scatterplot-body").appendTo(container);
    
    this._messageDiv = $('<div>').text("Loading...").addClass("facet-scatterplot-message").appendTo(bodyDiv);
    this._plotDiv = $('<div>').addClass("facet-scatterplot-plot").appendTo(bodyDiv);
    this._statusDiv = $('<div>').addClass("facet-scatterplot-status").appendTo(bodyDiv);
    
    this._plot = new ScatterplotWidget(this._plotDiv, { binColors: [ "#ccccff", "#6666ff" ] });
};

ScatterplotFacet.prototype.updateState = function(data) {
    if ("min" in data && "max" in data) {
        this._error = false;
        
        this._config.min = data.min;
        this._config.max = data.max;
        this._config.step = data.step;
        this._baseBins = data.baseBins;
        this._bins = data.bins;
        
        switch (this._config.mode) {
        case "min":
            this._from = Math.max(data.from, this._config.min);
            break;
        case "max":
            this._to = Math.min(data.to, this._config.max);
            break;
        default:
            this._from = Math.max(data.from, this._config.min);
            if ("to" in data) {
                this._to = Math.min(data.to, this._config.max);
            } else {
                this._to = data.max;
            }
        }
        
        this._numericCount = data.numericCount;
        this._nonNumericCount = data.nonNumericCount;
        this._blankCount = data.blankCount;
        this._errorCount = data.errorCount;
    } else {
        this._error = true;
        this._errorMessage = "error" in data ? data.error : "Unknown error.";
    }
    
    this.render();
};

ScatterplotFacet.prototype.render = function() {
    if (!this._initializedUI) {
        this._initializeUI();
        this._initializedUI = true;
    }
    
    if (this._error) {
        this._messageDiv.text(this._errorMessage).show();
        this._sliderDiv.hide();
        this._histogramDiv.hide();
        this._statusDiv.hide();
        this._otherChoicesDiv.hide();
        return;
    }
    
    this._messageDiv.hide();
    this._plotDiv.show();
    this._statusDiv.show();
        
    this._plot.update(
        this._config.min, 
        this._config.max, 
        this._config.step, 
        [ this._baseBins, this._bins ],
        this._from,
        this._to
    );
};

ScatterplotFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
    this._data = null;
};

ScatterplotFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};
