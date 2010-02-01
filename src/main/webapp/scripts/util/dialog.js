DialogSystem = {
    _layers: [],
    _bottomOverlay: null
};

DialogSystem.showDialog = function(elmt, onCancel) {
    if (DialogSystem._bottomOverlay == null) {
        DialogSystem._bottomOverlay = $('<div>&nbsp;</div>')
            .addClass("dialog-overlay")
            .css("z-index", 100)
            .appendTo(document.body);
    }
    
    var overlay = $('<div>&nbsp;</div>')
        .addClass("dialog-overlay2")
        .css("z-index", 101 + DialogSystem._layers.length * 2)
        .appendTo(document.body);
        
    var container = $('<div></div>')
        .addClass("dialog-container")
        .css("z-index", 102 + DialogSystem._layers.length * 2)
        .appendTo(document.body);
        
    elmt.css("visibility", "hidden").appendTo(container);
    container.css("top", Math.round((overlay.height() - elmt.height()) / 2) + "px");
    elmt.css("visibility", "visible");
    
    var layer = {
        overlay: overlay,
        container: container,
        onCancel: onCancel
    };
    DialogSystem._layers.push(layer);
    
    var level = DialogSystem._layers.length;
    return level;
};

DialogSystem.dismissAll = function() {
    DialogSystem.dismissUntil(0);
};

DialogSystem.dismissUntil = function(level) {
    for (var i = DialogSystem._layers.length - 1; i >= level; i--) {
        var layer = DialogSystem._layers[i];
        layer.overlay.remove();
        layer.container.remove();
        
        if (layer.onCancel) {
            try {
                layer.onCancel();
            } catch (e) {
                console.log(e);
            }
        }
    }
    DialogSystem._layers = DialogSystem._layers.slice(0, level);
    
    if (level == 0) {
        if (DialogSystem._bottomOverlay != null) {
            DialogSystem._bottomOverlay.remove();
            DialogSystem._bottomOverlay = null;
        }
    }
};

DialogSystem.createDialog = function() {
    return $('<div></div>').addClass("dialog-frame");
};

