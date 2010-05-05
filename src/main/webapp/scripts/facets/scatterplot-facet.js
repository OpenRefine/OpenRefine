function ScatterplotFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._error = false;
    this._initializedUI = false;
}

ScatterplotFacet.prototype.update = function() {
    this._plotAreaSelector.update();
};

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
    this._elmts.plotImg.imgAreaSelect({ hide : true });
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
    var dot = this._config.dot;
    if (typeof dot == 'number') this._config.dot.toFixed(2);
    return this._config;
};

ScatterplotFacet.prototype.hasSelection = function() {
    return  ("from_x" in this._config && this._config.from_x !== 0) ||
        ("from_y" in this._config && this._config.from_y !== 0) ||
        ("to_x" in this._config && this._config.to_x != this._config.l) ||
        ("to_y" in this._config && this._config.to_y != this._config.l);
};

ScatterplotFacet.prototype._initializeUI = function() {
    var self = this;
    var container = this._div.empty().show();
    
    var facet_id = container.attr("id");
    
    this._div.empty().show().html(
        '<div class="facet-title">' +
            '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
                '<td width="1%"><a href="javascript:{}" title="Remove this facet" class="facet-title-remove" bind="removeButton">&nbsp;</a></td>' +
                '<td>' +
                    '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">reset</a>' +
                    '<span bind="titleSpan"></span>' +
                '</td>' +
            '</tr></table></div>' +
        '</div>' +
        '<div class="facet-scatterplot-body" bind="bodyDiv">' +
            '<div class="facet-scatterplot-message" bind="messageDiv">Loading...</div>' +
            '<table width="100%"><tr>' + 
                '<td>' +
                    '<div class="facet-scatterplot-plot-container">' +
                        '<div class="facet-scatterplot-plot" bind="plotDiv">' +
                            '<img class="facet-scatterplot-image" bind="plotBaseImg" />' +
                            '<img class="facet-scatterplot-image" bind="plotImg" />' +
                        '</div>' +
                    '</div>' +
                '</td>' +
                '<td class="facet-scatterplot-selectors-container" width="100%">' +
                    '<div class="facet-scatterplot-selectors" bind="selectors">' +
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
                    '</div>' +
                '</td>' + 
            '</tr></table>' +
            '<div class="facet-scatterplot-status" bind="statusDiv"></div>' +
        '</div>'
    );
    this._elmts = DOM.bind(this._div);
    
    this._elmts.titleSpan.text(this._config.name);
    this._elmts.removeButton.click(function() { self._remove(); });
    this._elmts.resetButton.click(function() {
        self.reset();
        self._updateRest();
    });
    
    this._elmts.plotDiv.width(this._config.l + "px").height(this._config.l + "px");
    this._elmts.plotBaseImg.attr("src", this._formulateBaseImageUrl())
        .attr("width", this._config.l)
        .attr("height", this._config.l);
    this._elmts.plotImg.attr("src", this._formulateCurrentImageUrl())
        .attr("width", this._config.l)
        .attr("height", this._config.l);
    
    var ops = {
        instance: true,        
        handles: false,
        parent: this._elmts.plotDiv,
        fadeSpeed: 70,
        onSelectEnd: function(elmt, selection) {
            if (selection.height === 0 || selection.width === 0) {
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
    this._plotAreaSelector = this._elmts.plotImg.imgAreaSelect(ops);
    
    if (this._config.dim_x == 'lin' && this._config.dim_y == 'lin') {
        this._elmts.selectors.find("#" + facet_id + "-dim-lin").attr('checked','checked');
    } else if (this._config.dim_x == 'log' && this._config.dim_y == 'log') {
        this._elmts.selectors.find("#" + facet_id + "-dim-log").attr('checked','checked');
    }

    if (this._config.r == 'cw') {
        this._elmts.selectors.find("#" + facet_id + "-rot-cw").attr('checked','checked');
    } else if (this._config.r == 'ccw') {
        this._elmts.selectors.find("#" + facet_id + "-rot-ccw").attr('checked','checked');
    } else {
        this._elmts.selectors.find("#" + facet_id + "-rot-none").attr('checked','checked');
    }

    if (this._config.dot >= 1.2) {
        this._elmts.selectors.find("#" + facet_id + "-dot-big").attr('checked','checked');
    } else if (this._config.dot <= 0.4) {
        this._elmts.selectors.find("#" + facet_id + "-dot-small").attr('checked','checked');
    } else {
        this._elmts.selectors.find("#" + facet_id + "-dot-regular").attr('checked','checked');
    }
    
    this._elmts.selectors.find(".facet-scatterplot-dim-selector").change(function() {
        var dim = $(this).find("input:checked").val();
        self._config.dim_x = dim;
        self._config.dim_y = dim;
        self.changePlot();
    });

    this._elmts.selectors.find(".facet-scatterplot-rot-selector").change(function() {
        self._config.r = $(this).find("input:checked").val();
        self.changePlot();        
    });

    this._elmts.selectors.find(".facet-scatterplot-dot-selector").change(function() {
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
    
    this._elmts.selectors.find(".buttonset").buttonset();
};

ScatterplotFacet.prototype._formulateCurrentImageUrl = function() {
    return this._formulateImageUrl(ui.browsingEngine.getJSON(false, this), { color: "ff6a00" });
};

ScatterplotFacet.prototype._formulateBaseImageUrl = function() {
    return this._formulateImageUrl({},{ color: "888888", dot : this._config.dot * 0.9 });
};

ScatterplotFacet.prototype._formulateImageUrl = function(engineConfig, conf) {
    for (var p in conf) {
        if (conf.hasOwnProperty(p)) {        
            this._config[p] = conf[p];
        }
    }
    var params = {
        project: theProject.id,
        engine: JSON.stringify(engineConfig), 
        plotter: JSON.stringify(this._config) 
    };
    return "/command/get-scatterplot?" + $.param(params);
};

ScatterplotFacet.prototype.updateState = function(data) {
    if ("min_x" in data && "max_x" in data && "max_y" in data && "min_y" in data) {
        this._error = false;
        
        this._config.min_x = data.min_x;
        this._config.max_x = data.max_x;
        this._config.min_y = data.min_y;
        this._config.max_y = data.max_y;
        
        if ("from_x" in data) {
            this._config.from_x = Math.max(data.from_x, 0);
        }
        if ("to_x" in data) {
            this._config.to_x = Math.min(data.to_x, this._config.l);
        }

        if ("from_y" in data) {
            this._config.from_y = Math.max(data.from_y, 0);
        }
        if ("to_y" in data) {
            this._config.to_y = Math.min(data.to_y, this._config.l);
        }
    } else {
        this._error = true;
        this._errorMessage = "error" in data ? data.error : "Unknown error.";
    }
    
    this.render();
};

ScatterplotFacet.prototype.changePlot = function() {
    this._elmts.plotBaseImg.attr("src", this._formulateBaseImageUrl());
    this._elmts.plotImg.attr("src", this._formulateCurrentImageUrl());
    this._updateRest();
};

ScatterplotFacet.prototype.render = function() {
    if (!this._initializedUI) {
        this._initializeUI();
        this._initializedUI = true;
    }
    
    if (this._error) {
        this._elmts.messageDiv.text(this._errorMessage).show();
        this._elmts.plotDiv.hide();
        this._elmts.statusDiv.hide();
        return;
    }
    
    this._elmts.messageDiv.hide();
    this._elmts.plotDiv.show();
    this._elmts.statusDiv.show();
    
    this._elmts.plotImg.attr("src", this._formulateCurrentImageUrl());
};

ScatterplotFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
};

ScatterplotFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};
