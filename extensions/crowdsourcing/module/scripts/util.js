ZemantaExtension.util.loadCrowdFlowerApiKeyFromSettings = function(getApiKey) {
	$.post(
		      "/command/core/get-all-preferences",
		      {},
		      function (data) {
		    	  console.log("All CF settings: " + data.stringify());
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

	//console.log("Column name:" + columnName);
	var patt = /(\s)+/ig;
	var rep = '_';
	var safeName = columnName.toLowerCase();
	safeName = safeName.replace(patt, rep);
	console.log("Safe name: " + safeName);
	return safeName;
	
};


ZemantaExtension.util.generateCML = function(tabindex) {
	var cml = '';
    $('#project-columns-'+ tabindex + ' input.zem-col:checked').each( function() {
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
  	  				  console.log(JSON.stringify(data));
  	  				  alert("Error occured while loading existing jobs. Error: " + data['message']);  
  	  				  getJobs([], data.message);
  	  			  }
  			  }
  		  },
  		  "json"
    );     

};


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
