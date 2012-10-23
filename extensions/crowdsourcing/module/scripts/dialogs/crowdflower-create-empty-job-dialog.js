function ZemantaCrowdFlowerEmptyJobDialog(onDone) {
  this._onDone = onDone;
  this._jobData = {};
  this._jobColumns = [];
  
  var self = this;
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-create-empty-job-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("CrowdFlower: Create empty job dialog");
  
  this._elmts.okButton.click(function() {
      DialogSystem.dismissUntil(self._level - 1);
      
      var title = self._elmts.jobTitle.val();
      var instructions = self._elmts.jobInstructions.val();
      
      var cf_job = {
    		  "title": title,
    		  "instructions": instructions
      }; 
      self._jobData = cf_job;
     
      self._onDone(self._jobData);
  });
  
  
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });
   
  this._level = DialogSystem.showDialog(this._dialog);
  
};