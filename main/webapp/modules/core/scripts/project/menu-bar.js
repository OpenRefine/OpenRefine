function MenuBar(div) {
    this._div = div;
    this._initializeUI();
}

MenuBar.MenuItems = [
    {
        "id" : "core/project",
        "label" : "Project",
        "submenu" : [
            /*
            {
                "label": "Data Model",
                "submenu": [
                    {
                        "label": "Denormalize Records",
                        "click": function() { MenuBar.handlers.denormalizeRecords(); }
                    }
                ]
            },
            {},
            */
            {
                "id" : "core/rename-project",
                "label": "Rename...",
                "click": function() { MenuBar.handlers.renameProject(); }
            },
            {},
            {
                "id" : "core/export",
                "label": "Export Filtered Rows",
                "submenu": [
                    {
                        "id" : "core/export-tsv",
                        "label": "Tab-Separated Value",
                        "click": function() { MenuBar.handlers.exportRows("tsv", "tsv"); }
                    },
                    {
                        "id" : "core/export-csv",
                        "label": "Comma-Separated Value",
                        "click": function() { MenuBar.handlers.exportRows("csv", "csv"); }
                    },
                    {
                        "id" : "core/export-html-table",
                        "label": "HTML Table",
                        "click": function() { MenuBar.handlers.exportRows("html", "html"); }
                    },
                    {
                        "id" : "core/export-excel",
                        "label": "Excel",
                        "click": function() { MenuBar.handlers.exportRows("xls", "xls"); }
                    },
                    {},
                    {
                        "id" : "core/export-tripleloader",
                        "label": "Tripleloader",
                        "click": function() { MenuBar.handlers.exportTripleloader("tripleloader"); }
                    },
                    {
                        "id" : "core/export-mqlwrite",
                        "label": "MQLWrite",
                        "click": function() { MenuBar.handlers.exportTripleloader("mqlwrite"); }
                    },
                    {},
                    {
                        "id" : "core/export-templating",
                        "label": "Templating...",
                        "click": function() { new TemplatingExporterDialog(); }
                    }
                ]
            },
            {
                "id" : "core/export-project",
                "label": "Export Project",
                "click": function() { MenuBar.handlers.exportProject(); }
            }
        ]
    },
    {
        "id" : "core/schemas",
        "label" : "Schemas",
        "submenu" : [
            /*{
                "id" : "core/auto-schema-alignment",
                label: "Auto-Align with Freebase ...",
                click: function() { MenuBar.handlers.autoSchemaAlignment(); }
            },*/
            {
                "id" : "core/schema-alignment",
                label: "Edit Schema Aligment Skeleton ...",
                click: function() { MenuBar.handlers.editSchemaAlignment(false); }
            },
            {
                "id" : "core/reset-schema-alignment",
                label: "Reset Schema Alignment Skeleton ...",
                click: function() { MenuBar.handlers.editSchemaAlignment(true); }
            },
            {},
            {
                "id" : "core/load-info-freebase",
                label: "Load into Freebase ...",
                click: function() { MenuBar.handlers.loadIntoFreebase(); }
            },
            {
                "id" : "core/import-qa-data",
                label: "Import QA Data",
                click: function() { MenuBar.handlers.importQAData(); }
            }
        ]
    }
];

MenuBar.appendTo = function(path, what) {
    MenuBar.appendTo(MenuBar.MenuItems, path, what);
};

MenuBar.insertBefore = function(path, what) {
    MenuSystem.insertBefore(MenuBar.MenuItems, path, what);
};

MenuBar.insertAfter = function(path, what) {
    MenuSystem.insertAfter(MenuBar.MenuItems, path, what);
};

MenuBar.handlers = {};

MenuBar.prototype.resize = function() {
};

MenuBar.prototype._initializeUI = function() {
    this._mode = "inactive";
    this._menuItemRecords = [];

    this._div.addClass("menu-bar").html("&nbsp;");
    this._innerDiv = $('<div></div>').addClass("menu-bar-inner").appendTo(this._div);

    var self = this;
    for (var i = 0; i < MenuBar.MenuItems.length; i++) {
        var menuItem = MenuBar.MenuItems[i];
        this._createTopLevelMenuItem(menuItem.label, menuItem.submenu);
    }
    this._wireAllMenuItemsInactive();
};

MenuBar.prototype._createTopLevelMenuItem = function(label, submenu) {
    var self = this;

    var menuItem = MenuSystem.createMenuItem().text(label).appendTo(this._innerDiv);

    this._menuItemRecords.push({
        menuItem: menuItem,
        show: function() {
            MenuSystem.dismissUntil(self._level);

            menuItem.addClass("menu-expanded");

            MenuSystem.createAndShowStandardMenu(
                submenu,
                this,
                {
                    horizontal: false,
                    onDismiss: function() {
                        menuItem.removeClass("menu-expanded");
                    }
                }
            );
        }
    });
};

MenuBar.prototype._wireMenuItemInactive = function(record) {
    var self = this;
    var click = function() {
        self._activateMenu();
        record.show.apply(record.menuItem[0]);
    };

    record.menuItem.click(function() {
        // because we're going to rewire the menu bar, we have to
        // make this asynchronous, or jquery event binding won't work.
        window.setTimeout(click, 100);
    });
};

MenuBar.prototype._wireAllMenuItemsInactive = function() {
    for (var i = 0; i < this._menuItemRecords.length; i++) {
        this._wireMenuItemInactive(this._menuItemRecords[i]);
    }
};

MenuBar.prototype._wireMenuItemActive = function(record) {
    record.menuItem.mouseover(function() {
        record.show.apply(this);
    });
};

MenuBar.prototype._wireAllMenuItemsActive = function() {
    for (var i = 0; i < this._menuItemRecords.length; i++) {
        this._wireMenuItemActive(this._menuItemRecords[i]);
    }
};

MenuBar.prototype._activateMenu = function() {
    var self = this;

    var top = this._innerDiv.offset().top;

    this._innerDiv.remove().css("top", top + "px");
    this._wireAllMenuItemsActive();
    this._mode = "active";

    this._level = MenuSystem.showMenu(this._innerDiv, function() {
        self._deactivateMenu();
    });
};

MenuBar.prototype._deactivateMenu = function() {
    this._innerDiv.remove()
        .css("z-index", "auto")
        .css("top", "0px")
        .appendTo(this._div);

    this._wireAllMenuItemsInactive();
    this._mode = "inactive";
};

MenuBar.handlers.exportTripleloader = function(format) {
    if (!theProject.overlayModels.freebaseProtograph) {
        alert(
            "You haven't done any schema alignment yet,\nso there is no triple to export.\n\n" +
            "Use the Schemas > Edit Schema Alignment Skeleton...\ncommand to align your data with Freebase schemas first."
        );
    } else {
        MenuBar.handlers.exportRows(format, "txt");
    }
};

MenuBar.handlers.exportRows = function(format, ext) {
    var name = $.trim(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/export-rows/" + name + "." + ext)
        .attr("target", "gridworks-export");

    $('<input />')
        .attr("name", "engine")
        .attr("value", JSON.stringify(ui.browsingEngine.getJSON()))
        .appendTo(form);
    $('<input />')
        .attr("name", "project")
        .attr("value", theProject.id)
        .appendTo(form);
    $('<input />')
        .attr("name", "format")
        .attr("value", format)
        .appendTo(form);

    document.body.appendChild(form);

    window.open("about:blank", "gridworks-export");
    form.submit();
    
    document.body.removeChild(form);
};

MenuBar.handlers.exportProject = function() {
    var name = $.trim(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/export-project/" + name + ".gridworks.tar.gz")
        .attr("target", "gridworks-export");
    $('<input />')
        .attr("name", "project")
        .attr("value", theProject.id)
        .appendTo(form);

    document.body.appendChild(form);

    window.open("about:blank", "gridworks-export");
    form.submit();

    document.body.removeChild(form);
};

MenuBar.handlers.renameProject = function() {
    var name = window.prompt("Rename Project", theProject.metadata.name);
    if (name == null) {
        return;
    }

    name = $.trim(name);
    if (theProject.metadata.name == name || name.length == 0) {
        return;
    }

    $.ajax({
        type: "POST",
        url: "/command/rename-project",
        data: { "project" : theProject.id, "name" : name },
        dataType: "json",
        success: function (data) {
            if (data && typeof data.code != 'undefined' && data.code == "ok") {
                theProject.metadata.name = name;
                Gridworks.setTitle();
            } else {
                alert("Failed to rename project: " + data.message);
            }
        }
    });
};

MenuBar.handlers.autoSchemaAlignment = function() {
    //SchemaAlignment.autoAlign();
};

MenuBar.handlers.editSchemaAlignment = function(reset) {
    new SchemaAlignmentDialog(
        reset ? null : theProject.overlayModels.freebaseProtograph, function(newProtograph) {});
};

MenuBar.handlers.loadIntoFreebase = function() {
    new FreebaseLoadingDialog();
};

MenuBar.handlers.importQAData = function() {
    Gridworks.postProcess(
        "import-qa-data",
        {},
        {},
        { cellsChanged: true }
    );
};
