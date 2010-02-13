SchemaAlignmentDialog.UILink = function(link, table, options, parentUINode) {
    this._link = link;
    this._options = options;
    this._parentUINode = parentUINode;
    
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
        if (self._options.expanded) {
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
        .attr("src", this._options.expanded ? "images/expanded.png" : "images/collapsed.png")
        .appendTo(this._tdToggle)
        .click(function() {
            self._options.expanded = !self._options.expanded;
            
            $(this).attr("src", self._options.expanded ? "images/expanded.png" : "images/collapsed.png");
            
            show();
        });
    
    this._renderMain();
    this._renderDetails();
};

SchemaAlignmentDialog.UILink.prototype._renderMain = function() {
    $(this._tdMain).empty()
    
    var label = this._link.property != null ? this._link.property.id : "property?";
    
    var self = this;
    
    $('<img />')
        .attr("title", "remove property")
        .attr("src", "images/close.png")
        .css("cursor", "pointer")
        .prependTo(this._tdMain)
        .click(function() {
            window.setTimeout(function() {
                self._parentUINode.removeLink(self);
                self._tr.parentNode.removeChild(self._tr);
            }, 100);
        });
    
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
    
    var options = {
        type : '/type/property'
    };
    var expectedTypeID = this._parentUINode.getExpectedType();
    if (expectedTypeID != null) {
        options.mql_filter = [{
            "/type/property/schema" : { "id" : expectedTypeID }
        }];
    }

    input.suggest(options).bind("fb-select", function(e, data) {
        self._link.property = {
            id: data.id,
            name: data.name
        };
        self._configureTarget();
        
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

SchemaAlignmentDialog.UILink.prototype._configureTarget = function() {
    var self = this;
    $.getJSON(
        "http://api.freebase.com/api/service/mqlread?query=" + JSON.stringify({
            query: {
                "id" : this._link.property.id,
                "type" : "/type/property",
                "expected_type" : {
                    "id" : null,
                    "name" : null,
                    "/freebase/type_hints/mediator" : null
                }
            }
        }) + "&callback=?",
        null,
        function(o) {
            if ("result" in o) {
                var expected_type = o.result.expected_type;
                self._link.target.type = {
                    id: expected_type.id,
                    name: expected_type.name
                };
                if (expected_type["/freebase/type_hints/mediator"] == true) {
                    self._link.target.nodeType = "anonymous";
                } else if (expected_type.id == "/type/key") {
                    self._link.target.nodeType = "cell-as-key";
                } else if (expected_type.id.match(/^\/type\//)) {
                    self._link.target.nodeType = "cell-as-value";
                } else if (!("topic" in self._link.target)) {
                    self._link.target.nodeType = "cell-as-topic";
                    self._link.target.createForNoReconMatch = true;
                }
                
                self._targetUI.render();
            }
        },
        "jsonp"
    );
};
