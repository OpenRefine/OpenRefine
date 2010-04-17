function ScatterplotFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._error = false;
    this._initializedUI = false;
}

ScatterplotFacet.prototype.reset = function() {
    delete this._config.from_x;
    delete this._config.from_y;
    delete this._config.to_x;
    delete this._config.to_y;
    this._plotImg.imgAreaSelect({ hide : true});
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
    this._config.type = "scatterplot";
    return this._config;
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
    
    var params = {
        project: theProject.id,
        engine: JSON.stringify(ui.browsingEngine.getJSON()), 
        plotter: JSON.stringify(this._config) 
    };
    var url = "/command/get-scatterplot?" + $.param(params);
    
    this._messageDiv = $('<div>').text("Loading...").addClass("facet-scatterplot-message").appendTo(bodyDiv);
    this._plotDiv = $('<div>').addClass("facet-scatterplot-plot").appendTo(bodyDiv);
    this._plotImg = $('<img>')
        .addClass("facet-scatterplot-image")
        .attr("src",url)
        .attr("width", this._config.l)
        .attr("height", this._config.l)
        .imgAreaSelect({ 
            handles: false,
            fadeSpeed: 70,
            onSelectEnd: function(elmt, selection) {
                if (selection.height == 0 || selection.width == 0) {
                    self.reset();
                } else {
                    self._config.from_x = selection.x1;
                    self._config.to_x = selection.x2 - 2;
                    self._config.from_y = self._config.l - selection.y2 + 2;
                    self._config.to_y = self._config.l - selection.y1 - 1;
                }
                self._updateRest();
            }
        }).appendTo(this._plotDiv);
    this._statusDiv = $('<div>').addClass("facet-scatterplot-status").appendTo(bodyDiv);
};

ScatterplotFacet.prototype.updateState = function(data) {
    if ("min_x" in data && "max_x" in data && "max_x" in data && "min_x" in data) {
        this._error = false;
        
        this._config.min_x = data.min_x;
        this._config.max_x = data.max_x;
        this._config.min_y = data.min_y;
        this._config.max_y = data.max_y;
        
        this._config.from_x = Math.max(data.from_x, this._config.min_x);
        if ("to_x" in data) {
            this._config.to_x = Math.min(data.to_x, this._config.max_x);
        } else {
            this._config.to_x = data.max_x;
        }

        this._config.from_y = Math.max(data.from_y, this._config.min_x);
        if ("to_y" in data) {
            this._config.to_y = Math.min(data.to_y, this._config.max_x);
        } else {
            this._config.to_y = data.max_x;
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
};

ScatterplotFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
};

ScatterplotFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};
