
function ZemantaCrowdFlowerGoldDialog(onDone) {
  this._onDone = onDone;
  this._extension = {};
  this._existingJob = false;
  
  var self = this;
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-create-gold-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Create gold data");
  
  this._elmts.jobTabs.tabs();

  this._renderAllExistingJobs();
  this._renderAllColumns(this._elmts.columnList);
  
  this._elmts.columnsPanel.hide();
  
  this._elmts.chkUploadToNewJob.click(function () {
	
	  if(self._elmts.chkUploadToNewJob.is(':checked')) {
		  self._elmts.columnsPanel.show();
	  }
	  else {
		  self._elmts.columnsPanel.hide();
	  }
	  
  });
  
  this._elmts.copyJob.click(function() {
	  //get job id to copy from list of jobs
	  var params = {};
	  params.job_id = 0;
	  params.all_units = false;
	  params.gold = false;
	  
	  //TODO: check whether this is ok
	  self._extension = {};
	  self._extension = params;
	
	  ZemantaExtension.util.copyJob(self._extension, function(data){
		  self._updateJobList(data);
	  });
	  	  
  });
  
  this._elmts.okButton.click(function() {
	  self._extension = {};
      self._extension.title= self._elmts.jobTitle.val();
      self._extension.instructions = self._elmts.jobInstructions.val();
      self._extension.content_type = "json";
      self._extension.column_names = [];
      
      self._extension.new_job = true; //TODO: read this value from radio button
      
      //TODO: check if cml exists, if not, create a default one from column names
      $('#columns input.zem-col:checked').each( function() {
    	  self._extension.column_names.push($(this).attr('value'));
      });
      
      //TODO: if cml exist, get column names from it, column names are identified by {{}}
      
      console.log("Columns: " + self._extension.column_names);
      
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._extension);
  });
  
  
  this._elmts.cancelButton.click(function() {
	  
	  var curTabPanel = $('#jobTabs .ui-tabs-panel:not(.ui-tabs-hide)');
	  
	  var index = curTabPanel.index();
	  alert("Index: " + index);
	  
    DialogSystem.dismissUntil(self._level - 1);
  });
   
  
  this._elmts.jobTitle.blur(function () {
	  var title = self._elmts.jobTitle.val();	  
	  if(title.length < 5 || title.length > 255  ) {
		  //TODO: add better visual clues
		  alert("Title should be between 5 and 255 chars.");
	  } 
  });
  
  
  this._level = DialogSystem.showDialog(this._dialog);
  
};

//TODO: test this!!!!
ZemantaCrowdFlowerGoldDialog.prototype._updateJobList = function(data) {
	var selContainer = this._elmts.allJobsList;
	var jobs = data["jobs"];
	var selected = "";
	console.log("Data: " + JSON.stringify(data));
	
	//TODO: clear options from the list
	selContainer.empty();
	
	$('<option name="opt_none" value="none">--- select a job --- </option>').appendTo(selContainer);
	
	for (var index = 0; index < jobs.length; index++) {
		var value = jobs[index];
		console.log("Value: " + value);
		
		if(value.id === data.job_id) {
			console.log("Selected!");
			selected = " selected";
		} else {
			selected = "";
		}
		
		var job = $('<option name="opt_' + index + '" value=' + value.id + '' + selected + '>' + value.title + ' (job id: ' + value.id + ')</option>');		
		selContainer.append(job);

	}
};

ZemantaCrowdFlowerGoldDialog.prototype._renderAllExistingJobs = function() {
	
	var self = this;
	//var jobsContainer = $('<div id="existing-jobs">'); 
	var selContainer = self._elmts.allJobsList; //$('<select name="all-jobs">');
	var elemStatus = self._elmts.statusMessage;
	
	$('<option name="opt_none" value="none">--- select a job --- </option>').appendTo(selContainer);
	//$('<option name="opt_one" value="none">option 1</option>').appendTo(selContainer);
	
	ZemantaExtension.util.loadAllExistingJobs(function(data, status) {
		
		elemStatus.html("Status: " + status);
		
		$.each(data, function(index, value) {
			
			var title = (value.title == null)? "Title not defined" : value.title;
			
			var job = $('<option name="opt_' + index + '" value=' + value.id + '>' + title + ' (job id: ' + value.id + ')</option>');
			
			selContainer.append(job);
		});
		
		selContainer.change(function() {
			alert($(this).children(":selected").val());
		});
		
		//jobsContainer.append(selContainer);
		
		//var refresh_btn = $('<input type="button" value="Refresh" id="refresh-job-list" bind="refreshJobList"/>');
		
		//refresh_btn.click(function() {
		//	alert("Refresh list!");
		//	//todo: delete all options? How is a list updated?
		//});
		
		//jobsContainer.append(refresh_btn);
		//elem.append(jobsContainer);
	});
	
	
	
};


ZemantaCrowdFlowerGoldDialog.prototype._renderAllColumns = function(elem) {
	  
	var columns = theProject.columnModel.columns;
	
	var columnContainer = $('<div id="all-columns">');
	var columnListContainer = $('<div id="project-columns">');
	var chkid = 0;

	var renderColumns = function(columns, elem) {
		
		$.each(columns, function(index, value){
			var id = 'chk_' + chkid;
			var input = $('<input type="checkbox" class="zem-col" value="' + value.name + '" id="' + id + '">').appendTo(elem);
			$('<label for="' + id + '">' + value.name + '</label> <br/>').appendTo(elem);
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
		$('#project-columns input.zem-col').each(function () {
			$(this).attr('checked', true);
		});
	});
	
	columnContainer.appendTo(elem);	
};
