var ManageAccountDialog = {};
ManageAccountDialog.launch = function(logged_in_username, callback) {
  var self = this;
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/manage-account-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);

  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  if (logged_in_username != null) {
      elmts.loginArea.hide();
  } else {
      elmts.logoutArea.hide();
  }

  elmts.loggedInUsername.text(logged_in_username);
  
  frame.find('.cancel-button').click(function() {
     dismiss();
     callback(null);
  });

  elmts.loginButton.click(function() {
    $.post(
       "command/wikidata/login",
       elmts.loginForm.serialize(),
       function(data) {
         if (data.logged_in) {
           dismiss();
           callback(data.username);
         } else {
            elmts.invalidCredentials.text("Invalid credentials.");
         }
       });
  });

  elmts.logoutButton.click(function() {
    $.post(
       "command/wikidata/login",
       "logout=true",
       function(data) {
         if (!data.logged_in) {
           dismiss();
           callback(null);
         }
    }); 
  });
};

ManageAccountDialog.isLoggedIn = function(callback) {
   $.get(
      "command/wikidata/login",
       function(data) {
          callback(data.username);
   });
}; 

ManageAccountDialog.ensureLoggedIn = function(callback) {
    ManageAccountDialog.isLoggedIn(function(logged_in_username) {
        if (logged_in_username == null) {
            ManageAccountDialog.launch(null, callback);
        } else {
            callback(logged_in_username);
        }
    });
};

ManageAccountDialog.checkAndLaunch = function () {
    ManageAccountDialog.isLoggedIn(function(logged_in_username) {
       ManageAccountDialog.launch(logged_in_username, function(success) { });
   });
};
