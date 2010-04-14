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
        x_columnName : this._config.x_columnName,
        y_columnName : this._config.y_columnName,
        x_expression: this._config.x_expression,
        y_expression: this._config.y_expression,
        dot: this._config.dot,
        dim: this._config.dim,
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
        
    this._plot = new ScatterplotWidget(this._plotDiv, this._config); 
};

ScatterplotFacet.prototype.updateState = function(data) {
    if ("x_min" in data && "x_max" in data && "x_max" in data && "x_min" in data) {
        this._error = false;
        
        this._config.x_min = data.x_min;
        this._config.x_max = data.x_max;
        this._config.y_min = data.y_min;
        this._config.y_max = data.y_max;
        
        this._from_x = Math.max(data.from_x, this._config.x_min);
        if ("to_x" in data) {
            this._to_x = Math.min(data.to_x, this._config.x_max);
        } else {
            this._to_x = data.x_max;
        }

        this._from_y = Math.max(data.from_y, this._config.x_min);
        if ("to_y" in data) {
            this._to_y = Math.min(data.to_y, this._config.x_max);
        } else {
            this._to_y = data.x_max;
        }
        
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
        this._plotDiv.hide();
        this._statusDiv.hide();
        return;
    }
    
    this._messageDiv.hide();
    this._plotDiv.show();
    this._statusDiv.show();
        
    this._plot.update(
        this._config.x_min, 
        this._config.x_max, 
        this._x_from,
        this._x_to,
        this._config.y_min, 
        this._config.y_max, 
        this._y_from,
        this._y_to
    );
};

ScatterplotFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
};

ScatterplotFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};
