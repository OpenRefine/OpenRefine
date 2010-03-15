function ExtendDataPreviewDialog(column, rowIndices, onDone) {
    this._column = column;
    this._rowIndices = rowIndices;
    this._onDone = onDone;
    this._extension = { properties: [] };

    var self = this;
    var frame = this._frame = DialogSystem.createDialog();
    frame.width("900px").addClass("extend-data-preview-dialog");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add Columns from Freebase Based on Column " + column.name).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    var html = $(
        '<div class="grid-layout layout-normal layout-full"><table style="height: 600px">' +
            '<tr>' +
                '<td width="150" height="1">Add Property</td>' +
                '<td height="1">Preview</td>' +
            '</tr>' +
            '<tr>' +
                '<td height="1"><div class="input-container"><input bind="addPropertyInput" /></div></td>' +
                '<td height="100%" rowspan="3"><div class="preview-container" bind="previewContainer"></div></td>' +
            '</tr>' +
            '<tr>' +
                '<td height="1">Suggested Properties</td>' +
            '</tr>' +
            '<tr>' +
                '<td height="99%"><div class="suggested-property-container" bind="suggestedPropertyContainer"></div></td>' +
            '</tr>' +
        '</table></div>'
    ).appendTo(body);
    
    this._elmts = DOM.bind(html);
    
    $('<button></button>').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._onDone(self._previewWidget.getExpression(true));
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
    
    ExtendDataPreviewDialog.getAllProperties(column.reconConfig.type.id, function(properties) {
        self._show(properties);
    });
};

ExtendDataPreviewDialog.getAllProperties = function(typeID, onDone) {
    var query = {
        id : typeID,
        type : "/type/type",
        "/freebase/type_hints/included_types" : [{
            optional: true,
            properties : [{
                id : null,
                name : null,
                "/type/property/expected_type" : {
                    id : null,
                    "/freebase/type_hints/mediator" : []
                },
                sort : "name"
            }]
        }],
        properties : [{
            id : null,
            name : null,
            "/type/property/expected_type" : {
                id : null,
                "/freebase/type_hints/mediator" : []
            },
            sort : "name"
        }]
    };
    
    var allProperties = [];
    var processProperty = function(property) {
        var expectedType = property["/type/property/expected_type"];
        if (expectedType["/freebase/type_hints/mediator"].length > 0 && expectedType["/freebase/type_hints/mediator"][0]) {
            
        } else {
            allProperties.push({
                id : property.id,
                name : property.name,
                expected : expectedType.id
            });
        }
    };
    var processProperties = function(properties) {
        $.each(properties, function() { processProperty(this); });
    };
    
    $.getJSON(
        "http://api.freebase.com/api/service/mqlread?query=" + escape(JSON.stringify({ query : query })) + "&callback=?",
        null,
        function(o) {
            if ("result" in o) {
                processProperties(o.result.properties);
                $.each(o.result["/freebase/type_hints/included_types"], function() {
                    processProperties(this.properties);
                })
                
                onDone(allProperties);
            } else {
                onDone([]);
            }
        },
        "jsonp"
    );    
};

ExtendDataPreviewDialog.prototype._show = function(properties) {
    this._level = DialogSystem.showDialog(this._frame);
    
    var self = this;
    var container = this._elmts.suggestedPropertyContainer;
    var renderSuggestedProperty = function(property) {
        var div = $('<div>').addClass("suggested-property").appendTo(container);
        $('<a>').attr("href", "javascript:{}").text(property.name).appendTo(div).click(function() {
            self._addProperty(property);
        });
    };
    for (var i = 0; i < properties.length; i++) {
        renderSuggestedProperty(properties[i]);
    }
    
    var suggestConfig = {
        type: '/type/property',
        schema: this._column.reconConfig.type.id
    };
    
    this._elmts.addPropertyInput.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
        self._addProperty({
            id : data.id,
            name: data.name,
            expected: data["/type/property/expected_type"]
        });
    });
};

ExtendDataPreviewDialog.prototype._update = function() {
    this._elmts.previewContainer.empty().text("Querying Freebase ...");
    
    var self = this;
    var params = {
        project: theProject.id,
        columnName: this._column.name
    };
    
    $.post(
        "/command/preview-extend-data?" + $.param(params), 
        {
            rowIndices: JSON.stringify(this._rowIndices),
            extension: JSON.stringify(this._extension)
        },
        function(data) {
            self._renderPreview(data)
        },
        "json"
    );
};

ExtendDataPreviewDialog.prototype._addProperty = function(p) {
    var addSeveralToList = function(properties, oldProperties) {
        for (var i = 0; i < properties.length; i++) {
            addToList(properties[i], oldProperties);
        }
    };
    var addToList = function(property, oldProperties) {
        for (var i = 0; i < oldProperties.length; i++) {
            var oldProperty = oldProperties[i];
            if (oldProperty.id == property.id) {
                if ("included" in property) {
                    oldProperty.included = "included" in oldProperty ? 
                        (oldProperty.included || property.included) : 
                        property.included;
                }
                
                if ("properties" in property) {
                    if ("properties" in oldProperty) {
                        addSeveralToList(property.properties, oldProperty.properties);
                    } else {
                        oldProperty.properties = property.properties;
                    }
                }
                return;
            }
        }
        
        oldProperties.push(property);
    };
    
    addToList(p, this._extension.properties);
    
    this._update();
};

ExtendDataPreviewDialog.prototype._removeProperty = function(path) {
    var removeFromList = function(path, index, properties) {
        var id = path[index];
        
        for (var i = properties.length - 1; i >= 0; i--) {
            var property = properties[i];
            if (property.id == id) {
                if (index === path.length - 1) {
                    if ("included" in property) {
                        delete property.included;
                    }
                } else if ("properties" in property && property.properties.length > 0) {
                    removeFromList(path, index + 1, property.properties);
                }
                
                if (!("properties" in property) || property.properties.length === 0) {
                    properties.splice(i, 1);
                }
                
                return;
            }
        }
    };
    
    removeFromList(path, 0, this._extension.properties);
    
    this._update();
};

ExtendDataPreviewDialog.prototype._renderPreview = function(data) {
    var self = this;
    var container = this._elmts.previewContainer.empty();
    if (data.code == "error") {
        container.text("Error.");
        return;
    }
    
    var table = $('<table>')[0];
    var trHead = table.insertRow(table.rows.length);
    $('<th>').appendTo(trHead).text(this._column.name);
    
    for (var c = 0; c < data.columns.length; c++) {
        var column = data.columns[c];
        var th = $('<th>').appendTo(trHead);
        
        $('<span>').html(column.names.join(" &raquo; ")).appendTo(th);
        $('<img>')
            .attr("src", "images/close.png")
            .attr("title", "Remove this column")
            .click(function() {
                self._removeProperty(column.path);
            }).appendTo(th);
    }
    
    for (var r = 0; r < data.rows.length; r++) {
        var tr = table.insertRow(table.rows.length);
        var row = data.rows[r];
        
        for (var c = 0; c < row.length; c++) {
            var td = tr.insertCell(tr.cells.length);
            var cell = row[c];
            if (cell != null) {
                if ($.isPlainObject(cell)) {
                    $('<a>').attr("href", "http://www.freebase.com/view" + cell.id).text(cell.name).appendTo(td);
                } else {
                    $('<span>').text(cell).appendTo(td);
                }
            }
        }
    }
    
    container.append(table);
}