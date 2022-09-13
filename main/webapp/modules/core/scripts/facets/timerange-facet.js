/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

class TimeRangeFacet extends Facet{
  constructor(div, config, options) {
    super(div, config, options);

    this._from = ("from" in this._config) ? this._config.from : null;
    this._to = ("to" in this._config) ? this._config.to : null;
    this._step = ("step" in this._config) ? this._config.step : null;

    this._selectTime = ("selectTime" in this._config) ? this._config.selectTime : true;
    this._selectNonTime = ("selectNonTime" in this._config) ? this._config.selectNonTime : true;
    this._selectBlank = ("selectBlank" in this._config) ? this._config.selectBlank : true;
    this._selectError = ("selectError" in this._config) ? this._config.selectError : true;

    this._baseTimeCount = 0;
    this._baseNonTimeCount = 0;
    this._baseBlankCount = 0;
    this._baseErrorCount = 0;

    this._timeCount = 0;
    this._nonTimeCount = 0;
    this._blankCount = 0;
    this._errorCount = 0;

    this._error = false;
    this._initializedUI = false;
  };

  reset() {
    this._from = this._config.min;
    this._to = this._config.max;
    this._sliderWidget.update(
        this._config.min, 
        this._config.max, 
        this._config.step, 
        this._from,
        this._to
    );

    this._selectTime = true;
    this._selectNonTime = true;
    this._selectBlank = true;
    this._selectError = true;

    this._setRangeIndicators();
  };

  getUIState() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };

    return json;
  };

  getJSON() {
    var o = {
        type: "timerange",
        name: this._config.name,
        expression: this._config.expression,
        columnName: this._config.columnName,
        selectTime: this._selectTime,
        selectNonTime: this._selectNonTime,
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

  hasSelection() {
    if (!this._selectTime || !this._selectNonTime || !this._selectBlank || !this._selectError) {
      return true;
    }

    return (this._from !== null && (!this._initializedUI || this._from > this._config.min)) ||
    (this._to !== null && (!this._initializedUI || this._to < this._config.max));
  };

  _initializeUI() {
    var self = this;
    this._div
    .empty()
    .show()
    .html(
      '<div class="facet-title" bind="headerDiv">' +
        '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/remove-facet')+'" class="facet-title-remove" bind="removeButton">&nbsp;</a>' +
          '</td>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/minimize-facet')+'" class="facet-title-minimize" bind="minimizeButton">&nbsp;</a>' +
          '</td>' +
          '<td>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">'+$.i18n('core-facets/reset')+'</a>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="changeButton">'+$.i18n('core-facets/change')+'</a>' +
            '<span bind="facetTitle"></span>' +
          '</td>' +
        '</tr></table></div>' +
      '</div>' +
      '<div class="facet-expression" bind="expressionDiv" title="'+$.i18n('core-facets/click-to-edit')+'"></div>' +
      '<div class="facet-range-body">' +
        '<div class="facet-range-message" bind="messageDiv">'+$.i18n('core-facets/loading')+'</div>' +
        '<div class="facet-range-slider" bind="sliderWidgetDiv">' +
          '<div class="facet-range-histogram" bind="histogramDiv"></div>' +
        '</div>' +
        '<div class="facet-range-status" bind="statusDiv"></div>' +
        '<div class="facet-range-other-choices" bind="otherChoicesDiv"></div>' +
      '</div>'
    );
    this._elmts = DOM.bind(this._div);

    this._elmts.facetTitle.text(this._config.name);
    this._elmts.changeButton.attr("title",$.i18n('core-facets/current-exp')+": " + this._config.expression).on('click',function() {
      self._elmts.expressionDiv.slideToggle(100, function() {
        if (self._elmts.expressionDiv.css("display") != "none") {
          self._editExpression();
        }
      });
    });
    this._elmts.expressionDiv.text(this._config.expression).on('click',function() { 
      self._editExpression(); 
    }).hide();

    this._elmts.resetButton.on('click',function() {
      self.reset();
      self._updateRest();
    });
    
    this._elmts.removeButton.on('click',function() { self._remove(); });
    this._elmts.minimizeButton.on('click',function() { self._minimize(); });

    this._histogram = new HistogramWidget(this._elmts.histogramDiv, { binColors: [ "#668CFF", "#174092" ] });
    this._sliderWidget = new SliderWidget(this._elmts.sliderWidgetDiv);

    this._elmts.sliderWidgetDiv.on("slide", function(evt, data) {
      self._from = data.from;
      self._to = data.to;
      self._setRangeIndicators();
    }).on("stop", function(evt, data) {
      self._from = data.from;
      self._to = data.to;
      self._selectTime = true;
      self._updateRest();
    });
  };

  _renderOtherChoices() {
    var self = this;
    var container = this._elmts.otherChoicesDiv.empty();

    if (this._baseNonTimeCount === 0 && this._baseBlankCount === 0 && this._baseErrorCount === 0) {
      return;
    }

    var facet_id = this._div.attr("id");

    var choices = $('<div>').addClass("facet-range-choices");

    // ----------------- time -----------------

    var timeDiv = $('<div class="facet-range-item"></div>').appendTo(choices);            
    var timeCheck = $('<input type="checkbox" />').attr("id",facet_id + "-time").appendTo(timeDiv).on('change',function() {
      self._selectTime = !self._selectTime;
      self._updateRest();
    });
    if (this._selectTime) timeCheck.prop('checked', true);

    var timeLabel = $('<label>').attr("for", facet_id + "-time").appendTo(timeDiv);    
    $('<span>').text($.i18n('core-facets/time')+" ").addClass("facet-range-choice-label").appendTo(timeLabel);
    $('<div>').text(this._timeCount).addClass("facet-range-choice-count").appendTo(timeLabel);

    // ----------------- non-Time -----------------

    var nonTimeDiv = $('<div class="facet-range-item"></div>').appendTo(choices);            
    var nonTimeCheck = $('<input type="checkbox" />').attr("id",facet_id + "-non-time").appendTo(nonTimeDiv).on('change',function() {
      self._selectNonTime = !self._selectNonTime;
      self._updateRest();
    });
    if (this._selectNonTime) nonTimeCheck.prop('checked', true);

    var nonTimeLabel = $('<label>').attr("for", facet_id + "-non-time").appendTo(nonTimeDiv);    
    $('<span>').text($.i18n('core-facets/non-time')+" ").addClass("facet-range-choice-label").appendTo(nonTimeLabel);
    $('<div>').text(this._nonTimeCount).addClass("facet-range-choice-count").appendTo(nonTimeLabel);

    if (this._baseNonTimeCount === 0) nonTimeCheck.prop("checked", false);

    // ----------------- blank -----------------

    var blankDiv = $('<div class="facet-range-item"></div>').appendTo(choices);            
    var blankCheck = $('<input type="checkbox" />').attr("id",facet_id + "-blank").appendTo(blankDiv).on('change',function() {
      self._selectBlank = !self._selectBlank;
      self._updateRest();
    });
    if (this._selectBlank) blankCheck.prop('checked', true);

    var blankLabel = $('<label>').attr("for", facet_id + "-blank").appendTo(blankDiv);    
    $('<span>').text($.i18n('core-facets/blank')+" ").addClass("facet-range-choice-label").appendTo(blankLabel);
    $('<div>').text(this._blankCount).addClass("facet-range-choice-count").appendTo(blankLabel);

    if (this._baseBlankCount === 0) blankCheck.prop("checked", false);

    // ----------------- error -----------------

    var errorDiv = $('<div class="facet-range-item"></div>').appendTo(choices);            
    var errorCheck = $('<input type="checkbox" />').attr("id",facet_id + "-error").appendTo(errorDiv).on('change',function() {
      self._selectError = !self._selectError;
      self._updateRest();
    });
    if (this._selectError) errorCheck.prop('checked', true);

    var errorLabel = $('<label>').attr("for", facet_id + "-error").appendTo(errorDiv);    
    $('<span>').text($.i18n('core-facets/error')+" ").addClass("facet-range-choice-label").appendTo(errorLabel);
    $('<div>').text(this._errorCount).addClass("facet-range-choice-count").appendTo(errorLabel);

    if (this._baseErrorCount === 0) errorCheck.prop("checked", false);

    // --------------------------

    choices.appendTo(container);
  };

  _setRangeIndicators() {
    var fromDate = new Date(this._from);
    var toDate = new Date(this._to);

    if (this._step > 2629746000) { // > month
      var format = "yyyy";
      this._elmts.statusDiv.html(fromDate.toString(format) + " &mdash; " + toDate.toString(format));
    } else if (this.step > 3600000) { // > hour
      var format = "yyyy-MM-dd";
      this._elmts.statusDiv.html(fromDate.toString(format) + " &mdash; " + toDate.toString(format));
    } else {
      var timeOfDayformat = "HH:mm:ss";
      var dayOfYearFormat = "yyyy-MM-dd";
      this._elmts.statusDiv.html("<b>" + fromDate.toString(dayOfYearFormat) + "</b>  " + fromDate.toString(timeOfDayformat) + " &mdash; "  + "<b>" + toDate.toString(dayOfYearFormat) + "</b>  " + toDate.toString(timeOfDayformat) );
    }
  };

  updateState(data) {
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

      this._baseTimeCount = data.baseTimeCount;
      this._baseNonTimeCount = data.baseNonTimeCount;
      this._baseBlankCount = data.baseBlankCount;
      this._baseErrorCount = data.baseErrorCount;

      this._timeCount = data.timeCount;
      this._nonTimeCount = data.nonTimeCount;
      this._blankCount = data.blankCount;
      this._errorCount = data.errorCount;
    } else {
      this._error = true;
      this._errorMessage = "error" in data ? data.error : $.i18n('core-facets/unknown-error')+".";
    }

    this.render();
  };

  render() {
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

  _updateRest() {
    Refine.update({ engineChanged: true });
  };

  _editExpression() {
    var self = this;
    var title = (this._config.columnName) ? 
        ($.i18n('core-facets/edit-based-col')+" " + this._config.columnName) : 
          $.i18n('core-facets/edit-facet-exp');

        var column = Refine.columnNameToColumn(this._config.columnName);
        var o = DataTableView.sampleVisibleRows(column);

        new ExpressionPreviewDialog(
            title,
            column ? column.cellIndex : -1, 
                o.rowIndices,
                o.values,
                this._config.expression, 
                function(expr) {
              if (expr != self._config.expression) {
                self._config.expression = expr;
                self._elmts.expressionDiv.text(self._config.expression);

                self.reset();
                self._from = null;
                self._to = null;
                self._updateRest();
              }
            }
        );
  };
};

TimeRangeFacet.reconstruct = function(div, uiState) {
  return new TimeRangeFacet(div, uiState.c, uiState.o);
};

TimeRangeFacet.prototype.steps = [ 
                                  1,                  // msec
                                  1000,               // sec
                                  1000*60,            // min
                                  1000*60*60,         // hour
                                  1000*60*60*24,      // day
                                  1000*60*60*24*7,    // week
                                  1000*2629746,       // month (average Gregorian year / 12)
                                  1000*31556952,      // year (average Gregorian year)
                                  1000*31556952*10,   // decade 
                                  1000*31556952*100,  // century 
                                  1000*31556952*1000  // millennium 
                                  ];
