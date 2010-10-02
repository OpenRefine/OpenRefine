function ExtensionBar(div) {
    this._div = div;
    this._initializeUI();
}

ExtensionBar.MenuItems = [
    {
        "id" : "freebase",
        "label" : "Freebase",
        "submenu" : [
            {
                "id" : "freebase/schema-alignment",
                label: "Edit Schema Aligment Skeleton ...",
                click: function() { ExtensionBar.handlers.editSchemaAlignment(false); }
            },
            {
                "id" : "freebase/reset-schema-alignment",
                label: "Reset Schema Alignment Skeleton ...",
                click: function() { ExtensionBar.handlers.editSchemaAlignment(true); }
            },
            {},
            {
                "id" : "freebase/load-info-freebase",
                label: "Load into Freebase ...",
                click: function() { ExtensionBar.handlers.loadIntoFreebase(); }
            },
            {
                "id" : "freebase/browse-load",
                label: "Browse to Data Load ...",
                click: function() { ExtensionBar.handlers.browseToDataLoad(); }
            },
            {
                "id" : "freebase/import-qa-data",
                label: "Import QA Data",
                click: function() { ExtensionBar.handlers.importQAData(); }
            }
        ]
    }
];

ExtensionBar.addExtensionMenu = function(what) {
    ExtensionBar.appendTo(ExtensionBar.MenuItems, [], what);
}

ExtensionBar.appendTo = function(path, what) {
    ExtensionBar.appendTo(ExtensionBar.MenuItems, path, what);
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

    var menuItem = $("<a>").addClass("app-menu-button").text(label);
    
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

ExtensionBar.handlers.editSchemaAlignment = function(reset) {
    new SchemaAlignmentDialog(
        reset ? null : theProject.overlayModels.freebaseProtograph, function(newProtograph) {});
};

ExtensionBar.handlers.loadIntoFreebase = function() {
    new FreebaseLoadingDialog();
};

ExtensionBar.handlers.browseToDataLoad = function() {
    // The form has to be created as part of the click handler. If you create it
    // inside the getJSON success handler, it won't work.
    
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "GET")
        .attr("target", "dataload");

    document.body.appendChild(form);
    var w = window.open("about:blank", "dataload");
    
    $.getJSON(
        "/command/core/get-preference?" + $.param({ project: theProject.id, name: "freebase.load.jobID" }),
        null,
        function(data) {
            if (data.value == null) {
                alert("You have not tried to load the data in this project into Freebase yet.");
            } else {
                $(form).attr("action", "http://refinery.freebaseapps.com/load/" + data.value);
                form.submit();
                w.focus();
            }
            document.body.removeChild(form);
        }
    );
};

ExtensionBar.handlers.importQAData = function() {
    Refine.postCoreProcess(
        "import-qa-data",
        {},
        {},
        { cellsChanged: true }
    );
};
