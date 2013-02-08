ZemantaDBpediaExtension.util.loadSetting = function(settingName, onDone) {
	$.post(
		      "/command/core/get-all-preferences",
		      {},
		      function (data) {
		    	if (data != null && data[settingName] != null) {
		    		onDone(data[settingName]);
		    	}
		    	else {
		    		console.log("Setting " + settingName + " was not found in settings");
		    		onDone("");
		    	}
		      },
		      "json"
	 );	
};

ZemantaDBpediaExtension.util.loadZemantaSettings = function (onDone) {
	var self = this;
	self._apiKey = "";
	self._timeout = "";

	console.log("Loading Zemanta settings");
	ZemantaDBpediaExtension.util.loadSetting("zemanta.apikey", 
			function(apk) {
			self._apiKey = apk;
		
			ZemantaDBpediaExtension.util.loadSetting("zemantaService.timeout", 
					function (tmo) {
					self._timeout = tmo;
					onDone(self._apiKey, self._timeout);
					}
			);
	});
	
};



ZemantaDBpediaExtension.util.saveSetting = function(settingName, settingValue) {
	$.post(
          "/command/core/set-preference",
          {
            name : settingName,
            value : JSON.stringify(settingValue)
          },
          function(o) {
            if (o.code == "error") {
              alert(o.message);
            }
          },
          "json"
	);	
};


String.format = function format(string) {
    var args = arguments;
    var pattern = new RegExp("%([0-9]+)", "g");
    return String(string).replace(pattern, function(match, index) {
      if (index == 0 || index >= args.length)
        throw "Invalid index in format string";
      return args[index];
    });
};
