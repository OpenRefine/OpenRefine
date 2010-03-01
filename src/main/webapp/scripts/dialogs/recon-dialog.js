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
    
    var header = $('<div></div>').addClass("dialog-header").text("Reconcile column " + this._column.headerLabel).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    var html = $(
        '<div id="recon-dialog-tabs">' +
            '<ul>' +
                '<li><a href="#recon-dialog-tabs-heuristic">Heuristic</a></li>' +
                '<li><a href="#recon-dialog-tabs-strict">Strict</a></li>' +
            '</ul>' +
            '<div id="recon-dialog-tabs-heuristic">' +
                '<table class="recon-dialog-main-layout" width="100%">' +
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
                '</table>' +
                '<p>' +
                    '<input type="checkbox" checked bind="heuristicAutomatchCheck" /> Auto-match correctly-typed candidates scoring at least ' +
                    '<input size="3" value="100" bind="heuristicAutomatchScoreInput" />' +
                '</p>' +
            '</div>' +
            '<div id="recon-dialog-tabs-strict" style="display: none;">' +
                '<p>Each cell contains:</p>' +
                '<table class="recon-dialog-main-layout">' +
                    '<tr><td width="1%"><input type="radio" name="recon-dialog-strict-choice" value="id" checked /></td><td>a Freebase ID, e.g., /en/solar_system</td></tr>' +
                    '<tr><td><input type="radio" name="recon-dialog-strict-choice" value="guid" /></td><td>a Freebase GUID, e.g., #9202a8c04000641f80000000000354ae</td></tr>' +
                    '<tr>' +
                        '<td width="1%"><input type="radio" name="recon-dialog-strict-choice" value="key" /></td>' +
                        '<td>' +
                            '<table class="recon-dialog-inner-layout">' +
                                '<tr><td colspan="2">a Freebase key in</td></tr>' +
                                '<tr>' +
                                    '<td width="1%"><input type="radio" name="recon-dialog-strict-namespace-choice" value="wikipedia-en" checked /></td>' +
                                    '<td>the Wikipedia English namespace</td>' +
                                '</tr>' +
                                '<tr>' +
                                    '<td width="1%"><input type="radio" name="recon-dialog-strict-namespace-choice" value="other" /></td>' +
                                    '<td>this namespace: <input bind="strictNamespaceInput" /></td>' +
                                '</tr>' +
                            '</table>' +
                        '</td>' +
                    '</tr>' +
                '</table>' +
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
};

ReconDialog.prototype._populateDialog = function() {
    var self = this;
    
    /*
     *  Populate types in heuristic tab
     */
    var typeTable = $('<table></table>').addClass("recon-dialog-inner-layout").appendTo(this._elmts.heuristicTypeContainer)[0];
    var createTypeChoice = function(type, check) {
        var tr = typeTable.insertRow(typeTable.rows.length);
        var td0 = tr.insertCell(0);
        var td1 = tr.insertCell(1);
        
        td0.width = "1%";
        var checkbox = $('<input type="radio" name="recon-dialog-type-choice">')
            .attr("value", type.id)
            .attr("typeName", type.name)
            .appendTo(td0);
            
        if (check) {
            checkbox.attr("checked", "true");
        }
            
        $(td1).html(type.name + '<br/><span class="recon-dialog-type-id">' + type.id + '</span>');
    };
    for (var i = 0; i < this._types.length; i++) {
        createTypeChoice(this._types[i], i == 0);
    }
    
    this._elmts.heuristicTypeInput
        .suggest({ type : '/type/type' })
        .bind("fb-select", function(e, data) {
            $('input[name="recon-dialog-type-choice"][value=""]').attr("checked", "true");
        });
    
    /*
     *  Populate properties in heuristic tab
     */
    var heuristicDetailTable = $(
        '<table>' +
            '<tr><th>Column</th><th>Freebase property</th></tr>' +
        '</table>'
    ).addClass("recon-dialog-inner-layout").appendTo(this._elmts.heuristicDetailContainer)[0];
    
    function renderDetailColumn(column) {
        var tr = heuristicDetailTable.insertRow(heuristicDetailTable.rows.length);
        var td0 = tr.insertCell(0);
        var td1 = tr.insertCell(1);
        
        $(td0).html(column.headerLabel);
        $('<input size="15" />')
            .appendTo(td1)
            .suggest({ type: '/type/property' });
    }
    var columns = theProject.columnModel.columns;
    for (var i = 0; i < columns.length; i++) {
        var column = columns[i];
        if (column !== this._column) {
            renderDetailColumn(column);
        }
    }
    
    /*
     *  Populate strict tab
     */
     
    this._elmts.strictNamespaceInput
        .suggest({ type: '/type/namespace' })
        .bind("fb-select", function(e, data) {
            $('input[name="recon-dialog-strict-choice"][value="key"]').attr("checked", "true");
            $('input[name="recon-dialog-strict-namespace-choice"][value="other"]').attr("checked", "true");
        });
};

ReconDialog.prototype._onOK = function() {
    var tab = $("#recon-dialog-tabs").tabs('option', 'selected');
    if (tab == 0) {
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
    
    if (type == null)  {
        alert("Please specify a type.");
    } else {
        this._dismiss();
        
        Gridworks.postProcess(
            "reconcile",
            {
                columnName: this._column.headerLabel, 
                typeID: type.id, 
                typeName: type.name,
                autoMatch: this._elmts.heuristicAutomatchCheck[0].checked,
                minScore: this._elmts.heuristicAutomatchScoreInput[0].value
            }, 
            null,
            { cellsChanged: true, columnStatsChanged: true }
        );
    }
};