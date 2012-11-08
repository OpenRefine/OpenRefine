
function ZemantaCrowdFlowerDialog(onDone) {
  this._onDone = onDone;
  this._extension = {};
  
  var self = this;
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-job-columns-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Enter details for new CrowdFlower job");
  
  this._elmts.jobTabs.tabs();
  
  this._elmts.okButton.click(function() {
      self._extension.title= self._elmts.jobTitle.val();
      self._extension.instructions = self._elmts.jobInstructions.val();
      self._extension.content_type = "json";
      self._extension.column_names = [];
      self._extension.new_job = true;
      //self._extension.id="142827";
      self._extension.upload = self._elmts.uploadChkbox.is(':checked');

      
      $('#columns input.zem-col:checked').each( function() {
    	  self._extension.column_names.push($(this).attr('value'));
      });
      
      console.log("Columns: " + self._extension.column_names);
      
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._extension);
  });
  
  
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });
  
  
  colsHTML = ZemantaCrowdFlowerDialog.renderAllColumns();
  colsHTML.appendTo(this._elmts.columnList);
  
  this._elmts.columnList.hide();//find(':input:not(:disabled)').prop('disabled', true);
  
  this._elmts.uploadChkbox.click(function() {
	  
	  var enabled = self._elmts.uploadChkbox.is(':checked');
	  
	  if(enabled) {
		  self._elmts.columnList.show();
	  } else {
		  self._elmts.columnList.hide();
	  }
  });

  
  //adding validator to title
  this._elmts.jobTitle.blur(function () {
	  var title = self._elmts.jobTitle.val();
	  
	  if(title.length < 5) {
		  alert("Title should be between 5 and 255 chars.");
	  }
	  
  });
  
  
  this._level = DialogSystem.showDialog(this._dialog);
  
};


ZemantaCrowdFlowerDialog.renderAllColumns = function() {
	  
	var columns = theProject.columnModel.columns;
	
	var columnContainer = $('<div id="all-columns">');
	var columnListContainer = $('<div id="project-columns">');
	var chkid = 0;

	var renderColumns = function(columns, elem) {
		
		$.each(columns, function(index, value){
			var input = $('<input type="checkbox" class="zem-col" value="' + value.name + '" id="' + 'chk_' + chkid + '">').appendTo(elem);
			$('<label for="chk_' + chkid + '">' + value.name + '</label> <br/>').appendTo(elem);
			chkid++;
			
			//in case any other column is clicked, all-columns checked turns into false
			input.click(function() {
				$('input#all-cols').attr('checked',false);
			});
		});
		
	};
	
	var input = $('<input type="checkbox" value="all" id="all-cols">').appendTo(columnContainer);
	$('<label for="all-cols">All columns </label>').appendTo(columnContainer);
	$('<br /><br />').appendTo(columnContainer);
	renderColumns(columns, columnListContainer);
	columnListContainer.appendTo(columnContainer);
	
	//check all columns by default
	input.click(function() {
		$('#columns input.zem-col').each(function () {
			$(this).attr('checked', true);
		});
	});
	
	return columnContainer;	
};
