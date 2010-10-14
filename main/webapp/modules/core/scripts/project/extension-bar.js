function ExtensionBar(div) {
    this._div = div;
    this._initializeUI();
}

ExtensionBar.MenuItems = [
];

ExtensionBar.addExtensionMenu = function(what) {
    MenuSystem.appendTo(ExtensionBar.MenuItems, [], what);
}

ExtensionBar.appendTo = function(path, what) {
    MenuSystem.appendTo(ExtensionBar.MenuItems, path, what);
};

ExtensionBar.insertBefore = function(path, what) {
    MenuSystem.insertBefore(ExtensionBar.MenuItems, path, what);
};

ExtensionBar.insertAfter = function(path, what) {
    MenuSystem.insertAfter(ExtensionBar.MenuItems, path, what);
};

ExtensionBar.prototype.resize = function() {
};

ExtensionBar.prototype._initializeUI = function() {
    var elmts = DOM.bind(this._div);
    for (var i = 0; i < ExtensionBar.MenuItems.length; i++) {
        var menuItem = ExtensionBar.MenuItems[i];
        var menuButton = this._createMenuButton(menuItem.label, menuItem.submenu);
        elmts.menuContainer.append(menuButton);
    }
};

ExtensionBar.prototype._createMenuButton = function(label, submenu) {
    var self = this;

    var menuItem = $("<a>").addClass("button").append('<span class="button-menu">' + label + '</span>');
    
    menuItem.click(function(evt) {
        MenuSystem.createAndShowStandardMenu(
            submenu,
            this,
            { horizontal: false }
        );

        evt.preventDefault();
        return false;
    });
    
    return menuItem;
};

ExtensionBar.handlers = {};

ExtensionBar.handlers.openWorkspaceDir = function() {
    $.ajax({
        type: "POST",
        url: "/command/core/open-workspace-dir",
        dataType: "json",
        success: function (data) {
            if (data.code != "ok" && "message" in data) {
                alert(data.message);
            }
        }
    });
};
