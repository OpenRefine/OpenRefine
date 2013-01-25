function UtilitiesExtensionColumnsDialog(onDone) {
  this._onDone = onDone;
  this._columns = [];
  var dismissBusy = DialogSystem.showBusy();
    
  this._dialog = $(DOM.loadHTML("utilities", "scripts/dialogs/util-columns-to-remove-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Select columns to remove");
  
  var self = this;
  
  var columnsContainer = this._elmts.columnsMenu; 
  var columnListContainer = this._elmts.columnList;
  
  this._renderAllColumns(columnsContainer, columnListContainer);
  
  
  this._elmts.okButton.click(function() {
    	      
	  $('#project-columns input.zem-col:checked').each( function() {
		  var col_name = $(this).attr('value');
		  console.log("Added column name: " + col_name);
		  self._columns.push(col_name);
	  });

	  if(self._columns.length > 0) {
		  DialogSystem.dismissUntil(self._level - 1);
		  self._onDone(self._columns);
	  } else{
		  alert("No column was selected.");  
	  }
	  
  });
  
  
  this._elmts.cancelButton.click(function() {	  
    DialogSystem.dismissUntil(self._level - 1);
  });
   
    
  dismissBusy();
  this._level = DialogSystem.showDialog(this._dialog);
  
};



UtilitiesExtensionColumnsDialog.prototype._renderAllColumns = function(columnsContainer, columnListContainer) {
	  
	//var self = this;
	var columns = theProject.columnModel.columns;
	
	var chkid = 0;

	var renderColumns = function(columns, elem) {
		
		$.each(columns, function(index, value){
			var id = 'chk_' + chkid;
			$('<input type="checkbox" class="zem-col" value="' + value.name + '" id="' + id + '"/>').appendTo(elem);
			$('<label for="' + id + '">' + value.name + '</label> <br/>').appendTo(elem);
			chkid++;
		});
	};
	
	var linkSelectAll = $('<a href="#" id="select-all-columns"> Select all </a>');
	columnsContainer.append(linkSelectAll);
	var linkClearAll = $('<a href="#" id="clear-all-columns"> Clear all </a>');
	columnsContainer.append(linkClearAll);

	renderColumns(columns, columnListContainer);
	
	linkClearAll.click(function () {
		$('#project-columns input.zem-col').each(function () {
			$(this).attr('checked', false);
		});
	});
	
	linkSelectAll.click(function() {
		$('#project-columns input.zem-col').each(function () {
			$(this).attr('checked', true);
		});
	});
};

