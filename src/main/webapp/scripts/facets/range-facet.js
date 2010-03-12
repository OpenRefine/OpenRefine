function RangeFacet(div, config, options) {
    this._div = div;
    this._config = config;
    this._options = options;
    
    this._from = ("from" in this._config) ? this._config.from : null;
    this._to = ("to" in this._config) ? this._config.to : null;
    
    this._selectNumeric = ("selectNumeric" in this._config) ? this._config.selectNumeric : true;
    this._selectNonNumeric = ("selectNonNumeric" in this._config) ? this._config.selectNonNumeric : true;
    this._selectBlank = ("selectBlank" in this._config) ? this._config.selectBlank : true;
    this._selectError = ("selectError" in this._config) ? this._config.selectError : true;
    
    this._numericCount = 0;
    this._nonNumericCount = 0;
    this._blankCount = 0;
    this._errorCount = 0;
    
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
    
    this._selectNumeric = true;
    this._selectNonNumeric = true;
    this._selectBlank = true;
    this._selectError = true;
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
        columnName: this._config.columnName,
        selectNumeric: this._selectNumeric,
        selectNonNumeric: this._selectNonNumeric,
        selectBlank: this._selectBlank,
        selectError: this._selectError
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
    if (!this._selectNumeric || !this._selectNonNumeric || !this._selectBlank || !this._selectError) {
        return true;
    }
    
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
        self._selectNumeric = true;
        self._selectNonNumeric = true;
        self._selectBlank = true;
        self._selectError = true;
        
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
    
    this._messageDiv = $('<div>').text("Loading...").addClass("facet-range-message").appendTo(bodyDiv);
    this._histogramDiv = $('<div>').addClass("facet-range-histogram").appendTo(bodyDiv);
    this._sliderDiv = $('<div>').addClass("facet-range-slider").appendTo(bodyDiv);
    this._statusDiv = $('<div>').addClass("facet-range-status").appendTo(bodyDiv);
    this._otherChoicesDiv = $('<div>').addClass("facet-range-other-choices").appendTo(bodyDiv);
    
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
        self._selectNumeric = true;
        self._updateRest();
    };
    var sliderConfig = {
        min: this._config.min,
        max: this._config.max,
        stop: onStop,
        slide: onSlide
    };
        
    switch (this._config.mode) {
    case "min":
        sliderConfig.range = "max";
        sliderConfig.value = this._config.min;
        break;
    case "max":
        sliderConfig.range = "min";
        sliderConfig.value = this._config.max;
        break;
    default:
        sliderConfig.range = true;
        sliderConfig.values = [ this._config.min, this._config.max ];
    }
    
    this._sliderDiv.slider(sliderConfig);
};

RangeFacet.prototype._renderOtherChoices = function() {
    var self = this;
    var container = this._otherChoicesDiv.empty();
    
    var table = $('<table>').attr("cellpadding", "0").attr("cellspacing", "1").css("white-space", "pre").appendTo(container)[0];
    var tr0 = table.insertRow(0);
    var tr1 = table.insertRow(1);
    
    var td00 = $(tr0.insertCell(0)).attr("width", "1%");
    var numericCheck = $('<input type="checkbox" />').appendTo(td00).change(function() {
        self._selectNumeric = !self._selectNumeric;
        self._updateRest();
    });
    if (this._selectNumeric) {
        numericCheck[0].checked = true;
    }
    
    var td01 = $(tr0.insertCell(1));
    $('<span>').text("Numeric ").addClass("facet-choice-label").appendTo(td01);
    $('<span>').text(this._numericCount).addClass("facet-choice-count").appendTo(td01);
    
    var td02 = $(tr0.insertCell(2)).attr("width", "1%");
    var nonNumericCheck = $('<input type="checkbox" />').appendTo(td02).change(function() {
        self._selectNonNumeric = !self._selectNonNumeric;
        self._updateRest();
    });
    if (this._selectNonNumeric) {
        nonNumericCheck[0].checked = true;
    }
    
    var td03 = $(tr0.insertCell(3));
    $('<span>').text("Non-numeric ").addClass("facet-choice-label").appendTo(td03);
    $('<span>').text(this._nonNumericCount).addClass("facet-choice-count").appendTo(td03);
    
    var td10 = $(tr1.insertCell(0)).attr("width", "1%");
    var blankCheck = $('<input type="checkbox" />').appendTo(td10).change(function() {
        self._selectBlank = !self._selectBlank;
        self._updateRest();
    });
    if (this._selectBlank) {
        blankCheck[0].checked = true;
    }
    
    var td11 = $(tr1.insertCell(1));
    $('<span>').text("Blank ").addClass("facet-choice-label").appendTo(td11);
    $('<span>').text(this._blankCount).addClass("facet-choice-count").appendTo(td11);
    
    var td12 = $(tr1.insertCell(2)).attr("width", "1%");
    var errorCheck = $('<input type="checkbox" />').appendTo(td12).change(function() {
        self._selectError = !self._selectError;
        self._updateRest();
    });
    if (this._selectError) {
        errorCheck[0].checked = true;
    }
    
    var td13 = $(tr1.insertCell(3));
    $('<span>').text("Error ").addClass("facet-choice-label").appendTo(td13);
    $('<span>').text(this._errorCount).addClass("facet-choice-count").appendTo(td13);
    
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

RangeFacet.prototype.render = function() {
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
    this._sliderDiv.show();
    this._histogramDiv.show();
    this._statusDiv.show();
    this._otherChoicesDiv.show();
    
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
    this._renderOtherChoices();
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
