var DOM = {};

DOM.bind = function(elmt) {
    var map = {};
    
    for (var i = 0; i < elmt.length; i++) {
        DOM._bindDOMElement(elmt[i], map);
    }
    
    return map;
};

DOM._bindDOMElement = function(elmt, map) {
    var bind = elmt.getAttribute("bind");
    if (bind !== null && bind.length > 0) {
        map[bind] = $(elmt);
    }
    
    if (elmt.hasChildNodes()) {
        DOM._bindDOMChildren(elmt, map);
    }
};

DOM._bindDOMChildren = function(elmt, map) {
    var node = elmt.firstChild;
    while (node !== null) {
        var node2 = node.nextSibling;
        if (node.nodeType == 1) {
            DOM._bindDOMElement(node, map);
        }
        node = node2;
    }
};

DOM._loadedHTML = {};
DOM.loadHTML = function(module, path) {
    var fullPath = ModuleWirings[module] + path;
    if (!(fullPath in DOM._loadedHTML)) {
        $.ajax({
            async: false,
            url: fullPath,
            dataType: "html",
            success: function(html) {
                DOM._loadedHTML[fullPath] = html;
            }
        })
    }
    return DOM._loadedHTML[fullPath];
};
