MenuSystem = {
    _layers: [],
    _overlay: null
};

MenuSystem.showMenu = function(elmt, onDismiss) {
    if (MenuSystem._overlay == null) {
        MenuSystem._overlay = $('<div>&nbsp;</div>')
            .addClass("menu-overlay")
            .css("z-index", 1000)
            .appendTo(document.body)
            .click(MenuSystem.dismissAll);
    }
    
    elmt.css("z-index", 1001 + MenuSystem._layers.length).appendTo(document.body);
    
    var layer = {
        elmt: elmt,
        onDismiss: onDismiss
    };
    MenuSystem._layers.push(layer);
    
    var level = MenuSystem._layers.length;
    
    elmt.click(MenuSystem.dismissAll);
    
    return level;
};

MenuSystem.dismissAll = function() {
    MenuSystem.dismissUntil(0);
    if (MenuSystem._overlay != null) {
        MenuSystem._overlay.remove();
        MenuSystem._overlay = null;
    }
};

MenuSystem.dismissUntil = function(level) {
    for (var i = MenuSystem._layers.length - 1; i >= level; i--) {
        var layer = MenuSystem._layers[i];
        layer.elmt.remove();
        layer["onDismiss"]();
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
        .css("left", (offset.left + elmt.width() + 10) + "px");
};

MenuSystem.createAndShowStandardMenu = function(items, elmt, options) {
    options = options || {
        horizontal: false
    };
    
    var menu = MenuSystem.createMenu();
    if ("width" in options) {
        menu.width(options.width);
    }
    
    var createMenuItem = function(item) {
        if ("label" in item) {
            var menuItem = MenuSystem.createMenuItem().appendTo(menu);
            if ("submenu" in item) {
                menuItem.html(
                    '<table width="100%" cellspacing="0" cellpadding="0" class="menu-item-layout"><tr>' +
                        '<td>' + item.label + '</td>' +
                        '<td width="1%"><img src="/images/right-arrow.png" /></td>' +
                    '</tr></table>'
                );
                menuItem.mouseenter(function() {
                    MenuSystem.dismissUntil(level);
                    
                    menuItem.addClass("menu-expanded");
                    
                    MenuSystem.createAndShowStandardMenu(
                        item.submenu, 
                        this,
                        {
                            horizontal: true,
                            onDismiss: function() {
                                menuItem.removeClass("menu-expanded");
                            }
                        }
                    );
                });
            } else {
                menuItem.html(item.label).click(item.click);
                if ("tooltip" in item) {
                    menuItem.attr("title", item.tooltip);
                }
                menuItem.mouseenter(function() {
                    MenuSystem.dismissUntil(level);
                });
            }
        } else if ("heading" in item) {
            $('<div></div>').addClass("menu-section").text(item.heading).appendTo(menu);
        } else {
            $('<hr />').appendTo(menu);
        }
    };
    
    for (var i = 0; i < items.length; i++) {
        createMenuItem(items[i]);
    }
    
    var level = MenuSystem.showMenu(menu, "onDismiss" in options ? options.onDismiss : function(){});
    if (options.horizontal) {
        MenuSystem.positionMenuLeftRight(menu, $(elmt));
    } else {
        MenuSystem.positionMenuAboveBelow(menu, $(elmt));
    }
};
