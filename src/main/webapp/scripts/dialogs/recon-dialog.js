function ReconDialog(column, types) {
    this._column = column;
    this._types = types.slice(0, 10);
    
    var defaultTypes = {
        "/people/person" : {
            name: "Person"
        },
        "/location/location" : {
            name: "Location"
        }
    };
    $.each(this._types, function() {
        delete defaultTypes[this.id];
    });
    for (var id in defaultTypes) {
        this._types.push({
            id: id,
            name: defaultTypes[id].name
        });
    }
    
    this._createDialog();
}

ReconDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Reconcile column " + this._column.name).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
        '<div id="recon-dialog-tabs">' +
            '<ul>' +
                '<li><a href="#recon-dialog-tabs-heuristic">Heuristic</a></li>' +
                '<li><a href="#recon-dialog-tabs-strict">Strict</a></li>' +
            '</ul>' +
            '<div id="recon-dialog-tabs-heuristic">' +
                '<div class="grid-layout layout-normal layout-full"><table>' +
                    '<tr>' +
                        '<td>Reconcile each cell to a Freebase topic of type:</td>' +
                        '<td>Also use relevant details from other columns:</td>' +
                    '</tr>' +
                    '<tr>' +
                        '<td>' +
                            '<div class="recon-dialog-heuristic-types-container" bind="heuristicTypeContainer">' +
                            '</div>' +
                            '<table class="recon-dialog-heuristic-other-type-container recon-dialog-inner-layout">' +
                                '<tr>' +
                                    '<td width="1"><input type="radio" name="recon-dialog-type-choice" value=""></td>' +
                                    '<td>Search for type: <input size="20" bind="heuristicTypeInput" /></td>' +
                                '<tr>' +
                            '</table>' +
                        '</td>' +
                        '<td width="50%">' +
                            '<div class="recon-dialog-heuristic-details-container" bind="heuristicDetailContainer"></div>' +
                        '</td>' +
                    '</tr>' +
                    '<tr>' +
                        '<td>' +
                            '<input type="checkbox" checked bind="heuristicAutomatchCheck" /> Auto-match candidates with high confidence' +
                        '</td>' +
                        '<td>' +
                            'Use ' +
                            '<input type="radio" name="recon-dialog-heuristic-service" value="relevance" checked="" /> relevance service ' +
                            '<input type="radio" name="recon-dialog-heuristic-service" value="recon" /> recon service ' +
                        '</td>' +
                    '</tr>' +
                '</table></div>' +
            '</div>' +
            '<div id="recon-dialog-tabs-strict" style="display: none;">' +
                '<p>Each cell contains:</p>' +
                '<div class="grid-layout layout-normal layout-full"><table>' +
                    '<tr><td width="1%"><input type="radio" name="recon-dialog-strict-choice" value="id" checked /></td><td>a Freebase ID, e.g., /en/solar_system</td></tr>' +
                    '<tr><td><input type="radio" name="recon-dialog-strict-choice" value="guid" /></td><td>a Freebase GUID, e.g., #9202a8c04000641f80000000000354ae</td></tr>' +
                    '<tr>' +
                        '<td width="1%"><input type="radio" name="recon-dialog-strict-choice" value="key" /></td>' +
                        '<td>' +
                            '<div class="grid-layout layout-tighter layout-full"><table>' +
                                '<tr><td colspan="2">a Freebase key in</td></tr>' +
                                '<tr>' +
                                    '<td width="1%"><input type="radio" name="recon-dialog-strict-namespace-choice" value="/wikipedia/en" nsName="Wikipedia EN" checked /></td>' +
                                    '<td>the Wikipedia English namespace</td>' +
                                '</tr>' +
                                '<tr>' +
                                    '<td width="1%"><input type="radio" name="recon-dialog-strict-namespace-choice" value="other" /></td>' +
                                    '<td>this namespace: <input bind="strictNamespaceInput" /></td>' +
                                '</tr>' +
                            '</table></div>' +
                        '</td>' +
                    '</tr>' +
                '</table></div>' +
            '</div>' +
        '</div>'
    ).appendTo(body);
    
    this._elmts = DOM.bind(html);
    this._populateDialog();
    
    $('<button></button>').text("Start Reconciling").click(function() { self._onOK(); }).appendTo(footer);
    $('<button></button>').text("Cancel").click(function() { self._dismiss(); }).appendTo(footer);
    
    this._level = DialogSystem.showDialog(frame);
    
    $("#recon-dialog-tabs").tabs();
    $("#recon-dialog-tabs-strict").css("display", "");
    
    this._wireEvents();
};

ReconDialog.prototype._populateDialog = function() {
    var self = this;
    
    /*
     *  Populate types in heuristic tab
     */
    var typeTableContainer = $('<div>').addClass("grid-layout layout-tighter").appendTo(this._elmts.heuristicTypeContainer);
    var typeTable = $('<table></table>').appendTo(typeTableContainer)[0];
    var createTypeChoice = function(type, check) {
        var tr = typeTable.insertRow(typeTable.rows.length);
        var td0 = tr.insertCell(0);
        var td1 = tr.insertCell(1);
        
        td0.width = "1%";
        var radio = $('<input type="radio" name="recon-dialog-type-choice">')
            .attr("value", type.id)
            .attr("typeName", type.name)
            .appendTo(td0)
            .click(function() {
                self._rewirePropertySuggests(this.value);
            });
            
        if (check) {
            radio.attr("checked", "true");
        }
            
        $(td1).html(type.name + '<br/><span class="recon-dialog-type-id">' + type.id + '</span>');
    };
    for (var i = 0; i < this._types.length; i++) {
        createTypeChoice(this._types[i], i === 0);
    }
    
    /*
     *  Populate properties in heuristic tab
     */
    var heuristicDetailTableContainer = $('<div>')
        .addClass("grid-layout layout-tighter")
        .appendTo(this._elmts.heuristicDetailContainer);
        
    var heuristicDetailTable = $(
        '<table>' +
            '<tr><th>Column</th><th>Freebase property</th></tr>' +
        '</table>'
    ).appendTo(heuristicDetailTableContainer)[0];
    
    function renderDetailColumn(column) {
        var tr = heuristicDetailTable.insertRow(heuristicDetailTable.rows.length);
        var td0 = tr.insertCell(0);
        var td1 = tr.insertCell(1);
        
        $(td0).html(column.name);
        $('<input size="15" name="recon-dialog-heuristic-property" />')
            .attr("columnName", column.name)
            .appendTo(td1);
    }
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        if (column !== this._column) {
            renderDetailColumn(column);
        }
    }
};

ReconDialog.prototype._wireEvents = function() {
    var self = this;
    
    this._elmts.heuristicTypeInput
        .suggest({ type : '/type/type' })
        .bind("fb-select", function(e, data) {
            $('input[name="recon-dialog-type-choice"][value=""]').attr("checked", "true");
            
            self._rewirePropertySuggests(data.id);
        });
    
    this._rewirePropertySuggests(this._types[0].id);
    
    this._elmts.strictNamespaceInput
        .suggest({ type: '/type/namespace' })
        .bind("fb-select", function(e, data) {
            $('input[name="recon-dialog-strict-choice"][value="key"]').attr("checked", "true");
            $('input[name="recon-dialog-strict-namespace-choice"][value="other"]').attr("checked", "true");
        });
};

ReconDialog.prototype._rewirePropertySuggests = function(schema) {
    var inputs = $('input[name="recon-dialog-heuristic-property"]');
    
    inputs.suggest({
        type: '/type/property',
        schema: schema || "/common/topic"
    }).bind("fb-select", function(e, data) {
        $('input[name="recon-dialog-heuristic-service"][value="recon"]').attr("checked", "true"); 
    });
};

ReconDialog.prototype._onOK = function() {
    var tab = $("#recon-dialog-tabs").tabs('option', 'selected');
    if (tab === 0) {
        this._onDoHeuristic();
    } else {
        this._onDoStrict();
    }
};

ReconDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

ReconDialog.prototype._onDoHeuristic = function() {
    var type = this._elmts.heuristicTypeInput.data("data.suggest");

    var choices = $('input[name="recon-dialog-type-choice"]:checked');
    if (choices != null && choices.length > 0 && choices[0].value != "") {
        type = {
            id: choices[0].value,
            name: choices.attr("typeName")
        };
    }
    
    if (!type)  {
        alert("Please specify a type.");
    } else {
        var columnDetails = [];
        var propertyInputs = $('input[name="recon-dialog-heuristic-property"]');
        $.each(propertyInputs, function() {
            var property = $(this).data("data.suggest");
            if (property != null) {
                columnDetails.push({
                    column: this.getAttribute("columnName"),
                    property: {
                        id: property.id,
                        name: property.name
                    }
                });
            }
        });
        
        Gridworks.postProcess(
            "reconcile",
            {},
            {
                columnName: this._column.name,
                config: JSON.stringify({
                    mode: "heuristic",
                    service: $('input[name="recon-dialog-heuristic-service"]:checked')[0].value,
                    type: {
                        id: type.id, 
                        name: type.name
                    },
                    autoMatch: this._elmts.heuristicAutomatchCheck[0].checked,
                    columnDetails: columnDetails
                })
            },
            { cellsChanged: true, columnStatsChanged: true }
        );
        
        this._dismiss();
    }
};

ReconDialog.prototype._onDoStrict = function() {
    var bodyParams;
    
    var match = $('input[name="recon-dialog-strict-choice"]:checked')[0].value;
    if (match == "key") {
        var namespaceChoice = $('input[name="recon-dialog-strict-namespace-choice"]:checked')[0];
        var namespace;
        
        if (namespaceChoice.value == "other") {
            var suggest = this._elmts.strictNamespaceInput.data("data.suggest");
            if (!suggest) {
                alert("Please specify a namespace.");
                return;
            }
            namespace = {
                id: suggest.id,
                name: suggest.name
            };
        } else {
            namespace = {
                id: namespaceChoice.value,
                name: namespaceChoice.getAttribute("nsName")
            };
        }
    
        bodyParams = {
            columnName: this._column.name,
            config: JSON.stringify({
                mode: "strict",
                match: "key",
                namespace: namespace
            })
        };
    } else if (match == "id") {
        bodyParams = {
            columnName: this._column.name,
            config: JSON.stringify({
                mode: "strict",
                match: "id"
            })
        };
    } else if (match == "guid") {
        bodyParams = {
            columnName: this._column.name,
            config: JSON.stringify({
                mode: "strict",
                match: "guid"
            })
        };
    }
    
    Gridworks.postProcess(
        "reconcile",
        {},
        bodyParams,
        { cellsChanged: true, columnStatsChanged: true }
    );
    
    this._dismiss();
};