SchemaAlignmentDialog.UILink = function(link, table, expanded) {
    this._link = link;
    this._expanded = expanded;
    
    this._tr = table.insertRow(table.rows.length);
    this._tdMain = this._tr.insertCell(0);
    this._tdToggle = this._tr.insertCell(1);
    this._tdDetails = this._tr.insertCell(2);
    
    $(this._tdMain).addClass("schema-alignment-link-main").attr("width", "250").addClass("padded");
    $(this._tdToggle).addClass("schema-alignment-link-toggle").attr("width", "1%").addClass("padded");
    $(this._tdDetails).addClass("schema-alignment-link-details").attr("width", "90%");
    
    this._collapsedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("padded").html("...");
    this._expandedDetailDiv = $('<div></div>').appendTo(this._tdDetails).addClass("schema-alignment-detail-container");
    var self = this;
    var show = function() {
        if (self._expanded) {
            self._collapsedDetailDiv.hide();
            self._expandedDetailDiv.show();
        } else {
            self._collapsedDetailDiv.show();
            self._expandedDetailDiv.hide();
        }
    };
    show();
    
    $(this._tdToggle).html("&nbsp;");
    $('<img />')
        .attr("src", this._expanded ? "images/expanded.png" : "images/collapsed.png")
        .appendTo(this._tdToggle)
        .click(function() {
            self._expanded = !self._expanded;
            
            $(this).attr("src", self._expanded ? "images/expanded.png" : "images/collapsed.png");
            
            show();
        });
    
    this._renderMain();
    this._renderDetails();
};

SchemaAlignmentDialog.UILink.prototype._renderMain = function() {
    $(this._tdMain).empty()
    
    var label = this._link.property != null ? this._link.property.id : "property?";
    
    var self = this;
    
    var a = $('<a href="javascript:{}"></a>')
        .addClass("schema-alignment-link-tag")
        .html(label)
        .appendTo(this._tdMain)
        .click(function(evt) {
            self._showPropertySuggestPopup(this);
        });
        
    $('<img />').attr("src", "images/arrow-start.png").prependTo(a);
    $('<img />').attr("src", "images/arrow-end.png").appendTo(a);
};

SchemaAlignmentDialog.UILink.prototype._renderDetails = function() {
    var tableDetails = $('<table></table>').addClass("schema-alignment-table-layout").appendTo(this._expandedDetailDiv)[0];
    this._targetUI = new SchemaAlignmentDialog.UINode(
        this._link.target, 
        tableDetails, 
        { expanded: "links" in this._link.target && this._link.target.links.length > 0 });
};

SchemaAlignmentDialog.UILink.prototype._showPropertySuggestPopup = function(elmt) {
    self = this;
    
    var fakeMenu = MenuSystem.createMenu()
        .width(300)
        .height(100)
        .css("background", "none")
        .css("border", "none");
    
    var input = $('<input />').appendTo(fakeMenu);
    
    var level = MenuSystem.showMenu(fakeMenu, function(){});
    MenuSystem.positionMenuAboveBelow(fakeMenu, $(elmt));

    input.suggest({ type : '/type/property' }).bind("fb-select", function(e, data) {
        self._link.property = {
            id: data.id,
            name: data.name
        };
        
        window.setTimeout(function() {
            MenuSystem.dismissAll();
            self._renderMain();
        }, 100);
    });
    input[0].focus();
};

SchemaAlignmentDialog.UILink.prototype.getJSON = function() {
    if ("property" in this._link && this._link.property != null &&
        "target" in this._link && this._link.target != null) {
        
        var targetJSON = this._targetUI.getJSON();
        if (targetJSON != null) {
            return {
                property: cloneDeep(this._link.property),
                target: targetJSON
            };
        }
    }
    return null;
};
