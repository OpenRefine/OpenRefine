/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

function FreebaseLoadingDialog() {
  this._createDialog();
  this._signedin = false;
}

FreebaseLoadingDialog.prototype._createDialog = function() {
  var self = this;
  var dialog = $(DOM.loadHTML("freebase", "scripts/dialogs/freebase-loading-dialog.html"));
  this._elmts = DOM.bind(dialog);
  this._elmts.cancelButton.click(function() { self._dismiss(); });

  var provider = "www.freebase.com";
  var authorization = this._elmts.authorization;
  var loadButton = this._elmts.loadButton;

  var check_authorization = function(cont) {
    $.get("/command/freebase/check-authorization/" + provider, function(data) {
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

        self._signedin = false;
        $("#freebase-loading-source-name").keyup();

        DOM.bind(authorization).signin.click(function() {
          Sign.signin(function() {
            check_authorization(cont);
          },provider);
        });                    
      }
    },"json");
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

    $.post("/command/freebase/mqlwrite/" + provider, 
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
      "/command/freebase/preview-protograph?" + $.param({ project: theProject.id }),
      {
        protograph: JSON.stringify(theProject.overlayModels.freebaseProtograph || {}),
        engine: JSON.stringify(ui.browsingEngine.getJSON())
      },
      function(data) {
        if ("tripleloader" in data) {
          self._elmts.functionalCase.show();
          self._level = DialogSystem.showDialog(dialog);

          self._elmts.functionalTabs.tabs();

          self._elmts.previewContainer.text(data.tripleloader).show();

          self._elmts.source_name.keyup(function() {
            if (self._signedin && $(this).val() != "") {
              loadButton.removeAttr("disabled").removeClass("button-disabled");
            } else {
              loadButton.attr("disabled","disabled").addClass("button-disabled");
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

          $.getJSON(
            "/command/core/get-preference?" + $.param({ project: theProject.id, name: "freebase.load.jobName" }),
            null,
            function(data) {
              self._elmts.source_name[0].value = (data.value) ? data.value : theProject.metadata.name;
            }
          );

          if (typeof cont == "function") cont();
        } else {
          self._elmts.unalignedCase.show();
          self._level = DialogSystem.showDialog(dialog);

          self._elmts.alignButton.click(function() {
            self._dismiss();
            FreebaseExtension.handlers.editSchemaAlignment(false);
          });
          self._end();
        }
      },
      "json"
    );
  };

  show_triples(check_authorization);
};

FreebaseLoadingDialog.prototype._load = function() {
  var self = this;
  var qa = self._elmts.qaCheckbox.is(':checked');

  var get_refinery_url = function(url) {
    return "http://refinery.freebaseapps.com/load/" + url.split("/").slice(-1)[0];
  };

  var doLoad = function() {
    var dismissBusy = DialogSystem.showBusy();

    $.post(
      "/command/freebase/upload-data",
      {
        "project" : theProject.id, 
        "qa" : qa,
        "engine" : JSON.stringify(ui.browsingEngine.getJSON()),
        "source_name" : self._elmts.source_name.val(),
        "source_id" : self._elmts.source_id.val()
      }, 
      function(data) {
        dismissBusy();

        var body = self._elmts.dialogBody;
        if ("status" in data && typeof data.status == "object" && "code" in data.status && data.status.code == 200) {
          self._elmts.tripleCountSpan.text(data.result.added);
          self._elmts.refineryLink.attr("href", get_refinery_url(data.result.status_url));
          self._elmts.functionalCase.hide();
          self._elmts.loadedCase.show();
          self._end();
        } else {
          self._show_error("Error loading data",data);
        }
      },
      "json"
    );
  };

  if (qa) {
    var dialog = $(DOM.loadHTML("freebase", "scripts/dialogs/confirm-qa-dialog.html"));
    var elmts = DOM.bind(dialog);
    var level = DialogSystem.showDialog(dialog);
    var dismiss = function() {
      DialogSystem.dismissUntil(level - 1);
    };

    elmts.okButton.click(function() {
      doLoad();
      dismiss();
    });
    elmts.cancelButton.click(function() {
      dismiss();
    });
  } else {
    doLoad();
  }
};

FreebaseLoadingDialog.prototype._dismiss = function() {
  DialogSystem.dismissUntil(this._level - 1);
};

FreebaseLoadingDialog.prototype._show_error = function(msg, error) {
  this._elmts.dialogBody.children().hide();
  this._elmts.errorCase.show();
  this._elmts.errorMessage.text(msg);
  this._elmts.errorDetails.html(
    (('message' in error) ? '<p>' + error.message + '</p>' : '<pre>' + JSON.stringify(error, null, 2) + '</pre>') +
    (('stack' in error) ? '<pre>' + error.stack.replace(/\\n/g,'\n').replace(/\\t/g,'\t') + '</pre>' : "")
  );
  this._end();
  console.log(error);
};

FreebaseLoadingDialog.prototype._end = function() {
  var self = this;
  this._elmts.loadButton.text("Close").removeAttr("disabled").removeClass("button-disabled").unbind().click(function() {
    self._dismiss();
  });
  this._elmts.cancelButton.hide();
  this._elmts.authorization.hide();
};