ZemantaCrowdSourcingExtension.util.loadCrowdFlowerApiKeyFromSettings = function(getApiKey) {
	$.post(
		      "/command/core/get-all-preferences",
		      {},
		      function (data) {
		    	if (data != null && data["crowdflower.apikey"] != null) {

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

ZemantaCrowdSourcingExtension.util.convert2SafeName = function(columnName) {

	var patt = /(\s)+/ig;
	var rep = '_';
	var safeName = columnName.toLowerCase();
	safeName = safeName.replace(patt, rep);
	return safeName;
	
};


ZemantaCrowdSourcingExtension.util.generateCML = function(tabindex) {
	var cml = '';
    $('#project-columns-'+ tabindex + ' input.zem-col:checked').each( function() {
    	cml += '{{' + ZemantaExtension.util.convert2SafeName($(this).attr('value')) + '}}' + '<br/>';
    });
    
	return cml;
};


ZemantaCrowdSourcingExtension.util.loadAllExistingJobs = function(getJobs) {
    $.post(
  		  "command/crowdsourcing/preview-crowdflower-jobs",
  		  { 
  		  },
  		  function(data)
  		  {
  			  if(data != null) {  	  			  
  	  			  if(data.status != "ERROR") {
  	  				  getJobs(data['jobs'],data.status);
  	  			  } else{
  	  				  alert("An error occured while loading existing jobs.\n" + data.message);  
  	  				  getJobs([], data.message);
  	  			  }
  			  }
  		  },
  		  "json"
    );     

};


ZemantaCrowdSourcingExtension.util.copyJob = function(extension, updateJobs) {
    $.post(
  		  "command/crowdsourcing/copy-crowdflower-job",
  		  {"extension": JSON.stringify(extension)},
  		  function(data)
  		  {
  			  if(data != null && data.status != "ERROR") {
   	  			  updateJobs(data);
  			  } 
  			  else {
  				  alert("Could not refresh the job list.\n" + data.message);
  			  }
  		  },
  		  "json"
    );     
};


ZemantaCrowdSourcingExtension.util.getJobInfo = function(extension, updateJobInfo) {
	
    $.post(
  		  "command/crowdsourcing/get-crowdflower-job",
  		  {"extension": JSON.stringify(extension)},
  		  function(data)
  		  {
  			  if(data != null && data.status != "ERROR") {
  	  			  updateJobInfo(data);
  			  } else {
  				alert("Error occured while updating job information.\n" + data.message);
  			  }
  		  },
  		  "json"
    );     
};


