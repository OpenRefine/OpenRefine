var PerformEditsDialog = {};

PerformEditsDialog.launch = function(logged_in_username) {
  var self = this;
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/perform-edits-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  elmts.loggedInUsername.text(logged_in_username);
  
  frame.find('.cancel-button').click(function() {
     dismiss();
  });

  elmts.performEditsButton.click(function() {
    Refine.postProcess(
       "wikidata",
       "perform-wikibase-edits",
       {},
       elmts.performEditsForm.serialize(),
       {},
       { onDone:
          function() {
           dismiss();
          }
       });
  });
};

PerformEditsDialog.checkAndLaunch = function () {
   ManageAccountDialog.ensureLoggedIn(function(logged_in_username) {
       if (logged_in_username) {
          PerformEditsDialog.launch(logged_in_username);
       }
   });
};
