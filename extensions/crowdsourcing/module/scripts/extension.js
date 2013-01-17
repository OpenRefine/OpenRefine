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





ExtensionBar.appendTo(["zemanta"],[
   		 {
			 "id" : "zemanta/crowdflower",
			 "label" : "CrowdFlower",
			 "submenu" : [
				    		 {
				    			 "id": "zemanta/crowdflower/create-crowdflower-job",
				    			 label: "Create new job / upload data",
				    			 click: ZemantaCrowdSourcingExtension.handlers.openJobSettingsDialog
				    		 },
				    		 {
				    			"id": "zemanta/crowdflower/download-results",
				    			"label": "Download results",
				    			click: ZemantaCrowdSourcingExtension.handlers.doNothing
				    			 
				    		 },
				    		 {},
				    		 {
				    			 "id": "zemanta/crowdflower/templates",
				    			 label: "Templates",
				    			 "submenu": [
				    			             {
				    			            	 "id":"zemanta/crowdflower/templates/freebase",
				    			            	 "label": "Evaluate Freebase reconciliations",
				    			            	 click: ZemantaCrowdSourcingExtension.handlers.evaluateFreebaseReconDialog
				    			             },
				    			             {
				    			            	 "id":"zemanta/crowdflower/templates/dbpedia",
				    			            	 "label": "Evaluate DBpedia reconciliations",
				    			            	 click: ZemantaCrowdSourcingExtension.handlers.doNothing
				    			             }
				    			             ]
				    		 },
				    		 {},
				     		 {
				    			 "id": "zemanta/crowdflower/settings",
				    			 "label": "CrowdFlower settings",
				    			  click: ZemantaCrowdSourcingExtension.handlers.storeCrowdFlowerSettings
				    		 },
			              ]
		 },
		]);


