function RangeFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._from = ("from" in this._config) ? this._config.from : null;
    this._to = ("to" in this._config) ? this._config.to : null;
    
    this._error = false;
    this._initializedUI = false;
}

RangeFacet.prototype._setDefaults = function() {
    switch (this._config.mode) {
    case "min":
        this._from = this._config.min;
        break;
    case "max":
        this._to = this._config.max;
        break;
    default:
        this._from = this._config.min;
        this._to = this._config.max;
    }
    
};

RangeFacet.reconstruct = function(div, uiState) {
    return new RangeFacet(div, uiState.c, uiState.o);
};

RangeFacet.prototype.getUIState = function() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };
    
    return json;
}


RangeFacet.prototype.getJSON = function() {
    var o = {
        type: "range",
        name: this._config.name,
        mode: this._config.mode,
        expression: this._config.expression,
        columnName: this._config.columnName
    };
    
    if (this._config.mode == "min" || this._config.mode == "range") {
        if (this._from != null) {
            o.from = this._from;
        }
    }
    if (this._config.mode == "max" || this._config.mode == "range") {
        if (this._to != null) {
            o.to = this._to;
        }
    }
    
    return o;
};

RangeFacet.prototype.hasSelection = function() {
    switch (this._config.mode) {
    case "min":
        return this._from != null && (!this._initializedUI || this._from > this._config.min);
    case "max":
        return this._to != null && (!this._initializedUI || this._to < this._config.max);
    default:
        return (this._from != null && (!this._initializedUI || this._from > this._config.min)) ||
            (this._to != null && (!this._initializedUI || this._to < this._config.max));
    }
};

RangeFacet.prototype._initializeUI = function() {
    var self = this;
    var container = this._div.empty();
    
    var headerDiv = $('<div></div>').addClass("facet-title").appendTo(container);
    $('<span></span>').text(this._config.name).appendTo(headerDiv);
    
    var resetButton = $('<a href="javascript:{}"></a>').addClass("facet-choice-link").text("reset").click(function() {
        switch (self._config.mode) {
        case "min":
            self._from = self._config.min;
            self._sliderDiv.slider("value", self._from);
            break;
        case "max":
            self._to = self._config.max;
            self._sliderDiv.slider("value", self._to);
            break;
        default:
            self._from = self._config.min;
            self._to = self._config.max;
            self._sliderDiv.slider("values", 0, self._from);
            self._sliderDiv.slider("values", 1, self._to);
        }
        self._setRangeIndicators();
        self._updateRest();
    }).prependTo(headerDiv);
    
    var removeButton = $('<img>')
        .attr("src", "images/close.png")
        .attr("title", "Remove this facet")
        .addClass("facet-choice-link")
        .click(function() {
            self._remove();
        }).prependTo(headerDiv);
        
    var bodyDiv = $('<div></div>').addClass("facet-range-body").appendTo(container);
    
    if (this._error) {
        return;
    }
    
    this._histogramDiv = $('<div></div>').addClass("facet-range-histogram").appendTo(bodyDiv);
    this._sliderDiv = $('<div></div>').addClass("facet-range-slider").appendTo(bodyDiv);
    this._statusDiv = $('<div></div>').addClass("facet-range-status").appendTo(bodyDiv);
    
    var onSlide = function(event, ui) {
        switch (self._config.mode) {
        case "min":
            self._from = ui.value;
            break;
        case "max":
            self._to = ui.value;
            break;
        default:
            self._from = ui.values[0];
            self._to = ui.values[1];
        }
        self._setRangeIndicators();
    };
    var onStop = function() {
        self._updateRest();
    };
    var sliderConfig = {
        range: "max",
        min: this._config.min,
        max: this._config.max,
        value: 2,
        stop: onStop,
        slide: onSlide
    };
    if ("step" in this._config) {
        sliderConfig.step = this._config.step;
    }
        
    switch (this._config.mode) {
    case "min":
        sliderConfig.range = "max";
        sliderConfig.value = this._from;
        break;
    case "max":
        sliderConfig.range = "min";
        sliderConfig.value = this._to;
        break;
    default:
        sliderConfig.range = true;
        sliderConfig.values = [ this._from, this._to ];
    }
    
    this._sliderDiv.slider(sliderConfig);
    this._setRangeIndicators();
};

RangeFacet.prototype._setRangeIndicators = function() {
    var text;
    switch (this._config.mode) {
    case "min":
        text = "At least " + this._from;
        break;
    case "max":
        text = "At most " + this._to;
        break;
    default:
        text = this._from + " to " + this._to;
    }
    this._statusDiv.text(text);
};

RangeFacet.prototype.updateState = function(data) {
    if ("min" in data && "max" in data) {
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
    } else {
        this._error = true;
    }
    
    this.render();
};

RangeFacet.prototype.render = function() {
    if (!this._initializedUI) {
        this._initializeUI();
        this._initializedUI = true;
    }
    if (this._error) {
        return;
    }
    
    this._sliderDiv.slider("option", "min", this._config.min);
    this._sliderDiv.slider("option", "max", this._config.max);
    this._sliderDiv.slider("option", "step", this._config.step);
    
    var max = 0;
    for (var i = 0; i < this._baseBins.length; i++) {
        max = Math.max(max, this._baseBins[i]);
    }
    
    if (max == 0) {
        this._histogramDiv.hide();
    } else {
        var values = [];
        var diffs = [];
        
        for (var i = 0; i < this._baseBins.length; i++) {
            var v = Math.ceil(100 * this._bins[i] / max);
            var diff = Math.ceil(100 * this._baseBins[i] / max) - v;
            
            values.push(v == 0 ? 0 : Math.max(2, v)); // use min 2 to make sure something shows up
            diffs.push(diff == 0 ? 0 : Math.max(2, diff));
        }
        
        this._histogramDiv.empty().show();
        $('<img />').attr("src", 
            "http://chart.apis.google.com/chart?" + [
                "chs=" + this._histogramDiv[0].offsetWidth + "x50",
                //"cht=ls&chm=o,ff6600,0,-1,3&chls=0", // use for line plot
                "cht=bvs&chbh=r,0&chco=000088,aaaaff", // use for histogram
                "chd=t:" + values.join(",") + "|" + diffs.join(",")
            ].join("&")
        ).appendTo(this._histogramDiv);
    }
    
    this._setRangeIndicators();
};

RangeFacet.prototype._reset = function() {
    this._setDefaults();
    this._updateRest();
};

RangeFacet.prototype._remove = function() {
    ui.browsingEngine.removeFacet(this);
    
    this._div = null;
    this._config = null;
    this._data = null;
};

RangeFacet.prototype._updateRest = function() {
    Gridworks.update({ engineChanged: true });
};
