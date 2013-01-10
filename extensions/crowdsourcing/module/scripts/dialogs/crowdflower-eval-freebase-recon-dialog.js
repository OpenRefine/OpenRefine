
function ZemantaCFEvaluateFreebaseReconDialog(onDone) {
  this._onDone = onDone;
  this._extension = {};
  this._mappedFields = [];
  this._fields = [];
  var dismissBusy = DialogSystem.showBusy();
    
  this._dialog = $(DOM.loadHTML("crowdsourcing", "scripts/dialogs/crowdflower-eval-freebase-recon-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Evaluate Freebase reconciliation");
  
  this._elmts.jobTabs.tabs();

  //var tabindex = 0;  
  var self = this;
  
  self._renderColumns();
   
  this._elmts.okButton.click(function() {
	  self._extension = {};
      self._extension.content_type = "json";
      self._extension.column_names = [];
            
      //add mappings for anchor, link and recon column
      
      self._extension.job_id = self._elmts.jobID.val();

      var tmp = {};
      tmp.name = $('option[name=anchor]:selected').val();
      tmp.safe_name = 'anchor';
      self._extension.column_names.push(tmp);
      console.log("Column names: " + JSON.stringify(self._extension.column_names));
      
      var tmp1 = {};
      tmp1.name = $('option[name=link]:selected').val();
      tmp1.safe_name = 'link';
      self._extension.column_names.push(tmp1);
      console.log("Column names: " + JSON.stringify(self._extension.column_names));


      var tmp2 = {};
      tmp2.name = $('option[name=gold1]:selected').val();
      tmp2.safe_name = 'best_suggestion_gold';
      self._extension.column_names.push(tmp2);
      console.log("Column names: " + JSON.stringify(self._extension.column_names));

      var tmp3 = {};
      tmp3.name = $('option[name=gold2]:selected').val();
      tmp3.safe_name = 'enter_freebase_link_gold';
      self._extension.column_names.push(tmp3);
      console.log("Column names: " + JSON.stringify(self._extension.column_names));

      

      self._extension.recon_column = $('input[name=columns]:checked').val();      
    
      $('#info-fields input:checked').each( function() {
    	  var col = {};
    	  col.name = $(this).attr('value');
    	  col.safe_name = ZemantaExtension.util.convert2SafeName(col.name);
    	  self._extension.column_names.push(col);
      });
 
      console.log(JSON.stringify(self._extension));
      
	  DialogSystem.dismissUntil(self._level - 1);
	  self._onDone(self._extension);
  });
  
  
  this._elmts.cancelButton.click(function() {	  
    DialogSystem.dismissUntil(self._level - 1);    
    
  });
   
  
  
  dismissBusy();
  this._level = DialogSystem.showDialog(this._dialog);
  
};




ZemantaCFEvaluateFreebaseReconDialog.prototype._renderColumns = function() {
	  
	var self = this;
	var columns = theProject.columnModel.columns;
	var columnListContainer = self._elmts.projectColumns;
	
	var anchor = self._elmts.anchorField;
	var link = self._elmts.linkField;
	var info = self._elmts.infoField;
	var gold1 = self._elmts.goldColumn1;
	var gold2 = self._elmts.goldColumn2;
	
	var chkid = 0;

	var renderColumns = function(columns, elem) {
		
		$.each(columns, function(index, value){			
			if(value.reconConfig != null) {
				var id = 'chk_' + chkid;
				$('<input type="radio" name="columns" class="zem-col" value="' + value.name + '" id="' + id + '"/>').appendTo(elem);
				$('<label for="' + id + '">' + value.name + '</label><br/>').appendTo(elem);
				chkid++;					
			}
			
			anchor.append($('<option value="' + value.name + '" name="anchor">'+ value.name + '</option>'));
			link.append($('<option value="' + value.name + '" name="link">'+ value.name + '</option>'));
			info.append($('<input type="checkbox" value="' + value.name + '" >'+ value.name + '</checkbox>'));
			gold1.append($('<option value="' + value.name + '" name="gold1">'+ value.name + '</option>'));
			gold2.append($('<option value="' + value.name + '" name="gold2">'+ value.name + '</option>'));
		});
	};
	
	renderColumns(columns, columnListContainer);

};


ZemantaCFEvaluateFreebaseReconDialog.prototype._showColumnsDialog = function(field, mapped_col) {
	var self = this;
	
	var frame = DialogSystem.createDialog();
	  frame.width("500px");

	  $('<div></div>').addClass("dialog-header").text("Add mapping for field: " + field).appendTo(frame);
	  var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
	  var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

	  var columns = theProject.columnModel.columns;
	  
	  body.html(
			  '<div class="grid-layout layout-normal layout-full">' +
			 '<div id="columns" bind="projColumns" class="project-columns"></div>' +
			 '</div>'
	  );
	  
	  var bodyElmts = DOM.bind(body);
	  
	  console.log("Mapped column: " + mapped_col);
	  
	  $.each(columns, function(index, value){
		
		  var input = $('<input type="radio" name="columns" class="zem-col" value="' + value.name + '">' + value.name + '</input><br/>').appendTo(bodyElmts.projColumns);					
		  if(value.name === mapped_col) {
			  input.attr("checked","true");
		  }

	  });

	  footer.html(
			    '<button class="button" bind="okButton">&nbsp;&nbsp;OK&nbsp;&nbsp;</button>' +
			    '<button class="button" bind="cancelButton">Cancel</button>'
			  );
	  var footerElmts = DOM.bind(footer);

	  var level = DialogSystem.showDialog(frame);
	  
	  var dismiss = function() {
	    DialogSystem.dismissUntil(level - 1);
	  };

	  footerElmts.cancelButton.click(dismiss);

	  footerElmts.okButton.click(function() {
		  console.log("Column selected:" + $('input[name=columns]:checked').val());
		  var new_mapped = $('input[name=columns]:checked').val();
		  self._addFCMapping(field, mapped_col, new_mapped);		  
		  console.log("Updated mappings: " + JSON.stringify(self._mappedFields));
		  dismiss();
		  self._renderMappings();
	  });
};

ZemantaCFEvaluateFreebaseReconDialog.prototype._renderMappings = function() {
	var self = this;
	var elm_fields = self._elmts.extJobFields;
	elm_fields.empty();

	console.log("Rendering mappings...");
	//TODO: if initial render, there is data from response, otherwise store it somewhere
	$.each(self._fields, function(index, value) {
		var link = $('<a title="' + value + '" href="javascript:{}">' + value + '</a>').appendTo(elm_fields);
		$('<span>&nbsp;&nbsp;=&gt;&nbsp;&nbsp;</span>').appendTo(elm_fields);
		var mapped_column = self._getMappedColumn(value);
		console.log("Mapped column: " + mapped_column);
		var col_name = ((mapped_column === "") ? "(not mapped)": mapped_column);
		console.log("Getting mapped column: " + col_name);

		$('<span>' + col_name + '</span><br/>').appendTo(elm_fields);
		
		link.click (function(){
			var field = $(this).text();
			self._showColumnsDialog($(this).text(),self._getMappedColumn(field));
		});		
	});	

};

ZemantaCFEvaluateFreebaseReconDialog.prototype._addFCMapping = function(field, old_column, new_column) {
	var self = this;
	if(old_column === "") {
		var fc = {};
		fc.field = field;
		fc.column = new_column;
		self._mappedFields.push(fc);
	} else {
		$.each(self._mappedFields, function(index, value) {
			if(value.field === field) {
				value.column = new_column;
				return;
			}
		});
	}
};

ZemantaCFEvaluateFreebaseReconDialog.prototype._getMappedColumn = function (field) {
	
	var self = this;
	var column = "";
	$.each(self._mappedFields, function(index, value) {
		if(value.field === field) {
			column = value.column;
		}
	});
	
	return column;
};

