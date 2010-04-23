function FreebaseLoadingDialog() {
    this._createDialog();
}

FreebaseLoadingDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    frame.width("800px");
    
    var header = $('<div></div>').addClass("dialog-header").text('Load Data into Freebase').appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $(
        '<div class="dialog-footer">' +
           '<table width="100%"><tr>' +
             '<td class="left" style="text-align: left" width="40%" nowrap="true"></td>' + 
             '<td class="center" style="text-align: center" width="20%" nowrap="true"></td>' +
             '<td class="right" style="text-align: right" width="40%" nowrap="true"></td>' +
           '</tr></table>' +
        '</div>'
    ).appendTo(frame);
            
    $.post(
        "/command/export-rows", 
        { 
            project: theProject.id, 
            format : "tripleloader" 
        },
        function(data) {
            body.html(
                '<div class="freebase-loading-tripleloader-data">' + data + '</div>' +
                '<div class="freebase-loading-tripleloader-info">' +
                  '<div>Describe the data you\'re about to load &not;</div>' +
                  '<textarea></textarea>' +
                '</div>'
            );
            self._level = DialogSystem.showDialog(frame);
        }
    );
    
    var left_footer = footer.find(".left");   
    
    var cancel_button = $('<button id="freebase-loading-cancel"></button>').text("Cancel").click(function() { 
        self._dismiss(); 
    }).appendTo(left_footer);
    
    var center_footer = footer.find(".center");    
    
    var authorization = $('<div>').addClass("freebase-loading-authorization").hide().appendTo(center_footer);

    var right_footer = footer.find(".right");    
    
    var selector = $('<span>').addClass("freebase-loading-graph-selector").html("Load this data into " +
        '<input type="radio" bind="sandbox" id="freebase-loading-graph-selector-sandbox" name="graph-selector" checked="checked" value="sandbox"/><label class="sandbox" for="freebase-loading-graph-selector-sandbox" title="Load into the sandbox">sandbox</label>' +
        '<input type="radio" bind="freebase" id="freebase-loading-graph-selector-freebase" name="graph-selector" value="freebase"/><label class="freebase" for="freebase-loading-graph-selector-freebase" title="Load into Freebase">freebase</label>'
    ).buttonset().appendTo(right_footer);

    var load_button = $('<button id="freebase-loading-load"></button>').text("Sign In").appendTo(right_footer);
    
    this._elmts = DOM.bind(frame);
    
    var provider = "www.freebase.com";

    var check_authorization = function() {
        $.get("/command/check-authorization/" + provider, function(data) {
            if ("status" in data && data.status == "200 OK") {
                authorization.html('Signed in as: <a target="_new" href="http://www.freebase.com/view/user/' + data.username + '">' + data.username + '</a> | <a href="javascript:{}" bind="signout">Sign Out</a>').show();
                DOM.bind(authorization).signout.click(function() {
                    Sign.signout(check_authorization,provider);
                });
                load_button.text("Load").unbind().click(function() {
                    self._load(self);
                });
            } else {
                authorization.html("").hide();
                load_button.text("Sign In").removeAttr("disabled").unbind().click(function() {
                    Sign.signin(check_authorization,provider);
                });                    
            }
        },"json");
    };
    
    check_authorization();
    
    this._elmts.sandbox.click(function() {
        //check_authorization();
    });
    
    this._elmts.freebase.click(function() {
        if (!confirm("Are you sure your data is clean enough to enter Freebase?")) {
            self._elmts.sandbox.attr("checked","checked");
            self._elmts.freebase.removeAttr("checked");
            selector.find("input").button('refresh');
        }
        //check_authorization();
    });
};

FreebaseLoadingDialog.prototype._load = function(self) {
    $.post("/command/upload-data", 
        {
            project: theProject.id, 
            "graph" : ($("#freebase-loading-graph-selector-freebase").attr("checked")) ? "otg" : "sandbox",
            "info" : $("#freebase-loading-tripleloader-info textarea").val()
        }, 
        function(data) {
            var body = $(".dialog-body");
            if ("status" in data && data.status == "200 OK") {
                body.html('<div class="freebase-loading-tripleloader-message"><h2>Data successfully loaded!</h2>' + data.message + '</div>');
            } else {
                body.html('<div class="freebase-loading-tripleloader-message"><h2>Error loading data!</h2><pre>' + JSON.stringify(data,null,2) + '</pre></div>');
            }
            $("button#freebase-loading-load").text("Close").unbind().click(function() {
                self._dismiss();
            });
            $("button#freebase-loading-cancel").hide();
        },
        "json"
    );
}

FreebaseLoadingDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

