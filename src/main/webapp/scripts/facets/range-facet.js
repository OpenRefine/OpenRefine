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
    
    this._baseNumericCount = 0;
    this._baseNonNumericCount = 0;
    this._baseBlankCount = 0;
    this._baseErrorCount = 0;
    
    this._numericCount = 0;
    this._nonNumericCount = 0;
    this._blankCount = 0;
    this._errorCount = 0;
    
    this._error = false;
    this._initializedUI = false;
}

RangeFacet.prototype.reset = function() {
    this._from = this._config.min;
    this._to = this._config.max;
    this._sliderWidget.update(
        this._config.min, 
        this._config.max, 
        this._config.step, 
        this._from,
        this._to
    );
        
    this._selectNumeric = true;
    this._selectNonNumeric = true;
    this._selectBlank = true;
    this._selectError = true;
    
    this._setRangeIndicators();
};

RangeFacet.reconstruct = function(div, uiState) {
    return new RangeFacet(div, uiState.c, uiState.o);
};

RangeFacet.prototype.dispose = function() {
};

RangeFacet.prototype.getUIState = function() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };
    
    return json;
};

RangeFacet.prototype.getJSON = function() {
    var o = {
        type: "range",
        name: this._config.name,
        expression: this._config.expression,
        columnName: this._config.columnName,
        selectNumeric: this._selectNumeric,
        selectNonNumeric: this._selectNonNumeric,
        selectBlank: this._selectBlank,
        selectError: this._selectError
    };
    
    if (this._from !== null) {
        o.from = this._from;
    }
    if (this._to !== null) {
        o.to = this._to;
    }
    
    return o;
};

RangeFacet.prototype.hasSelection = function() {
    if (!this._selectNumeric || !this._selectNonNumeric || !this._selectBlank || !this._selectError) {
        return true;
    }
    
    return (this._from !== null && (!this._initializedUI || this._from > this._config.min)) ||
        (this._to !== null && (!this._initializedUI || this._to < this._config.max));
};

RangeFacet.prototype._initializeUI = function() {
    var self = this;
    this._div
        .empty()
        .show()
        .html(
            '<div class="facet-title" bind="headerDiv">' +
                '<img src="images/close.png" class="facet-choice-link" title="Remove this facet" bind="removeButton" />' +
                '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">reset</a>' +
                '<span bind="facetTitle"></span>' +
            '</div>' +
            '<div class="facet-expression" bind="expressionDiv"></div>' +
            '<div class="facet-range-body">' +
                '<div class="facet-range-message" bind="messageDiv">Loading...</div>' +
                '<div class="facet-range-slider" bind="sliderWidgetDiv">' +
                    '<div class="facet-range-histogram" bind="histogramDiv"></div>' +
                '</div>' +
                '<div class="facet-range-status" bind="statusDiv"></div>' +
                '<div class="facet-range-other-choices" bind="otherChoicesDiv"></div>' +
            '</div>'
        );
    this._elmts = DOM.bind(this._div);
    
    this._elmts.facetTitle.text(this._config.name);
    this._elmts.expressionDiv.text(this._config.expression);
    
    this._elmts.resetButton.click(function() {
        self.reset();
        self._updateRest();
    });
    this._elmts.removeButton.click(function() {
            self._remove();
        });
        
    this._histogram = new HistogramWidget(this._elmts.histogramDiv, { binColors: [ "#ccccff", "#6666ff" ] });
    this._sliderWidget = new SliderWidget(this._elmts.sliderWidgetDiv);
    
    this._elmts.sliderWidgetDiv.bind("slide", function(evt, data) {
        self._from = data.from;
        self._to = data.to;
        self._setRangeIndicators();
    }).bind("stop", function(evt, data) {
        self._from = data.from;
        self._to = data.to;
        self._selectNumeric = true;
        self._updateRest();
    });
};

RangeFacet.prototype._renderOtherChoices = function() {
    var self = this;
    var container = this._elmts.otherChoicesDiv.empty();
    
    if (this._baseNonNumericCount === 0 && this._baseBlankCount === 0 && this._baseErrorCount === 0) {
        return;
    }
    
    var table = $('<table>').attr("cellpadding", "0").attr("cellspacing", "1").css("white-space", "pre").appendTo(container)[0];
    var tr0 = table.insertRow(0);
    var tr1 = table.insertRow(1);
    
    /*
     *  Numeric
     */
    var td00 = $(tr0.insertCell(0)).attr("width", "1%");
    var td01 = $(tr0.insertCell(1));
    
    var numericCheck = $('<input type="checkbox" />').appendTo(td00).change(function() {
        self._selectNumeric = !self._selectNumeric;
        self._updateRest();
    });
    if (this._selectNumeric) {
        numericCheck[0].checked = true;
    }
    
    $('<span>').text("Numeric ").addClass("facet-choice-label").appendTo(td01);
    $('<span>').text(this._numericCount).addClass("facet-choice-count").appendTo(td01);
    
    /*
     *  Blank
     */
    var td02 = $(tr0.insertCell(2)).attr("width", "1%");
    var td03 = $(tr0.insertCell(3));
    if (this._baseBlankCount === 0) {
        td02.hide();
        td03.hide();
    } else {
        var blankCheck = $('<input type="checkbox" />').appendTo(td02).change(function() {
            self._selectBlank = !self._selectBlank;
            self._updateRest();
        });
        if (this._selectBlank) {
            blankCheck[0].checked = true;
        }
    
        $('<span>').text("Blank ").addClass("facet-choice-label").appendTo(td03);
        $('<span>').text(this._blankCount).addClass("facet-choice-count").appendTo(td03);
    }
    
    /*
     *  Non-Numeric
     */
    var td10 = $(tr1.insertCell(0)).attr("width", "1%");
    var td11 = $(tr1.insertCell(1));
    if (this._baseNonNumericCount === 0) {
        td10.hide();
        td11.hide();
    } else {
        var nonNumericCheck = $('<input type="checkbox" />').appendTo(td10).change(function() {
            self._selectNonNumeric = !self._selectNonNumeric;
            self._updateRest();
        });
        if (this._selectNonNumeric) {
            nonNumericCheck[0].checked = true;
        }
    
        $('<span>').text("Non-numeric ").addClass("facet-choice-label").appendTo(td11);
        $('<span>').text(this._nonNumericCount).addClass("facet-choice-count").appendTo(td11);
    }
    
    /*
     *  Error
     */
    var td12 = $(tr1.insertCell(2)).attr("width", "1%");
    var td13 = $(tr1.insertCell(3));
    if (this._baseErrorCount === 0) {
        td12.hide();
        td13.hide();
    } else {
        var errorCheck = $('<input type="checkbox" />').appendTo(td12).change(function() {
            self._selectError = !self._selectError;
            self._updateRest();
        });
        if (this._selectError) {
            errorCheck[0].checked = true;
        }
    
        $('<span>').text("Error ").addClass("facet-choice-label").appendTo(td13);
        $('<span>').text(this._errorCount).addClass("facet-choice-count").appendTo(td13);
    }
};

RangeFacet.prototype._setRangeIndicators = function() {
    this._elmts.statusDiv.html(this._from + " &mdash; " + this._to);
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
        
        this._baseNumericCount = data.baseNumericCount;
        this._baseNonNumericCount = data.baseNonNumericCount;
        this._baseBlankCount = data.baseBlankCount;
        this._baseErrorCount = data.baseErrorCount;

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
        this._elmts.messageDiv.text(this._errorMessage).show();
        this._elmts.sliderWidgetDiv.hide();
        this._elmts.histogramDiv.hide();
        this._elmts.statusDiv.hide();
        this._elmts.otherChoicesDiv.hide();
        return;
    }
    
    this._elmts.messageDiv.hide();
    this._elmts.sliderWidgetDiv.show();
    this._elmts.histogramDiv.show();
    this._elmts.statusDiv.show();
    this._elmts.otherChoicesDiv.show();
    
    this._sliderWidget.update(
        this._config.min, 
        this._config.max, 
        this._config.step, 
        this._from,
        this._to
    );
    this._histogram.update(
        this._config.min, 
        this._config.max, 
        this._config.step, 
        [ this._baseBins, this._bins ]
    );
    
    this._setRangeIndicators();
    this._renderOtherChoices();
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
