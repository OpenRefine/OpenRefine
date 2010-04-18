function ScatterplotFacet(div, config, options) {
    console.log(div);
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
    this._plotAreaSelector.setOptions({ hide : true });
    this._plotAreaSelector.update();
};

ScatterplotFacet.reconstruct = function(div, uiState) {
    return new ScatterplotFacet(div, uiState.c, uiState.o);
};

ScatterplotFacet.prototype.dispose = function() {
    this._plotImg.imgAreaSelect({ hide : true });
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
    return ("from_x" in this._config);
};

ScatterplotFacet.prototype._initializeUI = function() {
    var self = this;
    var container = this._div.empty().show();
    
    var facet_id = container.attr("id");
    
    var headerDiv = $('<div>').addClass("facet-title").appendTo(container);
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
        
    var bodyDiv = $('<div>').addClass("facet-scatterplot-body").appendTo(container);
    
    this._messageDiv = $('<div>').text("Loading...").addClass("facet-scatterplot-message").appendTo(bodyDiv);
    
    this._plotDiv = $('<div>')
        .addClass("facet-scatterplot-plot")
        .width(this._config.l + "px")
        .height(this._config.l + "px")
        .appendTo(bodyDiv);
        
    this._plotBaseImg = $('<img>')
        .addClass("facet-scatterplot-image")
        .attr("src", this._formulateBaseImageUrl())
        .attr("width", this._config.l)
        .attr("height", this._config.l)
        .appendTo(this._plotDiv);
    
    this._plotImg = $('<img>')
        .addClass("facet-scatterplot-image")
        .attr("src", this._formulateCurrentImageUrl())
        .attr("width", this._config.l)
        .attr("height", this._config.l)
        .appendTo(this._plotDiv);
    
    var ops = {
        instance: true,        
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
    };

    if (this.hasSelection()) {
        ops.x1 = this._config.from_x;
        ops.y1 = this._config.from_y;
        ops.x2 = this._config.to_x;
        ops.y2 = this._config.to_y;
    }

    this._plotAreaSelector = this._plotImg.imgAreaSelect(ops);
    
    this._selectors = $('<div class="facet-scatterplot-selectors">' +
        '<div class="buttonset facet-scatterplot-dim-selector">' +
            '<input type="radio" id="' + facet_id + '-dim-lin" name="' + facet_id + '-dim" value="lin"/><label class="dim-lin-label" for="' + facet_id + '-dim-lin" title="Linear Plot">lin</label>' +
            '<input type="radio" id="' + facet_id + '-dim-log" name="' + facet_id + '-dim" value="log"/><label class="dim-log-label" for="' + facet_id + '-dim-log" title="Logarithmic Plot">log</label>' +
        '</div>' + 
        '<div class="buttonset facet-scatterplot-rot-selector">' +
            '<input type="radio" id="' + facet_id + '-rot-ccw"  name="' + facet_id + '-rot" value="ccw"/><label class="rot-ccw-label" for="' + facet_id + '-rot-ccw" title="Rotated 45° Counter-Clockwise">&nbsp;</label>' +
            '<input type="radio" id="' + facet_id + '-rot-none" name="' + facet_id + '-rot" value="none"/><label class="rot-none-label" for="' + facet_id + '-rot-none" title="No rotation">&nbsp;</label>' +
            '<input type="radio" id="' + facet_id + '-rot-cw"   name="' + facet_id + '-rot" value="cw"/><label class="rot-cw-label" for="' + facet_id + '-rot-cw" title="Rotated 45° Clockwise">&nbsp;</label>' +
        '</div>' +
        '<div class="buttonset facet-scatterplot-dot-selector">' +
            '<input type="radio" id="' + facet_id + '-dot-small"   name="' + facet_id + '-dot" value="small"/><label class="dot-small-label" for="' + facet_id + '-dot-small" title="Small Dot Size">&nbsp;</label>' +
            '<input type="radio" id="' + facet_id + '-dot-regular" name="' + facet_id + '-dot" value="regular"/><label class="dot-regular-label" for="' + facet_id + '-dot-regular" title="Regular Dot Size">&nbsp;</label>' +
            '<input type="radio" id="' + facet_id + '-dot-big"     name="' + facet_id + '-dot" value="big"/><label class="dot-big-label" for="' + facet_id + '-dot-big" title="Big Dot Size">&nbsp;</label>' +
        '</div>' +
    '</div>');
    
    if (this._config.dim_x == 'lin' && this._config.dim_y == 'lin') {
        this._selectors.find("#" + facet_id + "-dim-lin").attr('checked','checked');
    } else if (this._config.dim_x == 'log' && this._config.dim_y == 'log') {
        this._selectors.find("#" + facet_id + "-dim-log").attr('checked','checked');
    }

    if (this._config.r == 'cw') {
        this._selectors.find("#" + facet_id + "-rot-cw").attr('checked','checked');
    } else if (this._config.r == 'ccw') {
        this._selectors.find("#" + facet_id + "-rot-ccw").attr('checked','checked');
    } else {
        this._selectors.find("#" + facet_id + "-rot-none").attr('checked','checked');
    }

    if (this._config.dot >= 1.2) {
        this._selectors.find("#" + facet_id + "-dot-big").attr('checked','checked');
    } else if (this._config.dot <= 0.4) {
        this._selectors.find("#" + facet_id + "-dot-small").attr('checked','checked');
    } else {
        this._selectors.find("#" + facet_id + "-dot-regular").attr('checked','checked');
    }
    
    this._selectors.find(".facet-scatterplot-dim-selector").change(function() {
        var dim = $(this).find("input:checked").val();
        self._config.dim_x = dim;
        self._config.dim_y = dim;
        self.changePlot();
    });

    this._selectors.find(".facet-scatterplot-rot-selector").change(function() {
        self._config.r = $(this).find("input:checked").val();
        self.changePlot();        
    });

    this._selectors.find(".facet-scatterplot-dot-selector").change(function() {
        var dot_size = $(this).find("input:checked").val();
        if (dot_size == "small") {
            self._config.dot = 0.4;
        } else if (dot_size == "big") {
            self._config.dot = 1.4;
        } else {
            self._config.dot = 0.8;
        }
        self.changePlot();
    });
    
    this._selectors.find("#" + facet_id + "-dim-lin").css
    this._selectors.find(".buttonset").buttonset();
        
    var plotTable = $(
        '<table width="100%"><tr>' + 
            '<td class="facet-scatterplot-plot-container"></td>' +
            '<td class="facet-scatterplot-selectors-container" width="100%"></td>' + 
        '</tr></table>'
    );
    
    plotTable.find(".facet-scatterplot-plot-container").append(this._plotDiv);
    plotTable.find(".facet-scatterplot-selectors-container").append(this._selectors);
    plotTable.appendTo(bodyDiv);
    
    this._statusDiv = $('<div>').addClass("facet-scatterplot-status").appendTo(bodyDiv);
};

ScatterplotFacet.prototype._formulateCurrentImageUrl = function() {
    return this._formulateImageUrl(ui.browsingEngine.getJSON(false, this), { color: "ff0000" });
};

ScatterplotFacet.prototype._formulateBaseImageUrl = function() {
    return this._formulateImageUrl({},{ color: "888888", dot : this._config.dot * 0.9 });
}

ScatterplotFacet.prototype._formulateImageUrl = function(engineConfig, conf) {
    for (var p in conf) {
        this._config[p] = conf[p];
    }
    var params = {
        project: theProject.id,
        engine: JSON.stringify(engineConfig), 
        plotter: JSON.stringify(this._config) 
    };
    return "/command/get-scatterplot?" + $.param(params);
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

ScatterplotFacet.prototype.changePlot = function() {
    this._plotBaseImg.attr("src", this._formulateBaseImageUrl());
    this._plotImg.attr("src", this._formulateCurrentImageUrl());
    this._updateRest();
}

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
    
    this._plotImg.attr("src", this._formulateCurrentImageUrl());
};

ScatterplotFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
};

ScatterplotFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};
