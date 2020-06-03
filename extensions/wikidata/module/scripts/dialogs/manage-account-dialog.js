var ManageAccountDialog = {};

ManageAccountDialog.firstLogin = true;

ManageAccountDialog.launch = function (logged_in_username, callback) {
  $.post(
      "command/core/get-all-preferences",
      null,
      function (preferences) {
        ManageAccountDialog.display(logged_in_username, preferences.wikidata_credentials, callback);
      },
      "json"
  );
};

ManageAccountDialog.display = function (logged_in_username, saved_credentials, callback) {
  // check if OAuth is supported
  var support_oauth = ManageAccountDialog.supportOAuth();

  var self = this;
  var frame;
  frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/manage-account-dialog.html"));

  var elmts = this._elmts = DOM.bind(frame);

  this._elmts.dialogHeader.text($.i18n('wikidata-account/dialog-header'));
  this._elmts.explainLogIn.html($.i18n('wikidata-account/explain-log-in'));
  if (support_oauth) {
    this._elmts.usernameLabel.hide();
    this._elmts.usernameInput.hide();
    this._elmts.passwordLabel.hide();
    this._elmts.passwordInput.hide();
  } else {
    this._elmts.usernameLabel.text($.i18n('wikidata-account/username-label'));
    this._elmts.usernameInput.attr("placeholder", $.i18n('wikidata-account/username-placeholder'));
    this._elmts.passwordLabel.text($.i18n('wikidata-account/password-label'));
    this._elmts.passwordInput.attr("placeholder", $.i18n('wikidata-account/password-placeholder'));
  }
  this._elmts.rememberCredentialsLabel.text($.i18n('wikidata-account/remember-credentials-label'));
  this._elmts.dialogHeader.text($.i18n('wikidata-account/dialog-header'));
  this._elmts.cancelButton1.text($.i18n('wikidata-account/close'));
  this._elmts.cancelButton2.text($.i18n('wikidata-account/close'));
  this._elmts.loggedInAs.text($.i18n('wikidata-account/logged-in-as'));
  this._elmts.logoutButton.text($.i18n('wikidata-account/log-out'));
  this._elmts.loginButton.val($.i18n('wikidata-account/log-in'));

  if (logged_in_username != null) {
    elmts.loginArea.remove();
  } else {
    elmts.logoutArea.remove();
  }

  this._level = DialogSystem.showDialog(frame);
  if (!support_oauth) {
    this._elmts.usernameInput.focus();
  }

  var dismiss = function () {
    DialogSystem.dismissUntil(self._level - 1);
  };

  elmts.loggedInUsername
      .text(logged_in_username)
      .attr('href', 'https://www.wikidata.org/wiki/User:' + logged_in_username);

  elmts.cancelButton1.click(function (e) {
    dismiss();
    callback(null);
  });
  elmts.cancelButton2.click(function (e) {
    dismiss();
    callback(null);
  });

  elmts.loginForm.submit(function (e) {
        frame.hide();
        if (support_oauth) {
          ManageAccountDialog.oauth(
              "command/wikidata/authorize?remember-credentials=" + elmts.rememberCredentials.val(),
              function () {
                $.get(
                    "command/wikidata/login",
                    function (data) {
                      if (data.logged_in) {
                        dismiss();
                        callback(data.username);
                      } else {
                        frame.show();
                        elmts.invalidCredentials.text("Invalid credentials.");
                      }
                    });
              });
        } else {
          Refine.postCSRF(
              "command/wikidata/login",
              elmts.loginForm.serialize(),
              function (data) {
                if (data.logged_in) {
                  dismiss();
                  callback(data.username);
                } else {
                  frame.show();
                  elmts.invalidCredentials.text("Invalid credentials.");
                }
              });
        }
        e.preventDefault();
      }
  );

  elmts.logoutButton.click(function () {
    Refine.postCSRF(
        "command/wikidata/login",
        "logout=true",
        function (data) {
          if (!data.logged_in) {
            dismiss();
            callback(null);
          }
        });
  });
};

ManageAccountDialog.supportOAuth = function () {
  var support_oauth = false;
  // must sync
  $.ajaxSetup({async: false});
  $.get(
      "command/wikidata/support-oauth",
      function (data) {
        support_oauth = data.support_oauth;
      }
  );
  $.ajaxSetup({async: true});
  return support_oauth;
};

ManageAccountDialog.oauth = function (path, callback) {
  var self = this;
  self.oauthWindow = window.open(path);
  self.oauthInterval = window.setInterval(function () {
    if (self.oauthWindow.closed) {
      window.clearInterval(self.oauthInterval);
      callback();
    }
  }, 1000);
};

ManageAccountDialog.isLoggedIn = function (callback) {
  var discardWaiter = function () {
  };
  if (ManageAccountDialog.firstLogin) {
    discardWaiter = DialogSystem.showBusy($.i18n('wikidata-account/connecting-to-wikidata'));
  }
  $.get(
      "command/wikidata/login",
      function (data) {
        discardWaiter();
        ManageAccountDialog.firstLogin = false;
        callback(data.username);
      });
};

ManageAccountDialog.ensureLoggedIn = function (callback) {
  ManageAccountDialog.isLoggedIn(function (logged_in_username) {
    if (logged_in_username == null) {
      ManageAccountDialog.launch(null, callback);
    } else {
      callback(logged_in_username);
    }
  });
};

ManageAccountDialog.checkAndLaunch = function () {
  ManageAccountDialog.isLoggedIn(function (logged_in_username) {
    ManageAccountDialog.launch(logged_in_username, function (success) {
    });
  });
};
