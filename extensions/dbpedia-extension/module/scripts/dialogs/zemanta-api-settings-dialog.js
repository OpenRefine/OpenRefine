function ZemantaSettingsDialog(onDone) {
  this._onDone = onDone;
  this._apiKey = "";

  var self = this;
  this._dialog = $(DOM.loadHTML("dbpedia-extension", "scripts/dialogs/zemanta-api-settings-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Enter your Zemanta API key");

  this._elmts.okButton.click(function() {
   
	var apiKey = self._elmts.apiKeyInput.val();
	  
	if (apiKey === "") {
      alert("Please enter your API key!");
    } else {
      self._apiKey = apiKey;	
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._apiKey);
    }
  });
  this._elmts.cancelButton.click(function() {
	    DialogSystem.dismissUntil(self._level - 1);
	  });
  
  this._level = DialogSystem.showDialog(this._dialog);

}