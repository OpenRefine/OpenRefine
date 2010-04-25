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
             '<td bind="left" style="text-align: left" width="40%" nowrap="true"></td>' + 
             '<td bind="center" style="text-align: center" width="20%" nowrap="true"></td>' +
             '<td bind="right" style="text-align: right" width="40%" nowrap="true"></td>' +
           '</tr></table>' +
        '</div>'
    ).appendTo(frame);
            
    this._elmts = DOM.bind(frame);
    
    var left_footer = this._elmts.left;  
    var center_footer = this._elmts.center;  
    var right_footer = this._elmts.right;  
    
    var cancel_button = $('<button bind="cancel" id="freebase-loading-cancel"></button>').text("Cancel").click(function() { 
        self._dismiss(); 
    }).appendTo(left_footer);
        
    var authorization = $('<div bind="authorization">').addClass("freebase-loading-authorization").hide().appendTo(center_footer);

    var selector = $('<span bind="selector">').addClass("freebase-loading-graph-selector").html("Load this data into " +
        '<input type="radio" bind="sandbox" id="freebase-loading-graph-selector-sandbox" name="graph-selector" checked="checked" value="sandbox"/><label class="sandbox" for="freebase-loading-graph-selector-sandbox" title="Load into the sandbox">sandbox</label>' +
        '<input type="radio" bind="freebase" id="freebase-loading-graph-selector-freebase" name="graph-selector" value="freebase"/><label class="freebase" for="freebase-loading-graph-selector-freebase" title="Load into Freebase">freebase</label>'
    ).buttonset().appendTo(right_footer);

    var load_button = $('<button bind="load" id="freebase-loading-load"></button>').text("Load").appendTo(right_footer);
    
    var provider = "www.freebase.com";

    var check_authorization = function(autoload) {
        $.get("/command/check-authorization/" + provider, function(data) {
            if ("status" in data && data.status == "200 OK") {
                authorization.html('Signed in as: <a target="_new" href="http://www.freebase.com/view/user/' + data.username + '">' + data.username + '</a> | <a href="javascript:{}" bind="signout">Sign Out</a>').show();
                DOM.bind(authorization).signout.click(function() {
                    Sign.signout(check_authorization,provider);
                });
                if (autoload) {
                    self._load();
                } else {
                    load_button.unbind().click(function() {
                        self._load();
                    });
                }
            } else {
                authorization.html("").hide();
                load_button.unbind().click(function() {
                    Sign.signin(function() {
                        check_authorization(true);
                    },provider);
                });                    
            }
        },"json");
    };
    
    $.post(
        "/command/export-rows", 
        { 
            project: theProject.id, 
            format : "tripleloader" 
        },
        function(data) {
            if (data == null || data == "") {
                body.html(
                    '<div class="freebase-loading-tripleloader-message">'+
                        '<h2>This dataset has no triples</h2>' +
                        '<p>Have you aligned it with the Freebase schemas yet?</p>' +
                    '</div>'
                );
                left_footer.hide();
                center_footer.hide();
                selector.hide();
                load_button.text("Close").unbind().click(function() {
                    self._dismiss();
                });
            } else {
                body.html(
                    '<div class="freebase-loading-tripleloader-data">' + data + '</div>' +
                    '<div class="freebase-loading-tripleloader-info">' +
                      '<div>Describe the data you\'re about to load &not;</div>' +
                      '<textarea bind="info"></textarea>' +
                    '</div>'
                );
                self._elmts = DOM.bind(frame);
                check_authorization(false);
            }
            self._level = DialogSystem.showDialog(frame);
        }
    );
};

FreebaseLoadingDialog.prototype._load = function() {
    var self = this;
    var freebase = self._elmts.freebase.attr("checked");

    var doLoad = function() {
        $.post("/command/upload-data", 
            {
                project: theProject.id, 
                "graph" : (freebase) ? "otg" : "sandbox",
                "info" : self._elmts.info.val()
            }, 
            function(data) {
                var body = $(".dialog-body");
                if ("status" in data && data.status == "200 OK") {
                    body.html(
                        '<div class="freebase-loading-tripleloader-message">' +
                            '<h2>Data successfully loaded</h2>' + 
                            '<p>' + data.message + '</p>' +
                        '</div>'
                    );
                } else {
                    body.html(
                        '<div class="freebase-loading-tripleloader-message">' +
                            '<h2>Error loading data</h2>' + 
                            '<p>' + data.message + '</p>' +
                            '<pre>' + data.stack.replace(/\\n/g,'\n').replace(/\\t/g,'\t') + '</p>' +
                        '</div>'
                    );
                }
                self._elmts.load.text("Close").unbind().click(function() {
                    self._dismiss();
                });
                self._elmts.cancel.hide();
                self._elmts.authorization.hide();
                self._elmts.selector.hide();
            },
            "json"
        );
    };
        
    if (freebase) {
        var dialog = $(
            '<div id="freebase-confirmation-dialog" title="Are you sure?">' +
                '<table><tr><td width="30%"><img src="/images/cop.png" width="140px"></td><td width="70%" style="text-align: center; vertical-align: middle; font-size: 120%">Are you sure this data is ready to be uploaded into <a href="http://www.freebase.com/" target="_new">Freebase</a>?</td></tr></table>' +
            '</div>'
        ).dialog({
            resizable: false,
            width: 400,
            height: 230,
            modal: true,
            buttons: {
                'yes': function() {
                    $(this).dialog('close');
                    doLoad();
                },
                'cancel': function() {
                    $(this).dialog('close');
                }
            }
        });
    } else {
        doLoad();
    }
}

FreebaseLoadingDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

