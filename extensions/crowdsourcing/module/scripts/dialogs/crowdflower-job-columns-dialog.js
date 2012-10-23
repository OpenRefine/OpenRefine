
function ZemantaCrowdFlowerDialog(onDone) {
  this._onDone = onDone;
  this._addAllColumns = false;
  this._jobColumns = [];
  
  var self = this;
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-job-columns-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Select columns for CrowdFlower job");
  
  this._elmts.okButton.click(function() {
	  jobColumns = self._jobColumns;
      $('#column input.zem-type:checked').each(function(){
      	jobColumns.push($(this).attr('value'));
      });
      
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._jobColumns);
  });
  
  
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });
  
};

