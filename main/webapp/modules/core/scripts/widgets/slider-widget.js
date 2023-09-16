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

function SliderWidget(elmt, options) {
  this._elmt = elmt;
  this._options = options || {};

  this._range = {
    min: 0,
    max: 1,
    step: 1,
    from: 0,
    to: 0
  };
  this._drag = null;

  var self = this;
  this._mouseMoveHandler = function(evt) {
    return self._onMouseMove(evt);
  };
  this._mouseUpHandler = function(evt) {
    return self._onMouseUp(evt);
  };

  this._initializeUI();
  this._update();
}

SliderWidget.prototype.update = function(min, max, step, from, to) {
  if (step <= 0) {
    step = 1;
  }
  max = Math.max(max, min + step);
  from = Math.max(min, from);
  to = Math.min(max, to);

  this._range = {
    min: min,
    max: max,
    step: step,
    from: from,
    to: to
  };
  this._update();
};

SliderWidget.prototype._initializeUI = function() {
  this._elmt.addClass("slider-widget");

  this._leftTintedRect = $("<div>").addClass("slider-widget-tint left").appendTo(this._elmt);
  this._rightTintedRect = $("<div>").addClass("slider-widget-tint right").appendTo(this._elmt);
  this._highlightRect = $("<div>").addClass("slider-widget-highlight slider-widget-draggable").attr("part", "highlight").appendTo(this._elmt);
  this._leftBracket = $("<div>").addClass("slider-widget-bracket slider-widget-draggable left").attr("part", "left").appendTo(this._elmt);
  this._rightBracket = $("<div>").addClass("slider-widget-bracket slider-widget-draggable right").attr("part", "right").appendTo(this._elmt);

  var self = this;
  this._elmt.find(".slider-widget-draggable")
  .on('mousedown',function(evt) {
    return self._onMouseDown(evt, this.getAttribute("part"));
  });

  this._highlightRect.on('dblclick',function(evt) {
    if (self._range.from > self._range.min || self._range.to < self._range.max) {
      self._range.from = self._range.min;
      self._range.to = self._range.max;
      self._update();
      self._trigger("stop");
    }
  });

  this._elmt
  .on('mousemove',function(evt) {
    return self._onMouseMove(evt);
  })
  .on('mouseup',function(evt) {
    return self._onMouseUp(evt);
  });
};

SliderWidget.prototype._onMouseDown = function(evt, part) {
  if (this._drag) {
    return;
  }

  $(document).on('mousemove',this._mouseMoveHandler);
  $(document).on('mouseup',this._mouseUpHandler);

  this._drag = {
    sureDrag: false,
    overlay: $('<div>').addClass("slider-widget-overlay").appendTo(document.body)
  };
  if ("highlight" == part) {
    this._drag.elmt = this._highlightRect;
    this._drag.value = this._range.from;
    $(this._drag.overlay).css("cursor", "move");
  } else if ("left" == part) {
    this._drag.elmt = this._leftBracket;
    $(this._drag.overlay).css("cursor", "e-resize");
  } else if ("right" == part) {
    this._drag.elmt = this._rightBracket;
    $(this._drag.overlay).css("cursor", "w-resize");
  }
  this._drag.what = part;
  this._drag.from = this._range.from;
  this._drag.to = this._range.to;
  this._drag.down = {
    x: evt.pageX,
    y: evt.pageY
  };
};

SliderWidget.prototype._onMouseUp = function(evt) {
  if (!(this._drag)) {
    return;
  }

  $(document).off("mousemove", this._mouseMoveHandler);
  $(document).off("mouseup", this._mouseUpHandler);

  if (this._drag.sureDrag) {
    this._update();
    this._trigger("stop");
  }
  this._drag.overlay.remove();
  this._drag = null;
};

SliderWidget.prototype._trigger = function(eventName) {
  this._elmt.trigger(eventName, [{ from: this._range.from, to: this._range.to }]);
};

SliderWidget.prototype._onMouseMove = function(evt) {
  if (!(this._drag)) {
    return;
  }

  var drag = this._drag;
  var range = this._range;

  var offset = this._elmt.offset();
  var xDiff = evt.pageX - drag.down.x;
  var yDiff = evt.pageX - drag.down.y;

  if (Math.abs(xDiff) >= 2) {
    drag.sureDrag = true;
  }

  var pixelWidth = this._elmt.width();
  var scale = pixelWidth / (range.max - range.min);
  var vDiff = xDiff / scale;

  var adjustFrom = function() {
    range.from = drag.from + Math.floor(vDiff / range.step) * range.step;
    range.from = Math.max(Math.min(range.from, range.max), range.min);
  };
  var adjustTo = function() {
    range.to = drag.to + Math.floor(vDiff / range.step) * range.step;
    range.to = Math.max(Math.min(range.to, range.max), range.min);
  };

  if (drag.what == "left") {
    adjustFrom();
    range.to = Math.min(Math.max(range.to, range.from + range.step), range.max);
  } else if (drag.what == "right") {
    adjustTo();
    range.from = Math.max(Math.min(range.from, range.to - range.step), range.min);
  } else {
    adjustFrom();
    adjustTo();
  }

  this._update();
  this._trigger("slide");

  evt.preventDefault();
  return false;
};

SliderWidget.prototype._update = function() {
  var range = this._range;

  var pixelWidth = this._elmt.width();
  var scale = pixelWidth / (range.max - range.min);
  var valueToPixel = function(x) {
    return (x - range.min) * scale;
  };

  var fromPixel = Math.floor(valueToPixel(range.from));
  var toPixel = Math.floor(valueToPixel(range.to));

  if (range.from == range.min && range.to == range.max) {
    this._leftTintedRect.hide();
    this._rightTintedRect.hide();
  } else {
    this._leftTintedRect.show().width(fromPixel);
    this._rightTintedRect.show().width(pixelWidth - toPixel);
  }

  this._highlightRect.css("left", (fromPixel - 1) + "px").width(toPixel - fromPixel);
  this._leftBracket.css("left", fromPixel + "px");
  this._rightBracket.css("left", toPixel + "px");
};
