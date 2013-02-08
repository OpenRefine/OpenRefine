function ZemantaDBpediaSettingsDialog(onDone) {
	this._onDone = onDone;
	this._apiKey = "";

	var self = this;
	self._dialog = $(DOM.loadHTML("dbpedia-extension", "scripts/dialogs/zemanta-api-settings-dialog.html"));
	self._elmts = DOM.bind(this._dialog);
	self._elmts.dialogHeader.text("DBpedia extension settings");

	//get api key from settings, if it exist

	ZemantaDBpediaExtension.util.loadSetting("zemanta.apikey",function (data) {
		self._elmts.apiKeyInput.val(data);
	});

	ZemantaDBpediaExtension.util.loadSetting("zemantaService.timeout",function (data) {
		self._elmts.zemTimeout.val(data);
	});

	ZemantaDBpediaExtension.util.loadSetting("dbpediaService.timeout",function (data) {
		self._elmts.dbpediaTimeout.val(data);
	});


	self._elmts.okButton.click(function() {

		var apiKey = self._elmts.apiKeyInput.val();
		var dbpediaTimeout = self._elmts.dbpediaTimeout.val();
		var zemantaTimeout = self._elmts.zemTimeout.val(); 

		if (apiKey === "") {
			alert("Please enter your API key!");
			return;
		}

		DialogSystem.dismissUntil(self._level - 1);
		onDone(apiKey, dbpediaTimeout, zemantaTimeout);

	});

	this._elmts.cancelButton.click(function() {
		DialogSystem.dismissUntil(self._level - 1);
	});

	this._level = DialogSystem.showDialog(this._dialog);

}