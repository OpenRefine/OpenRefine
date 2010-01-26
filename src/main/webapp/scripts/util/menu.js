MenuSystem = {
    _layers: [],
    _overlay: null
};

MenuSystem.showMenu = function(elmt) {
    if (MenuSystem._overlay == null) {
        MenuSystem._overlay = $('<div>&nbsp;</div>')
            .addClass("menu-overlay")
            .css("z-index", 1000)
            .appendTo(document.body)
            .click(MenuSystem.dismissAll);
    }
    
    elmt.css("z-index", 1001 + MenuSystem._layers.length).appendTo(document.body);
    
    var layer = {
        elmt: elmt
    };
    MenuSystem._layers.push(layer);
    
    var level = MenuSystem._layers.length;
    
    elmt.mouseout(function() { MenuSystem._dismissUtil(level); });
    elmt.click(MenuSystem.dismissAll);
};

MenuSystem.dismissAll = function() {
    MenuSystem._dismissUtil(0);
    if (MenuSystem._overlay != null) {
        MenuSystem._overlay.remove();
        MenuSystem._overlay = null;
    }
};

MenuSystem._dismissUtil = function(level) {
    for (var i = MenuSystem._layers.length - 1; i >= level; i--) {
        MenuSystem._layers[i].elmt.remove();
    }
    MenuSystem._layers = MenuSystem._layers.slice(0, level);
};

MenuSystem.createMenu = function() {
    return $('<div></div>').addClass("menu-container");
};

MenuSystem.createMenuItem = function() {
    return $('<a href="javascript:{}"></a>').addClass("menu-item");
};

MenuSystem.positionMenuAboveBelow = function(menu, elmt) {
    var offset = elmt.offset();
    menu.css("left", offset.left + "px")
        .css("top", (offset.top + elmt.height()) + "px");
};

MenuSystem.positionMenuLeftRight = function(menu, elmt) {
    var offset = elmt.offset();
    menu.css("top", offset.top + "px")
        .css("left", (offset.left + elmt.width()) + "px");
};
