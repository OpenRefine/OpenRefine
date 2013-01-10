/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

function ZemantaExtractEntitiesPreviewDialog(column, columnIndex,cellText,rowIndices, onDone) {
  this._column = column;
  this._columnIndex = columnIndex;
  this._rowIndices = rowIndices;
  this._onDone = onDone;
  this._extension = { entities: [], types: [] };
  this._extractAllTypes = false;

  var self = this;
  this._dialog = $(DOM.loadHTML("dbpedia-extension", "scripts/dialogs/extract-entities-preview-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._elmts.dialogHeader.text("Extract entities from '" + column.name + "'");
  this._elmts.originalText.text(cellText);
  
  this._elmts.okButton.click(function() {
	  extension = self._extension;
      $('#types input.zem-type:checked').each(function(){
      	extension.types.push($(this).attr('value'));
      })
      
      DialogSystem.dismissUntil(self._level - 1);
      self._onDone(self._extension);
  });
  
  
  this._elmts.cancelButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });
  

  var dismissBusy = DialogSystem.showBusy();
  ZemantaExtension.util.loadZemantaApiKeyFromSettings(function (apiKey) {
	  ZemantaExtractEntitiesPreviewDialog.getAllEntities(apiKey, cellText, function(entities) {
		    dismissBusy();    
		    self._show(entities);
		  });
  });

  
}

ZemantaExtractEntitiesPreviewDialog.getAllEntities = function(apiKey, cellText, onDone) {
  var done = false;
  var weHaveSuccess = false;
  
  if(apiKey) {  
	  $.ajax({
	      url: 'http://api.zemanta.com/services/rest/0.0/',
	      type: 'POST',
	      data: ZemantaExtension.util.prepareZemantaData(apiKey, cellText),
	      success: function (data) {
		      if (done) return;
		      	done = true;
              var allEntities = [];
              
              if(data != null && data.markup != null) {
	              for (var i = 0; i < data.markup.links.length; i++) {
	            	  allEntities.push(data.markup.links[i]);
	              }
	              weHaveSuccess = true;
	              onDone(allEntities);	      
      		  }
	      },
	      error: function(xhr, status, error){
	          alert("Error: " + xhr.status);
	      },
	      complete: function(){
	          if(!weHaveSuccess){
	               done = true;
	               onDone([]);
	          }
	      }
	     });
	    
	  	window.setTimeout(function() {
	  		if (done) return;
	
	    done = true;
	    console.log("Zemanta API timed out...");
	    alert("Zemanta API request timed out... please try again later.");
	    onDone([]);
	  }, 7000); // time to give up?
  }
  else {
	  alert("It seems you don't have a (valid) Zemanta API key in settings. " +
			  "Apply for Zemanta API at developer.zemanta.com and " +
			  "then save it into preferences. You can use Zemanta menu for this.");
	    onDone([]);

  }
};

ZemantaExtractEntitiesPreviewDialog.prototype._show = function(entities) {
  this._level = DialogSystem.showDialog(this._dialog);
  var container = this._elmts.previewContainer;
  var typesContainer = this._elmts.extractedTypes;
  container.empty();   
  typesContainer.empty();

  var div = $('<div id="entities">').appendTo(container);
  var ul = $('<ul>').appendTo(div);
  var alltypes = [];

  
//  if(entities.length < 1) {
//	  container.html('<span class="error">No entities were extracted, because Zemanta API is not available.' +  
//			  '<br />Please check your internet connection and try again later.</span>');
//	  return;
//  }
  var renderEntity = function(entity, elem) {
	  var label = entity.anchor;
	  alltypes = alltypes.concat(entity.entity_type);
	  tooltip = (entity.entity_type.length > 0)?entity.entity_type:"unknown";
	  $('<li><a class="extracted-entity" href="#" title="' + tooltip +'">' + label + "</a></li>")
	  .appendTo(elem);	  
  };
  
  for (var i = 0; i < entities.length; i++) {
	  renderEntity(entities[i], ul);
  }
  
  var getUniqueEntityTypes = function(types) {
	  var arrDistinct = new Array();
	  $.each(types, function(index, value) {
		  if($.inArray(value,arrDistinct) == -1) {
			  arrDistinct.push(value);
		  }			  
	  });
	  return arrDistinct;
  }
  
  var renderEntityTypesFilter = function(types, elem) {
	  var chkid = 1;
	  $.each(types, function(index, value) {
		  var input = $('<input type="checkbox" class="zem-type" value="' + value + 
				  '" id="' + "chk_" + chkid+'">').appendTo(elem);
		  $('<label for=""' + "chk_" + chkid + '>' + value + '</label>').appendTo(elem);
		  $('<br/>').appendTo(elem);
		  chkid++;
		  
		  input.click(function(){
			  $('#types input#all-types').attr('checked',false);
		  });
	  });
  };
  
  var typefilter = $('<div id="types">').appendTo(typesContainer);
  var input = $('<input type="checkbox" id="all-types" bind="allTypes" value="all">').appendTo(typefilter);
  $('<label for="allTypes" class="all-types-label">All types</label>').appendTo(typefilter);
  $('<br/>').appendTo(typefilter);
  alltypes = alltypes.concat(["unknown"]);
  alltypes = getUniqueEntityTypes(alltypes);
  renderEntityTypesFilter(alltypes,typefilter);

  input.click(function(){
	  //check all checkboxes
	  $('#types input.zem-type').each(function(){
		  $(this).attr('checked',true);
	  });
  });

  
};


