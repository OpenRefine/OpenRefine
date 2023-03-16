var ManageAccountDialog = {};

/**
 * Displays the logged in page if the user is logged in,
 * displays the login page otherwise.
 */
ManageAccountDialog.display = function (logged_in_username, onSuccess) {
  if (logged_in_username == null) {
    ManageAccountDialog.tryLoginWithCookies(onSuccess);
  } else {
    ManageAccountDialog.displayLoggedIn(logged_in_username);
  }
};


ManageAccountDialog.tryLoginWithCookies = function (onSuccess) {
  // Try with cookies first.
  const discardWaiter = DialogSystem.showBusy($.i18n('wikibase-account/connecting-to-wikibase'));
  Refine.postCSRF(
      "command/wikidata/login",
      {"wb-api-endpoint": WikibaseManager.getSelectedWikibaseApi()},
      function (data) {
        discardWaiter();
        if (data.logged_in) {
          onSuccess(data.username);
          ManageAccountDialog.displayLoggedIn(data.username);
        } else {
          // If failed, then login with username/password.
          ManageAccountDialog.displayPasswordLogin(onSuccess);
        }
      });
};

ManageAccountDialog.initCommon = function (elmts) {
  elmts.dialogHeader.text($.i18n('wikibase-account/dialog-header', WikibaseManager.getSelectedWikibaseName()));
  elmts.explainLogIn.html($.i18n('wikibase-account/explain-log-in',
      WikibaseManager.getSelectedWikibaseMainPage(), WikibaseManager.getSelectedWikibaseName()));
  elmts.cancelButton.text($.i18n('wikibase-account/close'));
};

ManageAccountDialog.displayLoggedIn = function (logged_in_username) {
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/logged-in-dialog.html"));
  var elmts = DOM.bind(frame);
  ManageAccountDialog.initCommon(elmts);
  elmts.loggedInAs.text($.i18n('wikibase-account/logged-in-as'));
  elmts.logoutButton.text($.i18n('wikibase-account/log-out'));

  var level = DialogSystem.showDialog(frame);
  var dismiss = function () {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.loggedInUsername
      .text(logged_in_username)
      .attr('href', WikibaseManager.getSelectedWikibaseRoot() + 'User:' + logged_in_username);

  elmts.cancelButton.on('click',function (e) {
    dismiss();
  });

  elmts.logoutButton.on('click',function () {
    frame.hide();
    Refine.postCSRF(
        "command/wikidata/login",
        $.param({
          "logout": "true",
          "wb-api-endpoint": WikibaseManager.getSelectedWikibaseApi()
        }),
        function (data) {
          frame.show();
          if (!data.logged_in) {
            dismiss();
          }
        });
  });
};

ManageAccountDialog.displayPasswordLogin = function (onSuccess) {
  const frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/password-login-dialog.html"));
  const elmts = DOM.bind(frame);
  ManageAccountDialog.initCommon(elmts);
  WikibaseManager.getSelectedWikibaseLogoURL(function(data) {
	elmts.wikibaseLogoImg.attr("src", data);
  });
  elmts.wikibaseMainPage.attr("href", WikibaseManager.getSelectedWikibaseMainPage());
  elmts.wikibaseLogoImg.attr("alt", $.i18n('wikibase-account/logo-alt-text', WikibaseManager.getSelectedWikibaseName()));
  elmts.explainBotPasswords.html($.i18n('wikibase-account/explain-bot-passwords', WikibaseManager.getSelectedWikibaseRoot() + 'Special:BotPasswords'));
  elmts.explainOwnerOnlyConsumerLogin.html($.i18n('wikibase-account/explain-owner-only-consumer-login'));
  elmts.invalidCredentials.text($.i18n('wikibase-account/invalid-credentials'));
  elmts.invalidCredentials.hide();
  elmts.usernameLabel.text($.i18n('wikibase-account/username-label'));
  elmts.usernameInput.attr("placeholder", $.i18n('wikibase-account/username-placeholder'));
  elmts.passwordLabel.text($.i18n('wikibase-account/password-label'));
  elmts.passwordInput.attr("placeholder", $.i18n('wikibase-account/password-placeholder'));
  elmts.rememberMe.text($.i18n('wikibase-account/remember-me'));
  elmts.passwordRememberMeTitle.attr("title", $.i18n('wikibase-account/password-remember-me-title'));
  elmts.loginButton.text($.i18n('wikibase-account/log-in'));
  elmts.usernameInput.trigger('focus');

  // We don't support logging in with owner-only consumer if the target Wikibase doesn't support OAuth.
  if (!WikibaseManager.getSelectedWikibaseOAuth()) {
    elmts.explainOwnerOnlyConsumerLogin.hide();
  }

  var level = DialogSystem.showDialog(frame);
  var dismiss = function () {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.cancelButton.on('click',function (e) {
    dismiss();
  });

  elmts.explainOwnerOnlyConsumerLogin.on('click',function (e) {
    dismiss();
    ManageAccountDialog.displayOwnerOnlyConsumerLogin(onSuccess);
  });

  elmts.loginForm.on('submit',function (e) {
    frame.hide();
    let formArr = elmts.loginForm.serializeArray();
    let username = elmts.usernameInput.val();
    formArr.push({name: "wb-api-endpoint", value: WikibaseManager.getSelectedWikibaseApi()});
    Refine.postCSRF(
        "command/wikidata/login",
        $.param(formArr),
        function (data) {
          if (data.logged_in) {
            dismiss();
            onSuccess(data.username);
          } else {
            frame.show();
            elmts.invalidCredentials.show();
          }
        });
    e.preventDefault();
  });
};

ManageAccountDialog.displayOwnerOnlyConsumerLogin = function (onSuccess) {
  var frame = $(DOM.loadHTML("wikidata", "scripts/dialogs/owner-only-consumer-login-dialog.html"));
  var elmts = DOM.bind(frame);
  ManageAccountDialog.initCommon(elmts);
  elmts.explainOwnerOnlyConsumerWiki.html($.i18n('wikibase-account/explain-owner-only-consumer-wiki',
      WikibaseManager.getSelectedWikibaseOAuth().registration_page));
  elmts.explainPasswordLogin.html($.i18n('wikibase-account/explain-password-login'));
  elmts.invalidCredentials.text($.i18n('wikibase-account/invalid-credentials'));
  elmts.invalidCredentials.hide();
  elmts.consumerTokenLabel.text($.i18n('wikibase-account/consumer-token-label'));
  elmts.consumerTokenInput.attr("placeholder", $.i18n('wikibase-account/consumer-token-placeholder'));
  elmts.consumerSecretLabel.text($.i18n('wikibase-account/consumer-secret-label'));
  elmts.consumerSecretInput.attr("placeholder", $.i18n('wikibase-account/consumer-secret-placeholder'));
  elmts.accessTokenLabel.text($.i18n('wikibase-account/access-token-label'));
  elmts.accessTokenInput.attr("placeholder", $.i18n('wikibase-account/access-token-placeholder'));
  elmts.accessSecretLabel.text($.i18n('wikibase-account/access-secret-label'));
  elmts.accessSecretInput.attr("placeholder", $.i18n('wikibase-account/access-secret-placeholder'));
  elmts.rememberMe.text($.i18n('wikibase-account/remember-me'));
  elmts.ownerOnlyConsumerRememberMeTitle.attr("title", $.i18n('wikibase-account/owner-only-consumer-remember-me-title'));
  elmts.loginButton.text($.i18n('wikibase-account/log-in'));
  elmts.consumerTokenInput.trigger('focus');

  var level = DialogSystem.showDialog(frame);
  var dismiss = function () {
    DialogSystem.dismissUntil(level - 1);
  };

  elmts.cancelButton.on('click',function (e) {
    dismiss();
  });

  elmts.explainPasswordLogin.on('click',function (e) {
    dismiss();
    ManageAccountDialog.displayPasswordLogin(onSuccess);
  });

  elmts.loginForm.on('submit',function (e) {
    frame.hide();
    let formArr = elmts.loginForm.serializeArray();
    formArr.push({name: "wb-api-endpoint", value: WikibaseManager.getSelectedWikibaseApi()});
    Refine.postCSRF(
        "command/wikidata/login",
        $.param(formArr),
        function (data) {
          if (data.logged_in) {
            dismiss();
            onSuccess(data.username);
          } else {
            frame.show();
            elmts.invalidCredentials.show();
          }
        });
    e.preventDefault();
  });
};

/**
 * Checks if the user is logged in or not.
 *
 * The callback needs to react to both cases.
 */
ManageAccountDialog.isLoggedIn = function (callback) {
  $.get(
      "command/wikidata/login",
      {"wb-api-endpoint": WikibaseManager.getSelectedWikibaseApi()},
      function (data) {
        if (data.logged_in) {
          // make sure that the user is logged in to the selected Wikibase
          callback(data.username)
        } else {
          callback(null);
        }
      });
};

/**
 * The onSuccess callback is called if and only if the user is logged in.
 */
ManageAccountDialog.ensureLoggedIn = function (onSuccess) {
  ManageAccountDialog.isLoggedIn(function (logged_in_username) {
    if (logged_in_username == null) {
      ManageAccountDialog.display(null, onSuccess);
    } else {
      onSuccess(logged_in_username);
    }
  });
};

ManageAccountDialog.checkAndLaunch = function () {
  ManageAccountDialog.isLoggedIn(function (logged_in_username) {
    ManageAccountDialog.display(logged_in_username, function (success) {
    });
  });
};
