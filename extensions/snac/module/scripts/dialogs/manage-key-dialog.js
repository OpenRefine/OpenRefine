var ManageKeysDialog = {};

ManageKeysDialog.firstLogin = true;

ManageKeysDialog.launch = function(apikey, callback) {
   $.get(
      "command/snac/apikey",
       function(data) {
          ManageKeysDialog.display(apikey, data.apikey, callback);
          //callback(data.username);
   });
};

ManageKeysDialog.display = function(apikey, saved_apikey, callback) {
  var self = this;
  var frame = $(DOM.loadHTML("snac", "scripts/dialogs/manage-key-dialog.html"));
  var elmts = this._elmts = DOM.bind(frame);
  
  ManageKeysDialog.firstLaunch = false;

  this._elmts.dialogHeader.text($.i18n('snac-account/dialog-header'));
  this._elmts.explainKey.html($.i18n('snac-account/explain-key'));
  this._elmts.keyLabel.text($.i18n('snac-account/key-label'));
  this._elmts.keyInput.attr("placeholder", $.i18n('snac-account/key-placeholder'));
  this._elmts.cancelButton.text($.i18n('snac-account/close'));
  this._elmts.loginButton.text($.i18n('snac-account/log-in'));
  let checked = 1;
  var checkedOrNah = document.getElementById("myCheck");

  valueChange = function(item){
      checkedOrNah = document.getElementById("myCheck");
      if (checkedOrNah.checked == true) {
        checked = 1;
        // document.getElementById("myCheck").checked = true;
        // console.log(checked);
      } else if (checkedOrNah.checked == false) {
          if (checked == null) {
              localStorage.removeItem('mycheckedvalue');
          } 
        checked = 0;
      }
  }
  if (apikey != null) {
    elmts.keyInput.val(apikey);
    } else if (saved_apikey != null) {
      console.log(localStorage.getItem("mycheckedvalue"));
      if (localStorage.getItem("mycheckedvalue") == 1) {
        elmts.keyInput.val(saved_apikey);
      } 
      else if (localStorage.getItem("mycheckedvalue") == 0){
        elmts.keyInput.val(null);
      }
    }
  this._level = DialogSystem.showDialog(frame);

  var dismiss = function() {
    DialogSystem.dismissUntil(self._level - 1);
  };

  frame.find('.cancel-button').click(function() {
     dismiss();
    //  console.log(checked);
     callback(null);
    // callback(apikey);
  });

  elmts.loginButton.click(function() {
      frame.hide();
      $.post(
          "command/snac/apikey",
          elmts.apiKeyForm.serialize(),
          function(data) {
              if (data.apikey) {
                dismiss();
                callback(data.apikey);
              } else {
                dismiss();
                callback(null);
              }
          });
          localStorage.setItem('mycheckedvalue', checked);
          console.log(checked);
  });
};
