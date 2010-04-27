function FreebaseLoadingDialog() {
    this._createDialog();
    this._signedin = false;
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
        '<input type="radio" bind="freebase" id="freebase-loading-graph-selector-freebase" name="graph-selector" value="freebase" disabled="disabled"/><label class="freebase" for="freebase-loading-graph-selector-freebase" title="Load into Freebase">freebase</label>'
    ).buttonset().appendTo(right_footer);

    var load_button = $('<button bind="load" id="freebase-loading-load" disabled="disabled"></button>').text("Load").appendTo(right_footer);
    
    var provider = "www.freebase.com";

    var check_authorization = function(cont) {
        $.get("/command/check-authorization/" + provider, function(data) {
            if ("status" in data && data.code == "/api/status/ok") {
                authorization.html('Signed in as: <a target="_new" href="http://www.freebase.com/view/user/' + data.username + '">' + data.username + '</a> | <a href="javascript:{}" bind="signout">Sign Out</a>').show();
                DOM.bind(authorization).signout.click(function() {
                    self._signedin = false;
                    load_button.attr("disabled","disabled");
                    $("#freebase-loading-graph-selector-freebase").attr("disabled","disabled").button("refresh");
                    Sign.signout(check_authorization,provider);
                });
                load_button.unbind().click(function() {
                    self._load();
                });
                self._signedin = true;
                $("#freebase-loading-source-name").keyup();
                if (typeof cont == "function") cont(data);
            } else {
                authorization.html('<a href="javascript:{}" bind="signin">Sign into Freebase</a> to enable loading').show();
                DOM.bind(authorization).signin.click(function() {
                    Sign.signin(function() {
                        check_authorization(cont);
                    },provider);
                });                    
            }
        },"json");
    };
    
    var check_allowed = function(user_id, cont) {
        var mql_query = {
            id : user_id,  
            "!/type/usergroup/member": [{
                "id": "/en/metaweb_staff"
            }]
        };        
        
        $.post("/command/mqlread/" + provider, 
            { "query" : JSON.stringify(mql_query) }, 
            function(data) {
                if ("status" in data && data.code == "/api/status/ok") {
                    if (typeof cont == "function") cont((data.result != null));
                } else {
                    self._show_error("Error checking if user is a staff member", data);
                }
            },
            "json"
        );
    };
    
    var make_topic = function(new_topic_id, topic_type, cont) {
        var mql_query = [{
            "create": "unless_exists",
            "name":   new_topic_id,
            "a:type": topic_type,
            "b:type": "/common/topic"
            "id":     null,
            "guid":   null
        }];

        $.post("/command/mqlwrite/" + provider, 
            { "query" : JSON.stringify(mql_query) }, 
            function(data) {
                if ("status" in data && data.code == "/api/status/ok") {
                    self._elmts.source_id.val(data.result[0].id);
                    if (typeof cont == "function") cont();
                } else {
                    self._show_error("Error creating new topic", data);
                }
            },
            "json"
        );
    };
    
    var show_triples = function(cont) {
        $.post("/command/export-rows", 
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
                    self._elmts = DOM.bind(frame);
                    self._end();
                } else {
                    body.html(
                        '<div class="freebase-loading-tripleloader-info"><table><tr>' +
                          '<td><div>Name this data load &not; <sup style="color: red">required</sup></div>' +
                          '<input type="text" size="40" id="freebase-loading-source-name" bind="source_name"></td>' +
                          '<td><div>Source ID &not; <sup style="color: #888">optional</sup></div>' +
                          '<input type="text" size="60" id="freebase-loading-source-id" bind="source_id"></td>' +
                        '</tr></table></div>' +
                        '<div class="freebase-loading-tripleloader-data">' + data + '</div>'
                    );
                    self._elmts = DOM.bind(frame);
                    
                    self._elmts.source_name.keyup(function() {
                        if (self._signedin && $(this).val() != "") {
                            load_button.removeAttr("disabled");
                        } else {
                            load_button.attr("disabled","disabled");
                        }
                    });
                    
                    self._elmts.source_id.suggest({
                        "type": "/dataworld/information_source",
                        "suggest_new": "Click here to add a new information source"
                    }).bind("fb-select", function(e, data) {
                        self._elmts.source_id.val(data.id);
                    }).bind("fb-select-new", function(e, val) {
                        make_topic(val, "/dataworld/information_source");
                    });

                    if (typeof cont == "function") cont();
                }

                self._level = DialogSystem.showDialog(frame);
            }
        );
    };

    show_triples(function() {
        check_authorization(function(data) {
            check_allowed(data.id, function(is_allowed) {
                if (is_allowed) {
                    $("#freebase-loading-graph-selector-freebase").removeAttr("disabled").button("refresh");
                }
            });
        });
    });
};

FreebaseLoadingDialog.prototype._load = function() {
    var self = this;
    var freebase = self._elmts.freebase.attr("checked");

    var get_peacock_url = function(url) {
      return "http://peacock.freebaseapps.com/stats/data.labs/" + url.split("/").slice(-2).join("/");
    }
    
    var doLoad = function() {
        $.post("/command/upload-data", 
            {
                project: theProject.id, 
                "graph" : (freebase) ? "otg" : "sandbox",
                "source_name" : self._elmts.source_name.val(),
                "source_id" : self._elmts.source_id.val()
            }, 
            function(data) {
                var body = $(".dialog-body");
                if ("status" in data && "code" in data.status && data.status.code == 200) {
                    body.html(
                        '<div class="freebase-loading-tripleloader-message">' +
                            '<h2>' + data.result.added + ' triples successfully scheduled for loading</h2>' + 
                            '<p>Follow the loading progress <a href="' + get_peacock_url(data.result.status_url) + '">here</a></p>' +
                        '</div>'
                    );
                    self._end();
                } else {
                    self._show_error("Error loading data",error);
                }
            },
            "json"
        );
    };
        
    if (freebase) {
        var dialog = $(
            '<div id="freebase-confirmation-dialog" title="Are you sure?">' +
                '<table><tr><td width="30%"><img src="/images/cop.png" width="140px"></td><td width="70%" style="text-align: center; vertical-align: middle; font-size: 120%">Are you sure this data is ready to be uploaded into <b>Freebase</b>?</td></tr></table>' +
            '</div>'
        ).dialog({
            resizable: false,
            width: 400,
            height: 230,
            modal: true,
            buttons: {
                'yes, I know what I\'m doing': function() {
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

FreebaseLoadingDialog.prototype._show_error = function(msg, error) {
    var body = $(".dialog-body");
    body.html(
        '<div class="freebase-loading-tripleloader-message">' +
            '<h2>' + msg + '</h2>' + 
            '<p>' + error.message + '</p>' +
            (('stack' in error) ? '<pre>' + error.stack.replace(/\\n/g,'\n').replace(/\\t/g,'\t') + '</p>' : "") +
        '</div>'
    );
    this._end();
};

FreebaseLoadingDialog.prototype._end = function() {
    var self = this;
    self._elmts.load.text("Close").removeAttr("disabled").unbind().click(function() {
        self._dismiss();
    });
    self._elmts.cancel.hide();
    self._elmts.authorization.hide();
    self._elmts.selector.hide();
};