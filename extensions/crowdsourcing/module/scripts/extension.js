var ZemantaExtension = {handlers: {}, util: {}};


ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings = function(getApiKey) {
	$.post(
		      "/command/core/get-all-preferences",
		      {},
		      function (data) {
		    	if (data && data["crowdflower.apikey"]) {
		    		getApiKey(data["crowdflower.apikey"]);
		    	}
		    	else {
		    		alert("CrowdFlower API key was not found in the settings. Please add it first.");
		    		getApiKey("");
		    	}
		      },
		      "json"
	 );	
};

ZemantaExtension.handlers.storeCrowdFlowerAPIKey = function() {
	
	new ZemantaSettingsDialog(function(newApiKey) {
		$.post(
	          "/command/core/set-preference",
	          {
	            name : "crowdflower.apikey",
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
	window.location = "/preferences";
};

ZemantaExtension.handlers.openJobSettingsDialog = function()  {
	
	new ZemantaCrowdFlowerDialog(function(data) {
	      $.post(
	    		  "command/crowdsourcing/create-crowdflower-job",
	    		  self._jobData,
	    		  function(o)
	    		  {
	    			  if (o.code == "error") {
			              alert(o.message);
			            }
	    			  else {
	    				  alert("Everything is ok!");
	    				  //TODO: store job id to project
	    				  //TODO: store unit id to each row! alternativa: download whole data, replace old one
	    			  }
	    		  },
	    		  "json"
	      );     

		
		
	});

};

ZemantaExtension.handlers.getApiKey =  function() {
	console.log("Getting API key...");
	ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings(function(apiKey) {
		console.log("Read API key: " + apiKey);
		return apiKey;
	});
};


ZemantaExtension.handlers.createEmptyJobDialog = function() {
	console.log("Creating new empty CrowdFlower job");
	
	//TODO:maybe jobData is not even needed
	new ZemantaCrowdFlowerEmptyJobDialog();
};


ExtensionBar.addExtensionMenu({
	"id": "zemanta",
	"label": "Zemanta",
	"submenu": [
   		 {
			 "id": "zemanta/openrefine-settings",
			 "label": "OpenRefine settings",
			 click: ZemantaExtension.handlers.openPreferences
		 },
		 {
			 "id" : "zemanta/crowdflower",
			 "label" : "CrowdFlower service",
			 "submenu" : [
			              {
			            	  "id":"zemanta/crowdflower/test",
			            	  "label": "Test",
			            	  click: ZemantaExtension.handlers.getApiKey
			              },
			     		 {
			    			 "id": "zemanta/crowdflower/settings",
			    			 "label": "Set CrowdFlower API key",
			    			 click: ZemantaExtension.handlers.storeCrowdFlowerAPIKey
			    		 },
			    		 {},
			    		 {
			    			 "id": "zemanta/crowdflower/create-empty-job",
			    			 label: "New empty job",
			    			 click: ZemantaExtension.handlers.createEmptyJobDialog
			    		 },
			    		 {
			    			 "id": "zemanta/crowdflower/create-job",
			    			 label: "New job from columns",
			    			 click: ZemantaExtension.handlers.openJobSettingsDialog
			    		 }
			              ]
		 },
		 {},
		 {
			 "id": "zemanta/az-mechturk-settings",
			 label: "Amazon Mechanical Turk settings",
			 click: ZemantaExtension.handlers.doNothing
		 }
		 
		]
});