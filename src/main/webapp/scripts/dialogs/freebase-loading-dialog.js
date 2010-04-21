function FreebaseLoadingDialog() {
    this._createDialog();
}

FreebaseLoadingDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("900px");
    
    var header = $('<div></div>').addClass("dialog-header").text('Load Data into Freebase').appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $(
        '<div class="dialog-footer">' +
           '<table width="100%"><tr>' +
             '<td class="left" style="text-align: left"></td>' + 
             '<td class="right" style="text-align: right"></td>' +
           '</tr></table>' +
        '</div>'
    ).appendTo(frame);
            
    $.post(
        "/command/export-rows?" + $.param({ 
            project: theProject.id, 
            format : "tripleloader" 
        }),
        {},
        function(data) {
            frame.find(".dialog-body").html('<div class="freebase-loading-tripleloader-data">' + data + '</pre>');
            self._level = DialogSystem.showDialog(frame);
        }
    );
    
    var left_footer = footer.find(".left");    
    $('<button></button>').text("Cancel").click(function() { 
        self._dismiss(); 
    }).appendTo(left_footer);
    
    var right_footer = footer.find(".right");    
    
    //$('<div class="freebase-loading-authorization signedin" style="display: none">Signed in as: <span class="user"></span></div>').appendTo(right_footer);
    
    var selector = $('<span>').addClass("freebase-loading-graph-selector").html("Load into " +
        '<input type="radio" bind="sandbox" id="freebase-loading-graph-selector-sandbox" name="graph-selector" checked="checked" value="sandbox"/><label class="sandbox" for="freebase-loading-graph-selector-sandbox" title="Load into the sandbox">sandbox</label>' +
        '<input type="radio" bind="otg" id="freebase-loading-graph-selector-otg" name="graph-selector" value="otg"/><label class="otg" for="freebase-loading-graph-selector-otg" title="Load into Freebase">freebase</label>'
    ).buttonset().appendTo(right_footer);
            
    $('<button></button>').text("Load").click(function() { 
        self._check_authorization(self._load); 
    }).appendTo(right_footer);
    
    this._elmts = DOM.bind(frame);

    this._elmts.otg.click(function() {
        if (!confirm("are you sure?")) {
            self._elmts.sandbox.attr("checked","checked");
            self._elmts.otg.removeAttr("checked");
            selector.find("input").button('refresh');
        }
    });
    
};

FreebaseLoadingDialog.prototype._check_authorization = function(cont) {
    var freebase = this._elmts.otg.attr("checked");
    var provider = (freebase) ? "freebase" : "sandbox";
    $.get("/command/check-authorization",{ "provider" : provider }, function(data) {
        if ("status" in data && data.status == "200 OK") {
            if (typeof cont == "function") cont();
        } else {
            alert("Sorry, we're working very hard to make this happen.");
            //Sign.signin(cont,provider);
        }
    },"json");
};

FreebaseLoadingDialog.prototype._load = function() {
    // do the real loading here
}

FreebaseLoadingDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

