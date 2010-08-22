function FreebaseLoadingDialog() {
    this._createDialog();
    this._signedin = false;
}

FreebaseLoadingDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("core", "scripts/dialogs/freebase-loading-dialog.html"));
    this._elmts = DOM.bind(dialog);
    this._elmts.cancelButton.click(function() { self._dismiss(); });
    this._elmts.selector.buttonset();

    var provider = "www.freebase.com";
    var authorization = this._elmts.authorization;
    var loadButton = this._elmts.loadButton;
    
    var check_authorization = function(cont) {
        $.get("/command/core/check-authorization/" + provider, function(data) {
            if ("status" in data && data.code == "/api/status/ok") {
                authorization.html('Signed in as: <a target="_new" href="http://www.freebase.com/view/user/' + data.username + '">' + data.username + '</a> | <a href="javascript:{}" bind="signout">Sign Out</a>').show();
                DOM.bind(authorization).signout.click(function() {
                    self._signedin = false;
                    loadButton.attr("disabled","disabled");
                    $("#freebase-loading-graph-selector-freebase").attr("disabled","disabled").button("refresh");
                    Sign.signout(check_authorization,provider);
                });
                loadButton.unbind().click(function() {
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
        $.get("/command/core/user-badges/" + provider, 
            { "user_id" : user_id }, 
            function(data) {
                if ("status" in data && data.code == "/api/status/ok") {
                    var badges = data.result['!/type/usergroup/member'];
                    var allowed = false;
                    for (var i = 0; i < badges.length; i++) {
                        var id = badges[i].id;
                        if (id == "/en/metaweb_staff") {
                            allowed = true;
                            break;
                        }
                    }
                    if (typeof cont == "function") cont(allowed);
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
            "b:type": "/common/topic",
            "id":     null,
            "guid":   null
        }];

        $.post("/command/core/mqlwrite/" + provider, 
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
        $.post(
            "/command/core/preview-protograph?" + $.param({ project: theProject.id }),
            {
                protograph: JSON.stringify(theProject.overlayModels.freebaseProtograph || {}),
                engine: JSON.stringify(ui.browsingEngine.getJSON())
            },
            function(data) {
                var body = self._elmts.dialogBody;
                if ("tripleloader" in data) {
                    body.html(
                        '<div class="freebase-loading-tripleloader-info"><table><tr>' +
                          '<td><div>Name this data load &not; <sup style="color: red">required</sup></div>' +
                          '<input type="text" size="40" id="freebase-loading-source-name" bind="source_name"></td>' +
                          '<td><div>Source ID &not; <sup style="color: #888">optional</sup></div>' +
                          '<input type="text" size="60" id="freebase-loading-source-id" bind="source_id"></td>' +
                        '</tr></table></div>' +
                        '<div class="freebase-loading-tripleloader-data">' + data.tripleloader + '</div>'
                    );
                    self._elmts = DOM.bind(dialog);
                    
                    self._elmts.source_name.keyup(function() {
                        if (self._signedin && $(this).val() != "") {
                            loadButton.removeAttr("disabled");
                        } else {
                            loadButton.attr("disabled","disabled");
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
                } else {
                    body.html(
                        '<div class="freebase-loading-tripleloader-message">'+
                            '<h2>This dataset has no triples</h2>' +
                            '<p>Have you aligned it with the Freebase schemas yet?</p>' +
                        '</div>'
                    );
                    self._elmts = DOM.bind(dialog);
                    self._end();
                }
                self._level = DialogSystem.showDialog(dialog);
            },
            "json"
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
        return "http://gridworks-loads.freebaseapps.com/job/" + url.split("/").slice(-1)[0];
    };
    
    var doLoad = function() {
        var dismissBusy = DialogSystem.showBusy();
        
        $.post("/command/core/upload-data", 
            {
                project: theProject.id, 
                "graph" : (freebase) ? "otg" : "sandbox",
                "engine" : JSON.stringify(ui.browsingEngine.getJSON()),
                "source_name" : self._elmts.source_name.val(),
                "source_id" : self._elmts.source_id.val()
            }, 
            function(data) {
                dismissBusy();
                
                var body = self._elmts.dialogBody;
                if ("status" in data && typeof data.status == "object" && "code" in data.status && data.status.code == 200) {
                    body.html(
                        '<div class="freebase-loading-tripleloader-message">' +
                            '<h2>' + data.result.added + ' triples successfully scheduled for loading</h2>' + 
                            '<h4>Follow the loading progress <a href="' + get_peacock_url(data.result.status_url) + '" target="_new">here</a>.</h4>' +
                        '</div>'
                    );
                    self._end();
                } else {
                    self._show_error("Error loading data",data);
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
};

FreebaseLoadingDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

FreebaseLoadingDialog.prototype._show_error = function(msg, error) {
    var body = self._elmts.dialogBody;
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
    self._elmts.loadButton.text("Close").removeAttr("disabled").unbind().click(function() {
        self._dismiss();
    });
    self._elmts.cancelButton.hide();
    self._elmts.authorization.hide();
    self._elmts.selector.hide();
};