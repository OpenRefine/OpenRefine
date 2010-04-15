function ScatterplotWidget(elmt, options) {
    this._elmt = elmt;
    this._options = options;
    
    this._plotter = { 
        'cx' : options.x_columnName, 
        'cy' : options.y_columnName,
        'xe' : options.x_expression,
        'ye' : options.y_expression,
        'dot': options.dot,
        'dim': options.dim
    };
    
    this._range = null;
    this._highlight = null;
    
    this._initializeUI();
}

ScatterplotWidget.prototype.highlight = function(from_x, to_x, from_y, to_y) {
    this._highlight = { from_x: from_x, to_x: to_x, from_y: from_y, to_y: to_y };
    this._update();
};

ScatterplotWidget.prototype.update = function(x_min, x_max, x_from, x_to, y_min, y_max, y_from, y_to) {
    if (typeof x_min == "undefined" || typeof y_min == "undefined") {
        this._range = null;
        this._highlight = null;

        this._elmt.hide();
    } else {
        this._range = { x_min: x_min, x_max: x_max, y_min: y_min, y_max: y_max };
                
        if (typeof from_x != "undefined" && typeof to_x != "undefined" && 
            typeof from_y != "undefined" && typeof to_y != "undefined") 
        {
            this._highlight = { from_x: from_x, to_x: to_x, from_y: from_y, to_y: to_y };
        }
        
        this._update();
    }
};

ScatterplotWidget.prototype._update = function() {
    if (this._highlight !== null) {
        this._highlight.from_x = Math.max(this._highlight.from_x, this._range.x_min);
        this._highlight.to_x = Math.min(this._highlight.to_x, this._range.max_x);
        this._highlight.from_y = Math.max(this._highlight.from_y, this._range.y_min);
        this._highlight.to_y = Math.min(this._highlight.to_y, this._range.max_y);
    }
    
    this._elmt.show();
    this._resize();
    this._render();
};

ScatterplotWidget.prototype._initializeUI = function() {
    self = this;
    
    this._elmt
        .empty()
        .hide()
        .addClass("scatterplot-widget")
        .html('<canvas bind="canvas"></canvas>');
        
    this._elmts = DOM.bind(this._elmt);
    this._elmts.canvas.imgAreaSelect({ 
        handles: false,
        fadeSpeed: 70,
        onSelectEnd: function(elmt, selection) {
            self.highlight(
                selection.x1,
                selection.x2,
                self._plotter.h - selection.y2,
                self._plotter.h - selection.y1
            );
        }
    });
};

ScatterplotWidget.prototype._resize = function() {
    this._plotter.w = this._elmts.canvas.width();
    this._plotter.h = ("height" in this._options) ? this._options.height : this._plotter.w;
    this._elmts.canvas.attr("width", this._plotter.w);
    this._elmts.canvas.attr("height", this._plotter.h);
};

ScatterplotWidget.prototype._render = function() {
    var self = this;
    var options = this._options;
    
    var canvas = this._elmts.canvas[0];
    var ctx = canvas.getContext('2d');
    ctx.fillStyle = "white";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.save();
    
    var img = new Image();  
    img.onload = function(){
        ctx.drawImage(img,0,0); 
        if (self._highlight != null) {
            var img2 = new Image();  
            img2.onload = function(){  
                ctx.drawImage(img2,0,0);
                ctx.restore();
            }  
            self._plotter.color = "000088";
            console.log(self._plotter);
            img2.src = self._get_image_url(self._plotter);
        }
    }  
    self._plotter.color = "000000";
    console.log(self._plotter);
    img.src = self._get_image_url(self._plotter);
};

ScatterplotWidget.prototype._get_image_url = function(o) {
    var params = {
        project: theProject.id,
        engine: JSON.stringify(ui.browsingEngine.getJSON()), 
        plotter: JSON.stringify(o) 
    }                
    return "/command/get-scatterplot?" + $.param(params);
};
