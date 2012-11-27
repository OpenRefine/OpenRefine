var ZemantaExtension = {handlers: {}, util: {}};


ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings = function(getApiKey) {
	$.post(
		      "/command/core/get-all-preferences",
		      {},
		      function (data) {
		    	if (data!=null && data["crowdflower.apikey"]!=null) {
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

ZemantaExtension.util.convert2SafeName = function(columnName) {

	console.log("Column name:" + columnName);
	var patt = /(\s)+/ig;
	var rep = '_';
	var safeName = columnName.replace(patt, rep);
	console.log("Safe name: " + safeName);
	return safeName;
	
};

ZemantaExtension.util.generateCML = function() {
	var cml = '';
    $('#columns input.zem-col:checked').each( function() {
    	cml += '{{' + ZemantaExtension.util.convert2SafeName($(this).attr('value')) + '}}' + '<br/>';
    	console.log("CML: " + cml);
    });
    
	return cml;
};


ZemantaExtension.util.loadAllExistingJobs = function(getJobs) {
    $.post(
  		  "command/crowdsourcing/preview-crowdflower-jobs",
  		  { 
  		  },
  		  function(data)
  		  {
  			  if(data != null) {
  	  			  console.log("Status: " + data.status);
  	  			  if(data.status != "ERROR") {
  	  				  getJobs(data['jobs'],data.status);
  	  			  } else{
  	  				  console.log(data);
  	  				  alert("Error occured while loading existing jobs. Error: " + data['message']);  
  	  				  getJobs([], data.message);
  	  			  }
  			  }
  		  },
  		  "json"
    );     

};


//jobParams: jobID, all_units - default false, gold - default false
//copy job, return updated list of jobs and new job id
ZemantaExtension.util.copyJob = function(extension, updateJobs) {
		
    $.post(
  		  "command/crowdsourcing/copy-crowdflower-job",
  		  {"extension": JSON.stringify(extension)},
  		  function(data)
  		  {
  			  console.log("Data returned: " + JSON.stringify(data));
  			  
  			  if(data != null) {
  	  			  console.log("Status: " + data.status);
  	  			  updateJobs(data);
  			  } 
  			  else {
  				  alert("Could not refresh list of jobs.");
  			  }
  		  },
  		  "json"
    );     
};


ZemantaExtension.util.getJobInfo = function(extension, updateJobInfo) {
	
    $.post(
  		  "command/crowdsourcing/get-crowdflower-job",
  		  {"extension": JSON.stringify(extension)},
  		  function(data)
  		  {
  			  console.log("Data returned: " + JSON.stringify(data));
  			  
  			  if(data != null) {
  	  			  console.log("Status: " + data.status);
  	  			  updateJobInfo(data);
  			  } else {
  				alert("Error occured while updating job information.");  
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

ZemantaExtension.handlers.getApiKey =  function() {
	console.log("Getting API key...");
	ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings(function(apiKey) {
		console.log("Read API key: " + apiKey);
		return apiKey;
	});
};



//todo: add configuration for job: number of judgements, gold, etc.
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
				    			 "id": "zemanta/crowdflower/create-crowdflower-job",
				    			 label: "Create new job / upload data",
				    			 click: ZemantaExtension.handlers.openJobSettingsDialog
				    		 },
				    		 {
				    			 "id": "zemanta/crowdflower/configure-job",
				    			 "label" :  "Configure job",
				    			 click: ZemantaExtension.handlers.doNothing 
				    		 },
				    		 {
				    			"id": "zemanta/crowdflower/download-results",
				    			"label": "Download results",
				    			click: ZemantaExtension.handlers.doNothing
				    			 
				    		 },
				    		 {},

			     		 {
			    			 "id": "zemanta/crowdflower/settings",
			    			 "label": "Set CrowdFlower API key",
			    			 click: ZemantaExtension.handlers.storeCrowdFlowerAPIKey
			    		 },
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