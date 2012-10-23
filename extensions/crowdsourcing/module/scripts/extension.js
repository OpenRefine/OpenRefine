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
	console.log("Open dialog with columns...");
	
	new ZemantaCrowdFlowerDialog(function(job_columns) {
		console.log("test 123");
		alert(job_columns);
	});

};

ZemantaExtension.handlers.createEmptyJobDialog = function() {
	console.log("Creating new empty CrowdFlower job");
	
	new ZemantaCrowdFlowerEmptyJobDialog(function(jobData){
		console.log("Tukaj ustvari request za jobData: " + jobData);
		console.log("Is title? " + jobData.title);
		console.log("Are instructions? " + jobData.instructions);
		
		//TODO: remove hardcoded API key
		var apiKey = "4d7e7346df7aecae92259843ca7f7bbad14bdbe2";
		
		var CF_SERVICE_URL = "https://api.crowdflower.com/v1/";
		var full_url = CF_SERVICE_URL + "jobs.json?";
		
		var params = {
				"key":apiKey,
				"job[title]": jobData.title,
				"job[instructions]": jobData.instructions
			};
		
		var safe_params = "";
		
		$.each(params, function(key, val){
			if (key != "key"){
				safe_params += "&";
			}
			safe_params += key + "=" + encodeURI(val);
		});
		
		console.log("Safe params: " + safe_params);
		console.log("complete url: " + full_url + safe_params);
		
		$.ajax({
			url: full_url + safe_params,
			type: "POST",
			crossDomain:true,
			dataType: 'json',
			success: function(data) {
				console.log("Response or sth: " + data);
			}
		});
				
	});
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
			    			 "id": "zemanta/crowdflower/settings",
			    			 "label": "CrowdFlower settings",
			    			 click: ZemantaExtension.handlers.storeCrowdFlowerAPIKey
			    		 },
			    		 {},
			    		 {
			    			 "id": "zemanta/crowdflower/create-empty-job",
			    			 label: "Create empty job",
			    			 click: ZemantaExtension.handlers.createEmptyJobDialog
			    		 },
			    		 {
			    			 "id": "zemanta/crowdflower/create-job",
			    			 label: "Create job from columns",
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