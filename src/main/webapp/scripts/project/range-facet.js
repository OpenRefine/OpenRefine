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
        this._min = this._config.min;
        break;
    case "max":
        this._max = this._config.max;
        break;
    default:
        this._min = this._config.min;
        this._max = this._config.max;
    }
};

RangeFacet.prototype.getJSON = function() {
    var o = cloneDeep(this._config);
    o.type = "range";
    
    switch (this._config.mode) {
    case "min":
        o.min = this._min;
        break;
    case "max":
        o.max = this._max;
        break;
    default:
        o.min = this._min;
        if (this._max == this._config.max) {
            // pretend to be open-ended
            o.mode = "min";
        } else {
            o.max = this._max;
        }
    }
    
    return o;
};

RangeFacet.prototype.hasSelection = function() {
    switch (this._config.mode) {
    case "min":
        return this._min > this._config.min;
    case "max":
        return this._max < this._config.max;
    default:
        return this._min > this._config.min ||
            this._max < this._config.max;
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

    this._sliderDiv = $('<div></div>').addClass("facet-range-slider").appendTo(bodyDiv);
    this._statusDiv = $('<div></div>').addClass("facet-range-status").appendTo(bodyDiv);
    
    var onSlide = function(event, ui) {
        switch (self._config.mode) {
        case "min":
            self._min = ui.value;
            break;
        case "max":
            self._max = ui.value;
            break;
        default:
            self._min = ui.values[0];
            self._max = ui.values[1];
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
        sliderConfig.value = this._min;
        break;
    case "max":
        sliderConfig.range = "min";
        sliderConfig.value = this._max;
        break;
    default:
        sliderConfig.range = true;
        sliderConfig.values = [ this._min, this._max ];
    }
    this._sliderDiv.slider(sliderConfig);
    this._setRangeIndicators();
};

RangeFacet.prototype._setRangeIndicators = function() {
    var text;
    switch (this._config.mode) {
    case "min":
        text = "At least " + this._min;
        break;
    case "max":
        text = "At most " + this._max;
        break;
    default:
        text = this._min + " to " + this._max;
    }
    this._statusDiv.text(text);
};

RangeFacet.prototype.updateState = function(data) {
    switch (this._config.mode) {
    case "min":
        this._min = data.min;
        break;
    case "max":
        this._max = data.max;
        break;
    default:
        this._min = data.min;
        if ("max" in data) {
            this._max = data.max;
        }
    }
    
    this.render();
};

RangeFacet.prototype.render = function() {
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
