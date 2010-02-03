function RangeFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._setDefaults();
    this._timerID = null;
    
    this._initializeUI();
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

RangeFacet.prototype.getJSON = function() {
    var o = cloneDeep(this._config);
    o.type = "range";
    
    switch (this._config.mode) {
    case "min":
        o.from = this._from;
        break;
    case "max":
        o.to = this._to;
        break;
    default:
        o.from = this._from;
        if (this._to == this._config.max) {
            // pretend to be open-ended
            o.mode = "min";
        } else {
            o.to = this._to;
        }
    }
    
    return o;
};

RangeFacet.prototype.hasSelection = function() {
    switch (this._config.mode) {
    case "min":
        return this._from > this._config.min;
    case "max":
        return this._to < this._config.max;
    default:
        return this._from > this._config.min ||
            this._to < this._config.max;
    }
};

RangeFacet.prototype._initializeUI = function() {
    var self = this;
    var container = this._div.empty();
    
    var headerDiv = $('<div></div>').addClass("facet-title").appendTo(container);
    $('<span></span>').text(this._config.name).appendTo(headerDiv);
    
    var removeButton = $('<a href="javascript:{}"></a>').addClass("facet-choice-link").text("remove").click(function() {
        self._remove();
    }).prependTo(headerDiv);
    
    var bodyDiv = $('<div></div>').addClass("facet-range-body").appendTo(container);
    
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
        self._scheduleUpdate();
    };
    var sliderConfig = {
        range: "max",
        min: this._config.min,
        max: this._config.max,
        value: 2,
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
    this._config.min = data.min;
    this._config.max = data.max;
    this._config.step = data.step;
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
    
    this.render();
};

RangeFacet.prototype.render = function() {
    this._sliderDiv.slider("option", "min", this._config.min);
    this._sliderDiv.slider("option", "max", this._config.max);
    this._sliderDiv.slider("option", "step", this._config.step);
    
    var max = 0;
    for (var i = 0; i < this._bins.length; i++) {
        max = Math.max(max, this._bins[i]);
    }
    if (max == 0) {
        this._histogramDiv.hide();
    } else {
        var a = [];
        for (var i = 0; i < this._bins.length; i++) {
            a.push(Math.round(100 * this._bins[i] / max));
        }
        
        this._histogramDiv.empty().show();
        $('<img />').attr("src", 
            "http://chart.apis.google.com/chart?" + [
                "cht=ls",
                "chs=" + this._histogramDiv[0].offsetWidth + "x50",
                "chm=o,ff6600,0,-1,3",
                "chls=0",
                "chd=t:" + a.join(",")
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

RangeFacet.prototype._scheduleUpdate = function() {
    if (this._timerID == null) {
        var self = this;
        this._timerID = window.setTimeout(function() {
            self._timerID = null;
            self._updateRest();
        }, 300);
    }
};

RangeFacet.prototype._updateRest = function() {
    ui.browsingEngine.update();
    ui.dataTableView.update(true);
};
