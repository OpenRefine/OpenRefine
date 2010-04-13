function HistogramWidget(elmt, options) {
    this._elmt = elmt;
    this._options = options;
    
    this._range = null;
    this._binMatrix = null;
    this._highlight = null;
    
    this._initializeUI();
}

HistogramWidget.prototype.highlight = function(from, to) {
    this._highlight = { from: from, to: to };
    this._update();
};

HistogramWidget.prototype.update = function(min, max, step, binMatrix, from, to) {
    if (typeof min == "undefined" || typeof binMatrix == "undefined" || binMatrix.length === 0 || binMatrix[0].length === 0) {
        this._range = null;
        this._binMatrix = null;
        this._highlight = null;
        
        this._elmt.hide();
    } else {
        this._range = { min: min, max: max, step: step };
        this._binMatrix = binMatrix;
        
        this._peak = 0;
        for (var r = 0; r < binMatrix.length; r++) {
            var row = binMatrix[r];
            for (var c = 0; c < row.length; c++) {
                this._peak = Math.max(this._peak, row[c]);
            }
        }
        
        if (typeof from != "undefined" && typeof to != "undefined") {
            this._highlight = { from: from, to: to };
        }
        
        this._update();
    }
};

HistogramWidget.prototype._update = function() {
    if (this._binMatrix !== null) {
        if (this._highlight !== null) {
            this._highlight.from = Math.max(this._highlight.from, this._range.min);
            this._highlight.to = Math.min(this._highlight.to, this._range.max);
        }
        
        this._elmt.show();
        this._resize();
        this._render();
    }
};

HistogramWidget.prototype._initializeUI = function() {
    this._elmt
        .empty()
        .hide()
        .addClass("histogram-widget")
        .html(
            '<canvas bind="canvas"></canvas>'
        );
        
    this._elmts = DOM.bind(this._elmt);
};

HistogramWidget.prototype._resize = function() {
    this._elmts.canvas.attr("height", "height" in this._options ? this._options.height : 50);
    this._elmts.canvas.attr("width", this._elmts.canvas.width());
};

HistogramWidget.prototype._render = function() {
    var self = this;
    var options = this._options;
    
    var canvas = this._elmts.canvas[0];
    var ctx = canvas.getContext('2d');
    ctx.fillStyle = "white";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    ctx.save();
    ctx.translate(0, canvas.height);
    ctx.scale(1, -1);
    
    var stepPixels = canvas.width / this._binMatrix[0].length;
    var stepScale = stepPixels / this._range.step;
    
    /*
     *  Draw axis
     */
    ctx.save();
    ctx.strokeStyle = "emptyBinColor" in options ? options.emptyBinColor : "#faa";
    ctx.lineWidth = 1;
    ctx.moveTo(0, 0);
    ctx.lineTo(canvas.width, 0);
    ctx.stroke();
    ctx.restore();

    /*
     *  Draw bins
     */
    var makeColor = function(i) {
        var n = Math.floor(15 * (self._binMatrix.length - i) / self._binMatrix.length);
        var h = n.toString(16);
        return "#" + h + h + h;
    };
    var renderRow = function(row, color) {
        ctx.save();
        ctx.lineWidth = 0;
        ctx.fillStyle = color;
        for (var c = 0; c < row.length; c++) {
            var x = self._range.min + c * self._range.step;
            var y = row[c];
            if (y > 0) {
                var left = c * stepPixels;
                var width = Math.ceil(stepPixels);
                var height = Math.ceil(y * canvas.height / self._peak);
                
                ctx.fillRect(left, 0, width, height);
            }
        }
        ctx.restore();
    };
    for (var r = 0; r < this._binMatrix.length; r++) {
        renderRow(
            this._binMatrix[r], 
            "binColors" in options && r < options.binColors.length ? 
                options.binColors[r] :
                makeColor(r)
        );
    }
    
    /*
     *  Draw highlight
     */
    if (this._highlight !== null) {
        ctx.fillStyle = "rgba(192,192,192, 0.5)";
        ctx.globalCompositeOperation = "source-over";
        if (this._highlight.from > this._range.min) {
            ctx.fillRect(
                0,
                0,
                (this._highlight.from - this._range.min) * stepScale,
                canvas.height
            );
        }
        if (this._highlight.to < this._range.max) {
            ctx.fillRect(
                (this._highlight.to - this._range.min) * stepScale,
                0,
                canvas.width - (this._highlight.to - this._range.min) * stepScale,
                canvas.height
            );
        }
    }
        
    ctx.restore();
};
