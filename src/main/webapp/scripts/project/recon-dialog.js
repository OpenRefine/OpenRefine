function ReconDialog(column, types) {
    this._column = column;
    this._types = types;
    this._createDialog();
}

ReconDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("500px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Reconcile column " + this._column.headerLabel).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    $('<p>').text("Reconcile cell values to Freebase topics of type:").appendTo(body);
    
    if (this._types.length > 0) {
        var createTypeChoice = function(type) {
            var div = $('<div>').appendTo(body);
            $('<input type="radio" name="recon-dialog-type-choice">')
                .attr("value", type.id)
                .attr("typeName", type.name)
                .appendTo(div);
                
            $('<span></span>').text(" " + type.name).appendTo(div);
            $('<span></span>').text(" (" + type.id + ")").appendTo(div);
        };
        for (var i = 0; i < this._types.length && i < 7; i++) {
            createTypeChoice(this._types[i]);
        }
        
        var divCustom = $('<div>').appendTo(body);
        $('<input type="radio" name="recon-dialog-type-choice">')
            .attr("value", "")
            .appendTo(divCustom);
            
        $('<span></span>').text(" Other: ").appendTo(divCustom);
        
        var input = $('<input />').appendTo(divCustom);
    } else {
        var input = $('<input />').appendTo($('<p></p>').appendTo(body));
    }
    
    var type = null;
    input.suggest({ type : '/type/type' }).bind("fb-select", function(e, data) {
        type = {
            id: data.id,
            name: data.name
        };
        $('input[name="recon-dialog-type-choice"][value=""]').attr("checked", "true");
    });
    
    var optionDiv = $('<p>').appendTo(body);
    var autoMatchCheckbox = $('<input type="checkbox" checked />').appendTo(optionDiv);
    $('<span>').text(" Auto-match correctly-typed candidates scoring at least ").appendTo(optionDiv);
    var minScoreInput = $('<input/>').attr("value", "100").appendTo(optionDiv);
    
    $('<button></button>').text("Start Reconciling").click(function() {
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
            DialogSystem.dismissUntil(level - 1);
            $.post(
                "/command/reconcile?" + $.param({
                    project: theProject.id, 
                    columnName: self._column.headerLabel, 
                    typeID: type.id, 
                    typeName: type.name,
                    autoMatch: autoMatchCheckbox[0].checked,
                    minScore: minScoreInput[0].value
                }), 
                { engine: JSON.stringify(ui.browsingEngine.getJSON()) },
                function(data) {
                    if (data.code != "error") {
                        ui.processWidget.update();
                    } else {
                        alert(data.message);
                    }
                },
                "json"
            );
        }
    }).appendTo(footer);
    
    $('<button></button>').text("Cancel").click(function() {
        DialogSystem.dismissUntil(level - 1);
    }).appendTo(footer);
    
    var level = DialogSystem.showDialog(frame);
    
    input[0].focus();
};

