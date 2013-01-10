function ZemantaSettingsDialog(onDone) {
  this._onDone = onDone;
  this._settings = {};
  this._settings.apiKey = "";
  this._settings.defaultTimeout = "1500";

  var self = this;
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-api-settings-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Enter your CrowdFlower API key");

  this._elmts.okButton.click(function() {
   
	var apiKey = self._elmts.apiKeyInput.val();
	var defaultTimeout = self._elmts.defaultTimeoutInput.val();
	  
	if (apiKey === "") {
      alert("Please enter your API key!");
    } else {
      self._settings.apiKey = apiKey;
      
      if(defaultTimeout != "") {
      	self._settings.defaultTimeout = defaultTimeout;
      }
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._settings);
    }
  });
  this._elmts.cancelButton.click(function() {
	    DialogSystem.dismissUntil(self._level - 1);
	  });
  
  this._level = DialogSystem.showDialog(this._dialog);

}