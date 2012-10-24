var ZemantaExtension = {handlers: {}, util: {}};


ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings = function(getApiKey) {
	$.post(
		      "/command/core/get-all-preferences",
		      {},
		      function (data) {
		    	if (data && data["crowdflower.apikey"]) {
		    		getApiKey(data["crowdflower.apikey"]);
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
	console.log("Open dialog with columns...");
	
	new ZemantaCrowdFlowerDialog(function(job_columns) {
		console.log("test 123");
		alert(job_columns);
	});

};

ZemantaExtension.handlers.createEmptyJobDialog = function() {
	console.log("Creating new empty CrowdFlower job");
	
	//TODO:maybe jobData is not even needed
	new ZemantaCrowdFlowerEmptyJobDialog(function(jobData){
		
//		var apiKey= "";
//		ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings(function(myApiKey) {
//			apiKey = myApiKey;
//		});
//		
//		var CF_SERVICE_URL = "https://api.crowdflower.com/v1/";
//		var full_url = CF_SERVICE_URL + "jobs.json?";
//		
//		var params = {
//				"key":apiKey,
//				"job[title]": jobData.title,
//				"job[instructions]": jobData.instructions
//			};
//		
//		var safe_params = "";
//		
//		$.each(params, function(key, val){
//			if (key != "key"){
//				safe_params += "&";
//			}
//			safe_params += key + "=" + encodeURI(val);
//		});
//		
//		console.log("Safe params: " + safe_params);
//		console.log("complete url: " + full_url + safe_params);
//		
//		$.ajax({
//			url: full_url + safe_params,
//			type: "POST",
//			crossDomain:true,
//			dataType: 'json',
//			success: function(data) {
//				console.log("Response or sth: " + data);
//			}
//		});
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
			            	  "id":"zemanta/crowdflower/test",
			            	  "label": "Test",
			            	  click: ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings(function(apiKey) {
			            		  console.log("Test API KEY: " + apiKey);
			            	  })
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