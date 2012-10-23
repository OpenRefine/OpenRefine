
function ZemantaCrowdFlowerDialog(onDone) {
  this._onDone = onDone;
  this._addAllColumns = false;
  this._jobColumns = [];
  
  var self = this;
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-job-columns-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Select columns for CrowdFlower job");
  
  this._elmts.okButton.click(function() {
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._jobColumns);
  });
  
  
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });
  
  
  colsHTML = ZemantaCrowdFlowerDialog.renderAllColumns();
  colsHTML.appendTo(this._elmts.columnList);
  
  this._level = DialogSystem.showDialog(this._dialog);
  
};

ZemantaCrowdFlowerDialog.renderAllColumns = function() {
	  
	var columns = theProject.columnModel.columns;
	console.log("project: " + columns);
	
	var columnContainer = $('<div id="columns">');
	var chkid = 1;

	var renderColumns = function(columns, elem) {
		$.each(columns, function(index, value){
			var input = $('<input type="checkbox" class="zem-col" value="' + value.name + 'id="' + 'chk_' + chkid + '">').appendTo(elem);
			$('<label for="chk_' + chkid + '">' + value.name + '</label>').appendTo(elem);
			$('<br />').appendTo(elem);
			chkid++;
			
			//in case any other column is clicked, all column is deselected
			input.click(function() {
				$('input#all-cols').attr('checked',false);
			});
		});
	};
	
	var input = $('<input type="checkbox" value="all" id="all-cols">').appendTo(columnContainer);
	$('<label for="all-cols">All columns </label>').appendTo(columnContainer);
	$('<br />').appendTo(columnContainer);
	renderColumns(columns, columnContainer);
	
	//check all columns by default
	input.click(function() {
		$('#columns input.zem-col').each(function () {
			$(this).attr('checked', true);
		});
	});
	
	return columnContainer;	
};
