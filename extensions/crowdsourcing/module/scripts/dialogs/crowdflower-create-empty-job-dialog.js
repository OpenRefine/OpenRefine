function ZemantaCrowdFlowerEmptyJobDialog() {
  this._onDone = onDone;
  this._extension = {};
  
  var self = this;
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-create-empty-job-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("CrowdFlower: Create empty job dialog");
  
  this._elmts.okButton.click(function() {
      DialogSystem.dismissUntil(self._level - 1);           
  });

  this._elmts.createJobButton.click(function() {
      var title = self._elmts.jobTitle.val();
      var instructions = self._elmts.jobInstructions.val();
      
      var cf_job = {
    		  "title": title,
    		  "instructions": instructions,
    		  "data" : [],
    		  "job_id": ''
      }; 
            
      self._extension = cf_job;
      
      $.post(
    		  "command/crowdsourcing/create-crowdflower-job",
    		  self._extension,
    		  function(data)
    		  {
    			  console.log("Got results from command: " + data.status);
    			  console.log("Job ID: " + data.job_id);
    			  self._update(data._extension.job_id);
    			  self._extension.job_id = data.job_id;
    		  },
    		  "json"
      );     
  });

  
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });
   
  this._level = DialogSystem.showDialog(this._dialog);
  
};

ZemantaCrowdFlowerEmptyJobDialog.prototype._update = function(data) {
	var container = this._elmts.results;
	$('<span style="font-weight:bold;">JOB ID: </span><span>' + data + '</span>').appendTo(container);
	
};