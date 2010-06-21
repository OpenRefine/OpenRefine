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
        if (defaultTypes.hasOwnProperty(id)) {
            this._types.push({
                id: id,
                name: defaultTypes[id].name
            });
        }
    }
    
    this._createDialog();
}

ReconDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/recon-dialog.html"));

    this._elmts = DOM.bind(dialog);
    this._elmts.dialogHeader.text("Reconcile column " + this._column.name);
    this._elmts.reconcileButton.click(function() { self._onOK(); });
    this._elmts.cancelButton.click(function() { self._dismiss(); });
    
    this._populateDialog();
    
    this._level = DialogSystem.showDialog(dialog);
    
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
        .suggestT({ type : '/type/type' })
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
    
    inputs.unbind().suggestP({
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
    if (choices !== null && choices.length > 0 && choices[0].value != "") {
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
            if (property && property.id) {
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