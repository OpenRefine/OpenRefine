function MenuBar(div) {
    this._div = div;
    this._initializeUI();
}

MenuBar.prototype._initializeUI = function() {
    this._mode = "inactive";
    this._menuItemRecords = [];
    
    this._div.addClass("menu-bar").html("&nbsp;");
    this._innerDiv = $('<div></div>').addClass("menu-bar-inner").appendTo(this._div);

    var self = this;
    
    this._createTopLevelMenuItem("Project", [
        {
            "label": "Export Filtered Rows",
            "submenu": [
                {
                    "label": "Tab-Separated Value",
                    "click": function() { self._doExportRows("tsv", "tsv"); }
                },
                {
                    "label": "HTML Table",
                    "click": function() { self._doExportRows("html", "html"); }
                },
                {
                    "label": "Excel",
                    "click": function() { self._doExportRows("xls", "xls"); }
                },
                {},
                {
                    "label": "Tripleloader",
                    "click": function() { self._doExportTripleloader(); }
                }
            ]
        },
        {
            "label": "Export Project",
            "click": function() { self._exportProject(); }
        }
    ]);
    this._createTopLevelMenuItem("Schemas", [
        /*{
            label: "Auto-Align with Freebase ...",
            click: function() { self._doAutoSchemaAlignment(); }
        },*/
        {
            label: "Edit Schema Aligment Skeleton ...",
            click: function() { self._doEditSchemaAlignment(false); }
        },
        {
            label: "Reset Schema Alignment Skeleton ...",
            click: function() { self._doEditSchemaAlignment(true); }
        },
        {},
        {
            label: "Load into Freebase ...",
            click: function() {}
        }
    ]);
    
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

MenuBar.prototype._doExportTripleloader = function() {
    if (theProject.protograph == null) {
        alert(
            "You haven't done any schema alignment yet,\nso there is no triple to export.\n\n" +
            "Use the Schemas > Edit Schema Alignment Skeleton...\ncommand to align your data with Freebase schemas first."
        );
    } else {
        this._doExportRows("tripleloader", "txt");
    }
};

MenuBar.prototype._doExportRows = function(format, ext) {
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

MenuBar.prototype._exportProject = function() {
    var name = $(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/export-project/" + name + ".gridworks.tar")
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

MenuBar.prototype._doAutoSchemaAlignment = function() {
    //SchemaAlignment.autoAlign();
};

MenuBar.prototype._doEditSchemaAlignment = function(reset) {
    new SchemaAlignmentDialog(reset ? null : theProject.protograph, function(newProtograph) {});
};
