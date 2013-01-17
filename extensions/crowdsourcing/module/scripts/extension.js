var ZemantaCrowdSourcingExtension = {handlers: {}, util: {}};


ZemantaCrowdSourcingExtension.handlers.storeCrowdFlowerSettings = function() {
	
	new ZemantaSettingsDialog(function(newSettings) {
		$.post(
	          "/command/core/set-preference",
	          {
	            name : "crowdflower.apikey",
	            value : JSON.stringify(newSettings.apiKey)
	          },
	          function(o) {
	            if (o.code == "error") {
	            	
	            	alert(o.message);
	            }
	          },
	          "json"
		);
		$.post(
		          "/command/core/set-preference",
		          {
		            name : "crowdflower.defaultTimeout",
		            value : JSON.stringify(newSettings.defaultTimeout)
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


ZemantaCrowdSourcingExtension.handlers.doNothing = function() {
	alert("Crowdsourcing extension active...");
};


ZemantaCrowdSourcingExtension.handlers.openJobSettingsDialog = function()  {
	
	new ZemantaCrowdFlowerDialog(function(extension) {
		
	      $.post(
	    		  "command/crowdsourcing/create-crowdflower-job",
	    		  { "project" : theProject.id, 
	    			"extension": JSON.stringify(extension),
	    			"engine" : JSON.stringify(ui.browsingEngine.getJSON())
	    		  },
	    		  function(o)
	    		  {
	    			  console.log("Status: " + o.status); 
	    			  alert("Status: " + o.status);
	    		  },
	    		  "json"
	      );     

	});
};

ZemantaCrowdSourcingExtension.handlers.evaluateFreebaseReconDialog = function()  {
	
	new ZemantaCFEvaluateFreebaseReconDialog(function(extension) {
		
	      $.post(
	    		  "command/crowdsourcing/evaluate-freebase-recon-job",
	    		  { "project" : theProject.id, 
	    			"extension": JSON.stringify(extension),
	    			"engine" : JSON.stringify(ui.browsingEngine.getJSON())
	    		  },
	    		  function(o)
	    		  {
	    			  console.log("Status: " + o.status); 
	    			  alert("Status: " + o.status);
	    		  },
	    		  "json"
	      );     

	});
};



ZemantaCrowdSourcingExtension.handlers.getApiKey =  function() {
	console.log("Getting API key...");
	ZemantaCrowdSourcingExtension.util.loadCrowdFlowerApiKeyFromSettings(function(apiKey) {
		console.log("Read API key: " + apiKey);
		return apiKey;
	});
};


ExtensionBar.addExtensionMenu({
	"id": "crowdsourcing-ext",
	"label": "Crowdsourcing",
	"submenu": [
				    		 {
				    			 "id": "crowdsourcing-ext/create-crowdflower-job",
				    			 label: "Create new job / upload data",
				    			 click: ZemantaCrowdSourcingExtension.handlers.openJobSettingsDialog
				    		 },
				    		 {},
				    		 {
				    			 "id": "crowdsourcing-ext/templates",
				    			 label: "Job templates",
				    			 "submenu": [
				    			             {
				    			            	 "id":"crowdsourcing-ext/templates/freebase",
				    			            	 "label": "Evaluate Freebase reconciliations",
				    			            	 click: ZemantaCrowdSourcingExtension.handlers.evaluateFreebaseReconDialog
				    			             },
				    			             {
				    			            	 "id":"crowdsourcing-ext/templates/dbpedia",
				    			            	 "label": "Evaluate DBpedia reconciliations",
				    			            	 click: ZemantaCrowdSourcingExtension.handlers.doNothing
				    			             }
				    			             ]
				    		 },
				    		 {},
				     		 {
				    			 "id": "crowdsourcing-ext/settings",
				    			 "label": "CrowdFlower settings",
				    			  click: ZemantaCrowdSourcingExtension.handlers.storeCrowdFlowerSettings
				    		 }
			              ]
		 });

