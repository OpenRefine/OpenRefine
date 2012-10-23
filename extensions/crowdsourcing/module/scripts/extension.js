var ZemantaExtension = {handlers: {}, util: {}};


ZemantaExtension.util.parseCrowdFlowerApiKey = function (prefs) {
	var apiKey = "";
	if(prefs != null) {
		$.each(prefs, function(key, val) {
			if(key === "crowdflower-api-key") {
				apiKey = val;
			}
		});
	}
	return apiKey;
};

ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings = function(getCrowdFlowerApiKey) {
	$.post(
		      "/command/core/get-all-preferences",
		      {},
		      function (data) {
		    	  getCrowdFlowerApiKey(ZemantaExtension.util.parseCrowdFlowerApiKey(data));
		    	  },
		      "json"
	 );
	
};

ZemantaExtension.handlers.storeCrowdFlowerAPIKey = function() {
	
	new ZemantaSettingsDialog(function(newApiKey) {
		$.post(
	          "/command/core/set-preference",
	          {
	            name : "crowdflower-api-key",
	            value : JSON.stringify(newApiKey)
	          },
	          function(o) {
	            if (o.code == "error") {
	              alert(o.message);
	            }
	          },
	          "json"
		);
	});
};

ZemantaExtension.handlers.doNothing = function() {
	alert("Crowdsourcing extension active...");
};

ZemantaExtension.handlers.openPreferences = function() {
	alert("Open preferences?...");
	window.location = "/preferences";
};

ZemantaExtension.handlers.openJobSettingsDialog = function()  {
	console.log("Open job columns...");
	
	new ZemantaCrowdFlowerDialog(function(job_columns) {
		console.log("test 123");
		alert(job_columns);
	});

};


ExtensionBar.addExtensionMenu({
	"id": "zemanta",
	"label": "Zemanta",
	"submenu": [
   		 {
			 "id": "zemanta/openrefine-settings",
			 label: "OpenRefine settings",
			 click: ZemantaExtension.handlers.openPreferences
		 },
		 {
			 "id": "zemanta/crowdflower-settings",
			 label: "CrowdFlower settings",
			 click: ZemantaExtension.handlers.storeCrowdFlowerAPIKey
		 },
		 {
			 "id": "zemanta/az-mechturk-settings",
			 label: "Amazon Mechanical Turk settings",
			 click: ZemantaExtension.handlers.doNothing
		 },
		 {
			 "id": "zemanta/test",
			 label: "Test CrowdFlower service",
			 click: ZemantaExtension.handlers.openJobSettingsDialog
		 }
		]
});