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

var SchemaAlignment = {};

SchemaAlignment._cleanName = function(s) {
   return s.replace(/\W/g, " ").replace(/\s+/g, " ").toLowerCase();
};

var SNACSchemaAlignmentDialog = {

};

/**
 * Installs the tabs in the UI the first time the snac
 * extension is called.
 */
SNACSchemaAlignmentDialog.setUpTabs = function() {
   var self = this;
   this._rightPanel = $('#right-panel');
   this._viewPanel = $('#view-panel').addClass('main-view-panel-tab');
   this._toolPanel = $('#tool-panel');
   this._summaryBar = $('#summary-bar')
      .addClass('main-view-panel-tab-header')
      .addClass('active')
      .attr('href', '#view-panel');

   this._schemaPanel = $('<div id="snac-schema-panel"></div>')
      .addClass('main-view-panel-tab')
      .appendTo(this._rightPanel);
   this._issuesPanel = $('<div id="snac-issues-panel"></div>')
      .addClass('main-view-panel-tab')
      .appendTo(this._rightPanel);
   this._previewPanel = $('<div id="snac-preview-panel"></div>')
      .addClass('main-view-panel-tab')
      .appendTo(this._rightPanel);

   var schemaButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
      .addClass('main-view-panel-tabs-snac')
      .attr('href', '#snac-schema-panel')
      .text($.i18n('snac-schema/schema-tab-header'))
      .appendTo(this._toolPanel)

   var issuesButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
      .addClass('main-view-panel-tabs-snac')
      .attr('href', '#snac-issues-panel')
      .text($.i18n('snac-schema/warnings-tab-header')+' ')
      .appendTo(this._toolPanel)
      .click(function() { SNACSchemaAlignmentDialog._save(); });
   this.issuesTabCount = $('<span></span>')
      .addClass('schema-alignment-total-warning-count')
      .appendTo(issuesButton)
      .hide();
   this.issueSpinner = $('<img />')
      .attr('src', 'images/large-spinner.gif')
      .attr('width', '16px')
      .appendTo(issuesButton);
   var previewButton = $('<div></div>')
      .addClass('main-view-panel-tab-header')
      .addClass('main-view-panel-tabs-snac')
      .attr('href', '#snac-preview-panel')
      .text($.i18n('snac-schema/edits-preview-tab-header'))
      .appendTo(this._toolPanel)
      // .click(function() { SNACSchemaAlignmentDialog._save(); });
   this.previewSpinner = $('<img />')
      .attr('src', 'images/large-spinner.gif')
      .attr('width', '16px')
      .appendTo(previewButton);

   this._unsavedIndicator = $('<span></span>')
      .html('&nbsp;*')
      .attr('title', $.i18n('snac-schema/unsaved-changes-alt'))
      .hide()
      .appendTo(schemaButton);


   $('.main-view-panel-tabs-snac').hide();

   $('.main-view-panel-tab-header').click(function(e) {
      var targetTab = $(this).attr('href');
      SNACSchemaAlignmentDialog.switchTab(targetTab);
      e.preventDefault();
   });

  /**
   * Init the schema tab
   */
   var schemaTab = $(DOM.loadHTML("snac", "scripts/schema-alignment-tab.html")).appendTo(this._schemaPanel);
   var schemaElmts = this._schemaElmts = DOM.bind(schemaTab);
   schemaElmts.dialogExplanation.text($.i18n('snac-schema/dialog-explanation'));
   // this._plusButton($.i18n('snac-schema/add-item-button'), schemaElmts.addItemButton);
   // schemaElmts.addItemButton.click(function(e) {
   //   self._addItem();
   //   SNACSchemaAlignmentDialog._hasChanged();
   //   e.preventDefault();
   // });
   schemaElmts.saveButton
      .text($.i18n('snac-schema/save-button'))
      .attr('title', $.i18n('snac-schema/save-schema-alt'))
      .prop('disabled', false)
      .addClass('disabled')
      .click(function() { SNACSchemaAlignmentDialog._save(); });
   schemaElmts.discardButton
      .text($.i18n('snac-schema/discard-button'))
      .attr('title', $.i18n('snac-schema/discard-schema-changes-alt'))
      .prop('disabled', false)
      .addClass('disabled')
      .click(function() { SNACSchemaAlignmentDialog._discardChanges(); });

   this._wikibasePrefix = "http://www.snac.org/entity/"; // hardcoded for now

   // Init the column area
   this.updateColumns();

   $('.schema-alignment-dialog-columns-area-constellation').hide();
   $('.schema-alignment-dialog-dropdown-area-constellation').hide();
   $('.schema-alignment-dialog-columns-area-constellation--ref').hide();

   var url = ReconciliationManager.ensureDefaultServicePresent();
   SNACSchemaAlignmentDialog._reconService = ReconciliationManager.getServiceFromUrl(url);

   /**
   * Init the dropdowns area
   */
   // this.addDropdowns();

   /**
   * Init the issues tab
   */
   var issuesTab = $(DOM.loadHTML("snac", "scripts/issues-tab.html")).appendTo(this._issuesPanel);
   var issuesElmts = this._issuesElmts = DOM.bind(issuesTab);
   issuesElmts.invalidSchemaWarningIssues.text($.i18n('snac-schema/invalid-schema-warning-issues'));

   /**
   * Init the preview tab
   */
   var previewTab = $(DOM.loadHTML("snac", "scripts/preview-tab.html")).appendTo(this._previewPanel);
   var previewElmts = this._previewElmts = DOM.bind(previewTab);
   SNACSchemaAlignmentDialog.updateNbEdits(0);
   previewElmts.invalidSchemaWarningPreview.text($.i18n('snac-schema/invalid-schema-warning-preview'));

   this._previewPanes = $(".schema-alignment-dialog-preview");

   // Load the existing schema
   this._reset(theProject.overlayModels.wikibaseSchema);
   // Perform initial preview
   this.preview();
}

/*******************************************************
* Schema Tab Matching for Resources and Constellations *
********************************************************/

//Create a table for the resource page function
function addResourceTable(columns, SNACcolumns) {
   var myTableDiv = document.getElementById("myDynamicTableResource");
   // myTableDiv.setAttribute('style', 'margin-left: 18px;');

   var table = document.createElement('TABLE');
   // table.border = '1px';

   var tableBody = document.createElement('TBODY');
   table.appendChild(tableBody);
   myTableDiv.appendChild(table);

   makeDropdown = () => {
      let dropdownOptionsArray = [document.getElementsByClassName('dropdown-default-resource')[0]];
      for (var i = 0; i < 10; i++) {
         let dropdownOptions = document.getElementsByClassName('dropdown-option-resource')[i];
         dropdownOptionsArray.push(dropdownOptions)
      }
      return dropdownOptionsArray;
   }

   for (var i = 0; i < columns.length; i++) {
      var tr = document.createElement('TR');
      tableBody.appendChild(tr);
      var column = columns[i];

      for (var j = 0; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.width = '100';
         var reconConfig = column.reconConfig;
         var cell = SNACSchemaAlignmentDialog._createDraggableColumn(column.name,
         reconConfig && reconConfig.identifierSpace === this._wikibasePrefix && column.reconStats);
         var dragDivElement = cell[0];
         var dragNode = document.createElement('div');
         dragNode.className += 'wbs-draggable-column wbs-unreconciled-column-undraggable';
         dragNode.style = 'width: 150px';
         dragNode.id = i;
         dragNode.append(dragDivElement.innerHTML);
         td.appendChild(dragNode);
         tr.appendChild(td);
      }

      var selectList = $("<select></select>").addClass('selectColumn').addClass('selectColumnRes').attr('style', 'width: 180px');

      //Create and append the options
      var defaultoption = document.createElement("option");
      defaultoption.setAttribute("value", "");
      defaultoption.text = "Select an Option";
      defaultoption.classList.add("dropdown-default-resource");
      selectList.append(defaultoption);

      for (var j = 0; j < SNACcolumns.length; j++) {
         var option = document.createElement("option");
         option.setAttribute("value", SNACcolumns[j]);
         option.text = SNACcolumns[j];
         option.classList.add("dropdown-option-resource");
         selectList.append(option);
      }

      for (var j = 1; j < 2; j+=2) {
         var td = document.createElement('TD');
         // td.width = '75';
         td.appendChild(selectList[0]);
         tr.appendChild(td);
      }
   }
   return myTableDiv;
 }

//Create a table for the constellation page function
function addConstellationTable(columns, SNACcolumns) {
   var myTableDiv = document.getElementById("myDynamicTableConstellation");
   var table = document.createElement('TABLE');
   var tableBody = document.createElement('TBODY');
   table.appendChild(tableBody);
   myTableDiv.appendChild(table);

   makeDropdown = () => {
      let dropdownOptionsArray = [document.getElementsByClassName('dropdown-default-const')[0]];
      for (var i = 0; i < 10; i++) {
         let dropdownOptions = document.getElementsByClassName('dropdown-option-const')[i];
         dropdownOptionsArray.push(dropdownOptions)
      }
      return dropdownOptionsArray;
   }

   // let columnsResource = ["id", "entity_type", "name_entry", "surname", "forename", "exist_dates", "bioghist", "place", "occupation", "related_constellation_ids", "related_resource_ids"];
   // for (var i = 0; i < columnsResource.length; i++) {
   for (var i = 0; i < columns.length; i++) {

      var tr = document.createElement('TR');
      tableBody.appendChild(tr);
      var column = columns[i];
      // var columnsResource = columns[i];

      for (var j = 0; j < 2; j+=2) {
         var td = document.createElement('TD');
         td.width = '100';
         // var reconConfig = columnsResource.reconConfig;
         // var cell = SNACSchemaAlignmentDialog._createDraggableColumn(columnsResource[i], false);
         var reconConfig = column.reconConfig;
         var cell = SNACSchemaAlignmentDialog._createDraggableColumn(column.name,
         reconConfig && reconConfig.identifierSpace === this._wikibasePrefix && column.reconStats);

         // var cell = SNACSchemaAlignmentDialog._createDraggableColumn(columnsResource[i],
         //    reconConfig && reconConfig.identifierSpace === this._wikibasePrefix && column.reconStats);
         var dragDivElement = cell[0];
         var dragNode = document.createElement('div');
         dragNode.className += 'wbs-draggable-column wbs-unreconciled-column-undraggable';
         dragNode.style = 'width: 150px';
         dragNode.id = i + columns.length;
         dragNode.append(dragDivElement.innerHTML);
         td.appendChild(dragNode);
         tr.appendChild(td);
      }

      var selectList = $("<select></select>").addClass('selectColumn').addClass('selectColumnConst').attr('style', 'width: 180px');

      //Create and append the options
      var defaultoption = document.createElement("option");
      defaultoption.setAttribute("value", "");
      defaultoption.text = "Select an Option";
      defaultoption.classList.add("dropdown-default-const");
      selectList.append(defaultoption);

      for (var j = 0; j < SNACcolumns.length; j++) {
         var option = document.createElement("option");
         option.setAttribute("value", SNACcolumns[j]);
         option.text = SNACcolumns[j];
         option.classList.add("dropdown-option-const");
         selectList.append(option);
      }

      for (var j = 1; j < 2; j+=2) {
         var td = document.createElement('TD');
         // td.width = '75';
         td.appendChild(selectList[0]);
         tr.appendChild(td);
      }
   }
   return myTableDiv;
}

SNACSchemaAlignmentDialog.updateColumns = function() {
   // ******* RESOURCES PAGE ******* //
   var columnsResource = theProject.columnModel.columns;
   this._columnAreaResource = $(".schema-alignment-dialog-columns-area-resource");
   this._columnAreaResource.addClass("snac-tab");
   this._columnAreaResource.empty();

   var SNACcolumnsResource = ["ID", "Type", "Title", "Display Entry", "Link", "Abstract", "Extent", "Date", "Language", "Holding Repository SNAC ID", "Note"];
   this._dropdownAreaResource = $(".schema-alignment-dialog-dropdown-area-resource");
   this._dropdownAreaResource.addClass("snac-tab");
   this._dropdownAreaResource.empty();

   // var dragItemsResource = ["ID", "Type", "Title", "Display Entry", "Link", "Abstract", "Extent", "Date", "Language", "Holding Repository SNAC ID", "Note"];
   var dragItemsResource = ["Abstract", "Date", "Display Entry", "Extent", "Holding Repository SNAC ID", "ID", "Language", "Link", "Note", "Title", "Type"];
   this._refcolumnAreaResource = $(".schema-alignment-dialog-columns-area-resource--ref");
   this._refcolumnAreaResource.addClass("snac-tab");
   this._refcolumnAreaResource.empty();

   var myTableDivResource = addResourceTable(columnsResource, SNACcolumnsResource);
   this._columnAreaResource.append(myTableDivResource);

   for (var i = 0; i < dragItemsResource.length; i++) {
      var cell = SNACSchemaAlignmentDialog._createDraggableColumn(dragItemsResource[i], false);
      cell.attr('id', 'dragResource');
      cell.attr('value', dragItemsResource[i]);
      this._refcolumnAreaResource.append(cell);
   }
   $('[id="dragResource"]').addClass("tooltip");

   // var toolTipSpan = document.createElement("span");                 // Create a <li> node
   // var toolTiptext = document.createTextNode("Water");





   // console.log($('[id="dragResource"]')[0].appendChild());

   // ******* CONSTELLATIONS PAGE ******* //
   // var columnsConstellation = theProject.columnModel.columns;
   var columnsConstellation = theProject.columnModel.columns;
   this._columnAreaConstellation = $(".schema-alignment-dialog-columns-area-constellation");
   this._columnAreaConstellation.addClass("snac-tab");
   this._columnAreaConstellation.empty();

   var SNACcolumnsConstellation = ["ID", "Entity Type", "Name Entry", "Surname", "Forename", "Exist Dates", "BiogHist", "Place", "Occupation", "Related Constellation IDs", "Related Resource IDs"];
   this._dropdownAreaConestellation = $(".schema-alignment-dialog-dropdown-area-constellation");
   this._dropdownAreaConestellation.addClass("snac-tab");
   this._dropdownAreaConestellation.empty();

   // var dragItemsConstellation = ["ID", "Entity Type", "Name Entry", "Surename", "Forename", "Exist Dates", "BiogHist", "Place", "Occupation", "Related Constellation IDs", "Related Resource IDs"];
   var dragItemsConstellation = ["BiogHist", "Entity Type", "Exist Dates", "Forename", "ID", "Name Entry" ,"Occupation", "Place", "Related Constellation IDs", "Related Resource IDs", "Surname"];
   this._refcolumnAreaConestellation = $(".schema-alignment-dialog-columns-area-constellation--ref");
   this._refcolumnAreaConestellation.addClass("snac-tab");
   this._refcolumnAreaConestellation.empty();

   var myTableDivConstellation = addConstellationTable(columnsConstellation, SNACcolumnsConstellation);
   this._columnAreaConstellation.append(myTableDivConstellation);

   for (var i = 0; i < dragItemsConstellation.length; i++) {
      var cell = SNACSchemaAlignmentDialog._createDraggableColumn(dragItemsConstellation[i], false);
      cell.attr('id', 'dragConstellation');
      cell.attr('value', dragItemsConstellation[i]);
      this._refcolumnAreaConestellation.append(cell);
   }

   // Reference Validator Function
   var dragResourceIDs =$.makeArray($('[id="dragResource"]'));
   function hideAndDisableRef(evt) {
      const selectedValue = [];  //Array to hold selected values
      $selectsRef.find(':selected').filter(function(i, el) { // Filter selected values and push to array
         return $(el).attr('value');
      }).each(function(i, el) {
         selectedValue.push($(el).attr('value'));
      });

      $selectsRef.find('.dropdown-option-resource').each(function(i, option) {   // Loop through all of the options
         if (selectedValue.indexOf($(option).attr('value')) > -1) { // Re-enable option if array does not contain current value
            if ($(option).is(':checked')) {  // Disable if current value is selected, else skip
               return;
            } else {
               $(this).attr('disabled', true);
               dragResourceIDs.forEach(r => {
                  if(r.value==this.innerHTML){
                     r.style.visibility = 'hidden';   // Hide value
                  };
               });
            }
         } else {
            $(this).attr('disabled', false);
            dragResourceIDs.forEach(c => {
               if(c.value==this.innerHTML){
                  c.style.visibility = 'visible';  // Show value
               };
            });
         }
      });
   };

   //Constellation Validator Function
   var dragConstellationIDs =$.makeArray($('[id="dragConstellation"]'));
   function hideAndDisableConst(evt) {
      const selectedValue = [];  //Array to hold selected values
      $selectsConst.find(':selected').filter(function(i, el) { //Filter selected values and push to array
         return $(el).attr('value');
      }).each(function(i, el) {
         selectedValue.push($(el).attr('value'));
      });
      // loop all the options
      $selectsConst.find('.dropdown-option-const').each(function(i, option) {
         if (selectedValue.indexOf($(option).attr('value')) > -1) { //Re-enable option if array does not contain current value
            if ($(option).is(':checked')) {  //Disable if current value is selected, else skip
               return;
            } else {
               $(this).attr('disabled', true);
               dragConstellationIDs.forEach(r => {
                  if(r.value==this.innerHTML){
                     r.style.visibility = 'hidden';   //Hide value
                  };
               });
            }
         } else {
            $(this).attr('disabled', false);
            dragConstellationIDs.forEach(c => {
               if(c.value==this.innerHTML){
                  c.style.visibility = 'visible';  //Show value
               };
            });
         }
      });
   };

   var tooltipsResource = [
      "Summary abstract of the resource",                                     // Abstract
      "Date or date range (YYYY or YYYY-YYYY)",                               // Date
      "The descriptive display name (e.g. Jacob Miller Papers, 1809-1882)",   // Display Entry
      "Extent",                                                               // Extent
      "The SNAC ID of the resource's Holding Repository",                     // Holding Repository
      "ID",                                                                   // ID
      "Language",                                                             // Language
      "The preferred permanent link to find aid",                             // Link
      "Note",                                                                 // Note
      "The official title (e.g. Papers, 1809-1882)",                          // Title
      "The resource type (ArchivalResource, BibliographicResource, etc.)"     // Type
   ];
   
   for(var i = 0 ; i<tooltipsResource.length; i++) {
      var toolTipSpan = document.createElement("span");
      var toolTiptext = document.createTextNode(tooltipsResource[i]);
      toolTipSpan.classList.add("tooltiptext");
      toolTipSpan.appendChild(toolTiptext);
      console.log($('[id="dragResource"]')[i]);
      $('[id="dragResource"]')[i].appendChild(toolTipSpan);
   }

   //Reference Validator Call onChange
   const $selectsRef = $(".selectColumnRes");
   $selectsRef.on('change', function(){
      hideAndDisableRef();
   });

   //Constellation Validator Call onChange
   const $selectsConst = $(".selectColumnConst");
   $selectsConst.on('change', function(){
      hideAndDisableConst();
   });

   //Allow names column (first column) to be droppable
   $('.wbs-draggable-column').droppable({
      hoverClass: 'active',
      drop: function(event, ui) {
         var id = $(this).attr('id');
         // var matchingSelector = $('.selectColumn').children('option').closest('[value = "'+$(ui.draggable).text()+'"]')[id];
         // console.log(matchingSelector);
         // matchingSelector.selected = 'true';
         $('.selectColumn')[id].value = $(ui.draggable).val();
         hideAndDisableRef();
         hideAndDisableConst();
      }
   });

   //Allow dropdown column to be droppable
   $('.selectColumn').droppable({
      hoverClass: 'active',
      drop: function(event, ui) {
         this.value = $(ui.draggable).val();
         hideAndDisableRef();
         hideAndDisableConst();
      },
   });
   $('.wbs-reconciled-column').draggable({
      helper: "clone",
      cursor: "crosshair",
      snap: ".wbs-item-input input, .wbs-target-input input",
      zIndex: 100,
   });
   $('.wbs-unreconciled-column').draggable({
      helper: "clone",
      helper: function(e) {
         var original = $(e.target).hasClass("ui-draggable") ? $(e.target) :  $(e.target).closest(".ui-draggable");
         return original.clone().css({
         width: original.width() // or outerWidth*
         });
      },
      cursor: "crosshair",
      snap: ".wbs-target-input input",
      zIndex: 100,
   });
}

SNACSchemaAlignmentDialog.switchbuttons = function() {
   if (document.getElementById('resourcebutton').checked) {
      // print text
   }
   else if (document.getElementById('constellationbutton').checked) {
      // print text
   }
}

// SNACSchemaAlignmentDialog.addDropdowns = function() {
//    var columns = theProject.columnModel.columns;
//    var SNACcolumns = ["ID", "Type", "Title", "Display Entry", "Link", "Abstract", "Extent", "Date", "Language", "Holding Repository SNAC ID", "Note"];
//    this._dropdownArea = $(".schema-alignment-dialog-dropdown-area");
//    this._dropdownArea.addClass("snac-tab");

//    for (var i = 0; i < columns.length; i++) {
//       //Create and append select list
//       var selectList = document.createElement("select");
//       selectList.setAttribute("id", "mySelect");
//       this._dropdownArea.appendChild(selectList);

//       //Create and append the options
//       for (var j = 0; j < SNACcolumns.length; j++) {
//          var option = document.createElement("option");
//          option.setAttribute("value", SNACcolumns[j]);
//          option.text = SNACcolumns[j];
//          selectList.append(option);
//       }
//    }
// }

SNACSchemaAlignmentDialog.switchTab = function(targetTab) {
   $('.main-view-panel-tab').hide();
   $('.main-view-panel-tab-header').removeClass('active');
   $('.main-view-panel-tab-header[href="'+targetTab+'"]').addClass('active');
   $(targetTab).show();
   resizeAll();
   SNACSchemaAlignmentDialog.preview();
   var panelHeight = this._viewPanel.height();
   this._schemaPanel.height(panelHeight);
   this._issuesPanel.height(panelHeight);
   this._previewPanel.height(panelHeight);

   // Resize the inside of the schema panel
   var headerHeight = this._schemaElmts.schemaHeader.outerHeight();
   this._schemaElmts.canvas.height(panelHeight - headerHeight - 10);

   if (targetTab === "#view-panel") {
      ui.dataTableView.render();
   }
}

SNACSchemaAlignmentDialog.isSetUp = function() {
   return $('#snac-schema-panel').length !== 0;
}

SNACSchemaAlignmentDialog.launch = function(onDone) {
   this._onDone = onDone;
   this._hasUnsavedChanges = false;


   if (!SNACSchemaAlignmentDialog.isSetUp()) {
      SNACSchemaAlignmentDialog.setUpTabs();
   }

   $('.main-view-panel-tabs-snac').show();
   $('.main-view-panel-tabs-wiki').hide();

   SNACSchemaAlignmentDialog.switchTab('#snac-schema-panel');

   // this._createDialog();
}


var beforeUnload = function(e) {
   if (SNACSchemaAlignmentDialog.isSetUp() && SNACSchemaAlignmentDialog._hasUnsavedChanges === true) {
      return (e = $.i18n('snac-schema/unsaved-warning'));
   }
};

$(window).bind('beforeunload', beforeUnload);

SNACSchemaAlignmentDialog._reset = function(schema) {
   // this._originalSchema = schema || { itemDocuments: [] };
   // this._schema = cloneDeep(this._originalSchema); // this is what can be munched on
   // this._copiedReference = null;
   //
   // $('#schema-alignment-statements-container').empty();
   //
   // if (this._schema && this._schema.itemDocuments) {
   //   for(var i = 0; i != this._schema.itemDocuments.length; i++) {
   //     this._addItem(this._schema.itemDocuments[i]);
   //   }
   // }
   $.get(
      "command/snac/resource",
      function(data) {
         console.log("command/snac/resource");
         //console.log("Resource status: " + data.resource);
      }
   );
};

/*************************
 * CHECK FOR ERRORS & SAVE *
 *************************/

// Will be used for save & issues
var error_fields = [];

SNACSchemaAlignmentDialog._save = function(onDone) {
   var self = this;
   var schema = this.getJSON();

   if (schema === null) {
      alert($.i18n('snac-schema/incomplete-schema-could-not-be-saved'));
   }

   var columns = theProject.columnModel.columns;
   
   // This helps the Issue tab to differentiate between what issues it will look at for Resource vs Constellation
   if (document.getElementById('resourcebutton').checked) {
      var dropDownValues = document.getElementsByClassName('selectColumnRes');
   }
   else {
      var dropDownValues = document.getElementsByClassName('selectColumnConst');
   }

   //   var dropDownValues = document.getElementsByClassName('selectColumn');

   var array_ddv = [];
   for (var j = 0; j < dropDownValues.length; j++){
      array_ddv.push(dropDownValues[j].value);
   }

   // Empty required field check (for issues tab)
   // The required fields for Resource vs Constellation are different, so required_fields will be used to check whether all the fields in this array have been used
   if (document.getElementById('resourcebutton').checked) {
      var mainfields = ["ID", "Type", "Title", "Display Entry", "Link", "Abstract", "Extent", "Date", "Language", "Holding Repository SNAC ID", "Note", "Script"];
      var required_fields = ["Title", "Link", "Type", "Holding Repository SNAC ID"];
   }

   else {
      var mainfields = ["ID", "Entity Type", "Name Entry", "Surename", "Forename", "Exist Dates", "BiogHist", "Place", "Occupation", "Related Constellation IDs", "Related Resource IDs"];
      var required_fields = ["Entity Type", "Name Entry"];

   }

   // For printing to issues tab
   var empty_required = false;
   for (var x = 0; x < required_fields.length; x++){
      if (!(array_ddv.includes(required_fields[x]))){
         empty_required = true;
         error = {
            title: `'${required_fields[x]}' found empty`,
            body: `The required field '${required_fields[x]}' is missing from schema.`,
         };
         error_fields.push(error);
      }
   }

   // Duplicate field check (for issues tab)
   var dup_dict = {}
   var dup_bool = false;
   for (var y = 0; y < array_ddv.length; y++){
      if (array_ddv[y] == ""){
         continue;
      }
      if (!(array_ddv[y] in dup_dict)){
         dup_dict[array_ddv[y]] = 1;
      }
      else{
         if (mainfields.includes(array_ddv[y])) {
            dup_bool = true;
            error = {
               title: `Duplicate values of '${array_ddv[y]}'`,
               body: `Duplicate values found for '${array_ddv[y]}'.`,
            };
            error_fields.push(error);
         }
         else {
            continue;
         }
        
      }
   }


   console.log(theProject.columnModel.columns);
  // Save resource
  console.log(dropDownValues);

   if (!dup_bool && !empty_required){
      var dict = {};
      var columns = theProject.columnModel.columns;
      // console.log(columns);
      console.log(dropDownValues);
      console.log(columns);
      for (var i = 0; i != columns.length; i++){
         console.log(i);
         console.log(columns[i].name);
         dict[columns[i].name] = dropDownValues[i].value;
      }
      $.post(
         "command/snac/resource",
         {
            "dict": JSON.stringify(dict),
            "project": JSON.stringify(theProject.id)
         },
         function(data, status) {
            console.log("Resource status: " + data.resource);
         }
      );
   }
   SNACSchemaAlignmentDialog._hasChanged();
};

SNACSchemaAlignmentDialog._discardChanges = function() {
   this._reset(theProject.overlayModels.wikibaseSchema);
   this._changesCleared();
}

SNACSchemaAlignmentDialog._changesCleared = function() {
   this._hasUnsavedChanges = false;
   this._unsavedIndicator.hide();
   this._schemaElmts.saveButton
      .prop('disabled', true)
      .addClass('disabled');
   this._schemaElmts.discardButton
      .prop('disabled', true)
      .addClass('disabled');
}

//format cells for columns
SNACSchemaAlignmentDialog._createDraggableColumn = function(name, reconciled, org) {
   var cell = $("<div></div>").addClass('wbs-draggable-column').text(name);
   if (reconciled) {
      cell.addClass('wbs-reconciled-column');
   } else {
      cell.addClass('wbs-unreconciled-column');
   }
   // cell.addClass(org);

   // cell.addClass(columnType);
  return cell;
}

SNACSchemaAlignmentDialog._plusButton = function(label, element) {
   var plus = $('<b></b>').html('+&nbsp;').appendTo(element);
   var span = $('<span></span>').text(label)
      .appendTo(element);
}

SNACSchemaAlignmentDialog._makeDeleteButton = function (noText) {
   var button = $('<div></div>').addClass('wbs-remove').append(
      $('<span></span>').addClass('wbs-icon')
   );
   if(noText === undefined) {
      button.append(
      $('<span></span>').text($.i18n('snac-schema/remove')));
   }
   return button;
}

/**************/
/*** ITEMS ****/
/**************/

SNACSchemaAlignmentDialog._addItem = function(json) {
   var subject = null;
   var statementGroups = null;
   var nameDescs = null;
   if (json) {
      subject = json.subject;
      statementGroups = json.statementGroups;
      nameDescs = json.nameDescs;
   }

   var item = $('<div></div>').addClass('wbs-item');
   $('#schema-alignment-statements-container').append(item);
   var deleteToolbar = $('<div></div>').addClass('wbs-toolbar')
      .attr('style', 'margin-top: 10px')
      .appendTo(item);
   var deleteButton = SNACSchemaAlignmentDialog._makeDeleteButton()
      .appendTo(deleteToolbar)
      .click(function(e) {
         item.remove();
         SNACSchemaAlignmentDialog._hasChanged();
         e.preventDefault();
      });
  var inputContainer = $('<div></div>').addClass('wbs-item-input').appendTo(item);
  SNACSchemaAlignmentDialog._initField(inputContainer, "wikibase-item", subject);
  var right = $('<div></div>').addClass('wbs-item-contents').appendTo(item);

   // Terms
   $('<span></span>').addClass('wbs-namedesc-header')
      .text($.i18n('snac-schema/terms-header')).appendTo(right);
   $('<div></div>').addClass('wbs-namedesc-container')
      .attr('data-emptyplaceholder', $.i18n('snac-schema/empty-terms'))
      .appendTo(right);
   var termToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
   var addNamedescButton = $('<a></a>').addClass('wbs-add-namedesc')
   .click(function(e) {
      SNACSchemaAlignmentDialog._addNameDesc(item, null);
      e.preventDefault();
   }).appendTo(termToolbar);
   SNACSchemaAlignmentDialog._plusButton(
      $.i18n('snac-schema/add-term'), addNamedescButton);

   // Clear the float
   $('<div></div>').attr('style', 'clear: right').appendTo(right);

   // Statements
   $('<div></div>').addClass('wbs-statements-header')
      .text($.i18n('snac-schema/statements-header')).appendTo(right);
   $('<div></div>').addClass('wbs-statement-group-container')
      .attr('data-emptyplaceholder', $.i18n('snac-schema/empty-statements'))
      .appendTo(right);
   var statementToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
   var addStatementButton = $('<a></a>').addClass('wbs-add-statement-group')
      .click(function(e) {
         SNACSchemaAlignmentDialog._addStatementGroup(item, null);
         e.preventDefault();
      }).appendTo(statementToolbar);

   SNACSchemaAlignmentDialog._plusButton(
      $.i18n('snac-schema/add-statement'), addStatementButton);

   if (statementGroups) {
      for(var i = 0; i != statementGroups.length; i++) {
         SNACSchemaAlignmentDialog._addStatementGroup(item, statementGroups[i]);
      }
   }

   if (nameDescs) {
      for(var i = 0; i != nameDescs.length; i++) {
         SNACSchemaAlignmentDialog._addNameDesc(item, nameDescs[i]);
      }
   }
}

SNACSchemaAlignmentDialog._itemToJSON = function (item) {
   var statementGroupLst = new Array();
   var statementsDom = item.find('.wbs-statement-group');
   statementsDom.each(function () {
      var statementGroupJSON = SNACSchemaAlignmentDialog._statementGroupToJSON($(this));
      if (statementGroupJSON !== null) {
         statementGroupLst.push(statementGroupJSON);
      }
   });
   var nameDescLst = new Array();
   var nameDescsDom = item.find('.wbs-namedesc');
   nameDescsDom.each(function () {
      var nameDescJSON = SNACSchemaAlignmentDialog._nameDescToJSON($(this));
      if (nameDescJSON !== null) {
         nameDescLst.push(nameDescJSON);
      }
   });
   var inputContainer = item.find(".wbs-item-input").first();
   var subjectJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(inputContainer);
   if (subjectJSON !== null &&
      statementGroupLst.length === statementsDom.length &&
      nameDescLst.length === nameDescsDom.length) {
   return {subject: subjectJSON,
      statementGroups: statementGroupLst,
      nameDescs: nameDescLst};
   } else {
      return null;
   }
};

/**************************
 * NAMES AND DESCRIPTIONS *
 **************************/

SNACSchemaAlignmentDialog._addNameDesc = function(item, json) {
   var type = 'ALIAS';
   var value = null;
   if (json) {
      type = json.name_type;
      value = json.value;
   }

   var container = item.find('.wbs-namedesc-container').first();
   var namedesc = $('<div></div>').addClass('wbs-namedesc').appendTo(container);
   var type_container = $('<div></div>').addClass('wbs-namedesc-type').appendTo(namedesc);
   var type_input = $('<select></select>').appendTo(type_container);
   $('<option></option>')
      .attr('value', 'LABEL')
      .text($.i18n('snac-schema/label'))
      .appendTo(type_input);
   $('<option></option>')
      .attr('value', 'DESCRIPTION')
      .text($.i18n('snac-schema/description'))
      .appendTo(type_input);
   $('<option></option>')
      .attr('value', 'ALIAS')
      .text($.i18n('snac-schema/alias'))
      .appendTo(type_input);
   type_input.val(type);
   type_input.on('change', function(e) {
      SNACSchemaAlignmentDialog._hasChanged();
   });

   var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(namedesc);
   SNACSchemaAlignmentDialog._makeDeleteButton().click(function(e) {
      namedesc.remove();
      SNACSchemaAlignmentDialog._hasChanged();
      e.preventDefault();
   }).appendTo(toolbar);

   var right = $('<div></div>').addClass('wbs-right').appendTo(namedesc);
   var value_container = $('<div></div>').addClass('wbs-namedesc-value').appendTo(namedesc);
   SNACSchemaAlignmentDialog._initField(value_container, "monolingualtext", value);
}

SNACSchemaAlignmentDialog._nameDescToJSON = function (namedesc) {
   var type = namedesc.find('select').first().val();
   var value = namedesc.find('.wbs-namedesc-value').first().data("jsonValue");
   return {
      type: "wbnamedescexpr",
      name_type: type,
      value: value,
   };
}

/********************
 * STATEMENT GROUPS *
 ********************/

SNACSchemaAlignmentDialog._addStatementGroup = function(item, json) {
   var property = null;
   var statements = null;
   if (json) {
      property = json.property;
      statements = json.statements;
   }

   var container = item.find('.wbs-statement-group-container').first();
   var statementGroup = $('<div></div>').addClass('wbs-statement-group');
   var inputContainer = $('<div></div>').addClass('wbs-prop-input').appendTo(statementGroup);
   var right = $('<div></div>').addClass('wbs-right').appendTo(statementGroup);
   var statementContainer = $('<div></div>').addClass('wbs-statement-container').appendTo(right);
   SNACSchemaAlignmentDialog._initPropertyField(inputContainer, statementContainer, property);
   var toolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
   var addValueButton = $('<a></a>').addClass('wbs-add-statement').click(function(e) {
      var datatype = inputContainer.data("jsonValue").datatype;
      SNACSchemaAlignmentDialog._addStatement(statementContainer, datatype, null);
      e.preventDefault();
   }).appendTo(toolbar).hide();
   SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-value'), addValueButton);
   var removeButton = SNACSchemaAlignmentDialog._makeDeleteButton()
      .addClass('wbs-remove-statement-group')
      .appendTo(toolbar)
      .click(function(e) {
         statementGroup.remove();
         e.preventDefault();
      });

   container.append(statementGroup);

   if (statements) {
      for (var i = 0; i != statements.length; i++) {
         SNACSchemaAlignmentDialog._addStatement(statementContainer, property.datatype, statements[i]);
         addValueButton.show();
         removeButton.hide();
      }
   } else {
      inputContainer.find('input').focus();
   }
}

SNACSchemaAlignmentDialog._statementGroupToJSON = function (statementGroup) {
   var lst = new Array();
   var domStatements = statementGroup.find('.wbs-statement-container').first().children('.wbs-statement');
   domStatements.each(function () {
      var statementJSON = SNACSchemaAlignmentDialog._statementToJSON($(this));
      if (statementJSON !== null) {
         lst.push(statementJSON);
      }
   });
   var inputContainer = statementGroup.find(".wbs-prop-input").first();
   var propertyJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(inputContainer);
   if (propertyJSON !== null && domStatements.length === lst.length && lst.length > 0) {
      return {property: propertyJSON,
         statements: lst
      };
   } else {
      return null;
   }
};

/**************
 * STATEMENTS *
 **************/

SNACSchemaAlignmentDialog._addStatement = function(container, datatype, json) {
   var qualifiers = null;
   var references = null;
   var value = null;
   if (json) {
      qualifiers = json.qualifiers;
      references = json.references;
      value = json.value;
   }

   var statement = $('<div></div>').addClass('wbs-statement');
   var inputContainer = $('<div></div>').addClass('wbs-target-input').appendTo(statement);
   SNACSchemaAlignmentDialog._initField(inputContainer, datatype, value);

   // If we are in a mainsnak...
   // (see https://www.mediawiki.org/wiki/Wikibase/DataModel#Snaks)
   if (container.parents('.wbs-statement').length == 0) {
      // add delete button
      var toolbar1 = $('<div></div>').addClass('wbs-toolbar').appendTo(statement);
      SNACSchemaAlignmentDialog._makeDeleteButton().click(function(e) {
         SNACSchemaAlignmentDialog._removeStatement(statement);
         e.preventDefault();
      }).appendTo(toolbar1);

      // add rank
      var rank = $('<div></div>').addClass('wbs-rank-selector-icon').prependTo(inputContainer);

      // add qualifiers...
      var right = $('<div></div>').addClass('wbs-right').appendTo(statement);
      var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);
      var toolbar2 = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
      var addQualifierButton = $('<a></a>').addClass('wbs-add-qualifier')
         .click(function(e) {
         SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, null);
         e.preventDefault();
      }).appendTo(toolbar2);
      SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-qualifier'), addQualifierButton);

      if (qualifiers) {
         for (var i = 0; i != qualifiers.length; i++) {
            SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, qualifiers[i]);
         }
      }

      // and references
      $('<div></div>').attr('style', 'clear: right').appendTo(statement);
      var referencesToggleContainer = $('<div></div>').addClass('wbs-references-toggle').appendTo(statement);
      var triangle = $('<div></div>').addClass('triangle-icon').addClass('pointing-right').appendTo(referencesToggleContainer);
      var referencesToggle = $('<a></a>').appendTo(referencesToggleContainer);
      right = $('<div></div>').addClass('wbs-right').appendTo(statement);
      var referenceContainer = $('<div></div>').addClass('wbs-reference-container').appendTo(right);
      referencesToggleContainer.click(function(e) {
         triangle.toggleClass('pointing-down');
         triangle.toggleClass('pointing-right');
         referenceContainer.toggle(100);
         e.preventDefault();
      });
      referenceContainer.hide();
      var right2 = $('<div></div>').addClass('wbs-right').appendTo(right);
      var toolbar3 = $('<div></div>').addClass('wbs-toolbar').appendTo(right2);
      var addReferenceButton = $('<a></a>').addClass('wbs-add-reference')
         .click(function(e) {
         referenceContainer.show();
         SNACSchemaAlignmentDialog._addReference(referenceContainer, null);
         SNACSchemaAlignmentDialog._updateReferencesNumber(referenceContainer);
         e.preventDefault();
      }).appendTo(toolbar3);
      SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-reference'), addReferenceButton);

      var pasteToolbar = $('<div></div>').addClass('wbs-toolbar').appendTo(referencesToggleContainer);
      var referencePaste = $('<span></span>')
         .addClass('wbs-paste-reference')
         .appendTo(pasteToolbar);
      if (SNACSchemaAlignmentDialog._copiedReference === null) {
         referencePaste.hide();
      }
      var pasteIcon = $('<span></span>').addClass('wbs-icon').appendTo(referencePaste);
      var referencePasteButton = $('<a></a>')
         .addClass('wbs-paste-reference-button')
         .text($.i18n('snac-schema/paste-reference'))
         .appendTo(referencePaste)
         .click(function(e) {
         if (SNACSchemaAlignmentDialog._copiedReference !== null) {
            SNACSchemaAlignmentDialog._addReference(referenceContainer, SNACSchemaAlignmentDialog._copiedReference);
            SNACSchemaAlignmentDialog._updateReferencesNumber(referenceContainer);
            referencePaste.hide();
            SNACSchemaAlignmentDialog._hasChanged();
         }
         e.preventDefault();
         e.stopPropagation();
      });

      if (references) {
         for (var i = 0; i != references.length; i++) {
            SNACSchemaAlignmentDialog._addReference(referenceContainer, references[i]);
         }
      }
      SNACSchemaAlignmentDialog._updateReferencesNumber(referenceContainer);
   }
   container.append(statement);
}

SNACSchemaAlignmentDialog._statementToJSON = function (statement) {
   var inputContainer = statement.find(".wbs-target-input").first();
   var qualifiersList = new Array();
   var referencesList = new Array();
   var qualifiersDom = statement.find('.wbs-qualifier-container').first().children();
   qualifiersDom.each(function () {
      var qualifierJSON = SNACSchemaAlignmentDialog._qualifierToJSON($(this));
      if (qualifierJSON !== null) {
         qualifiersList.push(qualifierJSON);
      }
   });
   var referencesDom = statement.find('.wbs-reference-container').first().children();
   referencesDom.each(function () {
      var referenceJSON = SNACSchemaAlignmentDialog._referenceToJSON($(this));
      if (referenceJSON !== null) {
         referencesList.push(referenceJSON);
      }
   });
   var valueJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(inputContainer);
   if (referencesList.length === referencesDom.length &&
      qualifiersList.length === qualifiersDom.length &&
      valueJSON !== null) {
      return {
         value: valueJSON,
         qualifiers: qualifiersList,
         references: referencesList,
      };
   } else {
      return null;
   }
};

/**************
 * QUALIFIERS *
 **************/

SNACSchemaAlignmentDialog._addQualifier = function(container, json) {
   var property = null;
   var value = null;
   if (json) {
      property = json.prop;
      value = json.value;
   }

   var qualifier = $('<div></div>').addClass('wbs-qualifier').appendTo(container);
   var toolbar1 = $('<div></div>').addClass('wbs-toolbar').appendTo(qualifier);
   var inputContainer = $('<div></div>').addClass('wbs-prop-input').appendTo(qualifier);
   var right = $('<div></div>').addClass('wbs-right').appendTo(qualifier);
   var deleteButton = SNACSchemaAlignmentDialog._makeDeleteButton()
      .addClass('wbs-remove-statement-group')
      .appendTo(toolbar1).click(function(e) {
         qualifier.remove();
         SNACSchemaAlignmentDialog._hasChanged();
         e.preventDefault();
      });
   var statementContainer = $('<div></div>').addClass('wbs-statement-container').appendTo(right);
   SNACSchemaAlignmentDialog._initPropertyField(inputContainer, statementContainer, property);
   if (value && property) {
      SNACSchemaAlignmentDialog._addStatement(statementContainer, property.datatype, {value:value});
   } else {
      inputContainer.find('input').focus();
   }
}

SNACSchemaAlignmentDialog._qualifierToJSON = function(elem) {
   var prop = elem.find(".wbs-prop-input").first();
   var target = elem.find(".wbs-target-input").first();
   var propJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(prop);
   var valueJSON = SNACSchemaAlignmentDialog._inputContainerToJSON(target);
   if (propJSON !== null && valueJSON !== null) {
      return {
         prop: propJSON,
         value: valueJSON,
      };
   } else {
      return null;
   }
}

/**************
 * REFERENCES *
 **************/

SNACSchemaAlignmentDialog._addReference = function(container, json) {
   var snaks = null;
   if (json) {
      snaks = json.snaks;
   }

   var reference = $('<div></div>').addClass('wbs-reference').appendTo(container);
   var referenceHeader = $('<div></div>').addClass('wbs-reference-header').appendTo(reference);
   var referenceCopy = $('<span></span>').addClass('wbs-copy-reference').appendTo(referenceHeader);
   var referenceCopyIcon = $('<span></span>').addClass('wbs-icon').appendTo(referenceCopy);
   var copyButton = $('<span></span>')
      .addClass('wbs-copy-reference-button')
      .text($.i18n('snac-schema/copy-reference'))
      .appendTo(referenceCopy)
      .click(function(e) {
         if (SNACSchemaAlignmentDialog._copyReference(reference)) {
            $(this).text($.i18n('snac-schema/reference-copied'))
                  .parent().addClass('wbs-copied-reference');
            container.parent().parent().find('.wbs-paste-reference').hide();
         }
         e.preventDefault();
      });
   var toolbarRef = $('<div></div>').addClass('wbs-toolbar').appendTo(referenceHeader);
   SNACSchemaAlignmentDialog._makeDeleteButton().click(function(e) {
      reference.remove();
      SNACSchemaAlignmentDialog._updateReferencesNumber(container);
      SNACSchemaAlignmentDialog._hasChanged();
      e.preventDefault();
   }).appendTo(toolbarRef);
   var right = $('<div></div>').addClass('wbs-right').appendTo(reference);
   var qualifierContainer = $('<div></div>').addClass('wbs-qualifier-container').appendTo(right);
   var toolbar2 = $('<div></div>').addClass('wbs-toolbar').appendTo(right);
   var addSnakButton = $('<a></a>').addClass('wbs-add-qualifier')
      .click(function(e) {
         SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, null);
         e.preventDefault();
      }).appendTo(toolbar2);
   SNACSchemaAlignmentDialog._plusButton($.i18n('snac-schema/add-reference-snak'), addSnakButton);

   if (snaks) {
      for (var i = 0; i != snaks.length; i++) {
         SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, snaks[i]);
      }
   } else {
      SNACSchemaAlignmentDialog._addQualifier(qualifierContainer, null);
   }
}

SNACSchemaAlignmentDialog._referenceToJSON = function(reference) {
   var snaks = reference.find('.wbs-qualifier-container').first().children();
   var snaksList = new Array();
   snaks.each(function () {
      var qualifier = SNACSchemaAlignmentDialog._qualifierToJSON($(this));
      if (qualifier !== null) {
         snaksList.push(qualifier);
      }
   });
   if (snaksList.length === snaks.length) {
      return {snaks:snaksList};
   } else {
      return null;
   }
}

SNACSchemaAlignmentDialog._updateReferencesNumber = function(container) {
   var childrenCount = container.children().length;
   var statement = container.parents('.wbs-statement');
   var a = statement.find('.wbs-references-toggle a').first();
   a.html(childrenCount+$.i18n('snac-schema/nb-references'));
}

SNACSchemaAlignmentDialog._copyReference = function(reference) {
   // mark any other copied reference as not copied
   $('.wbs-copy-reference-button')
      .text($.i18n('snac-schema/copy-reference'));
   $('.wbs-copy-reference')
      .removeClass('wbs-copied-reference');
   var copiedReference = SNACSchemaAlignmentDialog._referenceToJSON(reference);
   if (copiedReference !== null) {
      SNACSchemaAlignmentDialog._copiedReference = copiedReference;
      $('.wbs-paste-reference').show();
      return true;
   } else {
      return false;
   }
}

/************************
 * FIELD INITIALIZATION *
 ************************/

SNACSchemaAlignmentDialog._getPropertyType = function(pid, callback) {
   $.ajax({
      url:'https://www.snac.org/w/api.php',
      data: {
         action: "wbgetentities",
         format: "json",
         ids: pid,
         props: "datatype",
      },
      dataType: "jsonp",
      success: function(data) {
         callback(data.entities[pid].datatype);
      }
   });
}

SNACSchemaAlignmentDialog._initPropertyField = function(inputContainer, targetContainer, initialValue) {
   var input = $('<input></input>').appendTo(inputContainer);
   input.attr("placeholder", $.i18n('snac-schema/property-placeholder'));

   if (this._reconService !== null) {
      endpoint = this._reconService.suggest.property;
      var suggestConfig = $.extend({}, endpoint);
      suggestConfig.key = null;
      suggestConfig.query_param_name = "prefix";

      input.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
         // Fetch the type of this property and add the appropriate target value type
         var statementGroup = inputContainer.parents(".wbs-statement-group, .wbs-qualifier").first();
         SNACSchemaAlignmentDialog._getPropertyType(data.id, function(datatype) {
            inputContainer.data("jsonValue", {
               type : "wbpropconstant",
               pid : data.id,
               label: data.name,
               datatype: datatype,
            });
            SNACSchemaAlignmentDialog._addStatement(targetContainer, datatype, null);
            var addValueButtons = targetContainer.parent().find('.wbs-add-statement');
            var removeGroupButton = targetContainer.parent().find('.wbs-remove-statement-group');
            removeGroupButton.hide();
            addValueButtons.show();
         });
         SNACSchemaAlignmentDialog._hasChanged();
      }).bind("fb-textchange", function(evt, data) {
         inputContainer.data("jsonValue", null);
         targetContainer.find('.wbs-statement').remove();
         var addValueButtons = targetContainer.parent().find('.wbs-add-statement');
         var removeGroupButton = targetContainer.parent().find('.wbs-remove-statement-group');
         addValueButtons.hide();
         removeGroupButton.show();
      });
      // adds tweaks to display the validation status more clearly, like in snac
      fixSuggestInput(input);
   }

   // Init with the provided initial value.
   if (initialValue) {
      if (initialValue.type === "wbpropconstant") {
         input.val(initialValue.label);
         input.addClass('wbs-validated-input');
      }
      inputContainer.data("jsonValue", initialValue);
   }
}

SNACSchemaAlignmentDialog._initField = function(inputContainer, mode, initialValue, changedCallback) {
  var input = $('<input></input>').appendTo(inputContainer);

   if (! changedCallback) {
      changedCallback = SNACSchemaAlignmentDialog._hasChanged;
   }

   if (this._reconService !== null && (mode === "wikibase-item" || mode === "unit")) {
      if (mode === "wikibase-item") {
         input.attr("placeholder", $.i18n('snac-schema/item-or-reconciled-column'));
      } else {
         input.attr("placeholder", $.i18n('snac-schema/unit'));
      }
      var endpoint = null;
      endpoint = this._reconService.suggest.entity;
      var suggestConfig = $.extend({}, endpoint);
      suggestConfig.key = null;
      suggestConfig.query_param_name = "prefix";
      if ('view' in this._reconService && 'url' in this._reconService.view && !('view_url' in endpoint)) {
         suggestConfig.view_url = this._reconService.view.url;
      }

      input.suggest(suggestConfig).bind("fb-select", function(evt, data) {
         inputContainer.data("jsonValue", {
            type : "wbitemconstant",
            qid : data.id,
            label: data.name,
         });
         changedCallback();
      });
      // adds tweaks to display the validation status more clearly, like in snac
      fixSuggestInput(input);

   } else if (this._reconService !== null && mode === "wikibase-property") {
      var endpoint = null;
      endpoint = this._reconService.suggest.property;
      var suggestConfig = $.extend({}, endpoint);
      suggestConfig.key = null;
      suggestConfig.query_param_name = "prefix";

      input.suggestP(suggestConfig).bind("fb-select", function(evt, data) {
         inputContainer.data("jsonValue", {
            type : "wbpropconstant",
            pid : data.id,
            label: data.name,
            datatype: "not-important",
         });
         changedCallback();
      });
    // adds tweaks to display the validation status more clearly, like in snac
    fixSuggestInput(input);
   } else if (mode === "time") {
      input.attr("placeholder", "YYYY(-MM(-DD))");
      var propagateValue = function(val) {
         // TODO add validation here
         inputContainer.data("jsonValue", {
            type: "wbdateconstant",
            value: val,
         });
      };
      propagateValue("");
      input.change(function() {
         propagateValue($(this).val());
         changedCallback();
      });
      SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^\d{4}(-[0-1]\d(-[0-3]\d)?)?$/);
   } else if (mode === "globe-coordinate") {
      input.attr("placeholder", "lat,lon");
      var propagateValue = function(val) {
         // TODO add validation here
         inputContainer.data("jsonValue", {
            type: "wblocationconstant",
            value: val,
         });
      };
      propagateValue("");
      input.change(function() {
         propagateValue($(this).val());
         changedCallback();
      });
      SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^[\-+]?\d+(\.\d*)?[,\/][\-+]?\d+(\.\d*)?([,\/]\d+(\.\d*)?)?$/);
   } else if (mode === "language") {
      input.attr("placeholder", "lang");
      input.addClass("wbs-language-input");
      input.langsuggest().bind("fb-select", function(evt, data) {
         inputContainer.data("jsonValue", {
               type: "wblanguageconstant",
               id: data.id,
               label: data.name,
         });
         changedCallback();
      });
      fixSuggestInput(input);
   } else if (mode === "monolingualtext") {
      input.remove();
      var inputContainerLanguage = $('<div></div>')
      .addClass('wbs-monolingual-container')
      .width('30%')
      .appendTo(inputContainer);
      var inputContainerValue = $('<div></div>')
      .addClass('wbs-monolingual-container')
      .width('70%')
      .appendTo(inputContainer);

      var langValue = null;
      var strValue = null;
      if (initialValue) {
         langValue = initialValue.language;
         strValue = initialValue.value;
      }

      var propagateValue = function() {
         inputContainer.data("jsonValue", {
            type: "wbmonolingualexpr",
            language: inputContainerLanguage.data("jsonValue"),
            value: inputContainerValue.data("jsonValue"),
         });
         changedCallback();
      }
      SNACSchemaAlignmentDialog._initField(inputContainerLanguage, "language", langValue, propagateValue);
      SNACSchemaAlignmentDialog._initField(inputContainerValue, "string", strValue, propagateValue);
   } else if (mode === "quantity") {
      input.remove();
      var inputContainerAmount = $('<div></div>')
         .addClass('wbs-quantity-container')
         .width('60%')
         .appendTo(inputContainer);
      var inputContainerUnit = $('<div></div>')
         .addClass('wbs-quantity-container')
         .width('40%')
         .appendTo(inputContainer);

      var amountValue = null;
      var unitValue = null;
      if (initialValue) {
         amountValue = initialValue.amount;
         unitValue = initialValue.unit;
      }

      var propagateValue = function() {
         inputContainer.data("jsonValue", {
            type: "wbquantityexpr",
            amount: inputContainerAmount.data("jsonValue"),
            unit: inputContainerUnit.data("jsonValue"),
         });
         changedCallback();
      }
      SNACSchemaAlignmentDialog._initField(inputContainerAmount, "amount", amountValue, propagateValue);
      SNACSchemaAlignmentDialog._initField(inputContainerUnit, "unit", unitValue, propagateValue);
   } else {
      var propagateValue = function(val) {
         inputContainer.data("jsonValue", {
            type: "wbstringconstant",
            value: val,
         });
      };
      propagateValue("");
      input.change(function() {
         propagateValue($(this).val());
         changedCallback();
      });
      if (mode === "amount") {
         input.attr("placeholder", $.i18n('snac-schema/amount'));
         SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^[\-+]?\d+(\.\d*)?(E[\-+]\d+)?$/);
      } else if (mode === "url") {
         input.attr("placeholder", $.i18n('snac-schema/full-url'));
         SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^https?:\/\/.+$/);
      } else if (mode === "tabular-data") {
         input.attr("placeholder", $.i18n('snac-schema/tabular-data-with-prefix'));
         SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^Data:.+$/);
      } else if (mode === "commonsMedia") {
         input.attr("placeholder", $.i18n('snac-schema/commons-media'));
      } else if (mode === "math") {
         input.attr("placeholder", $.i18n('snac-schema/math-expression'));
      } else if (mode === "geo-shape") {
         input.attr("placeholder", $.i18n('snac-schema/geoshape-with-prefix'));
         SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^Data:.+$/);
      } else {
         SNACSchemaAlignmentDialog.setupStringInputValidation(input, /^.+$/);
      }
      if (mode !== "external-id" &&
         mode !== "url" &&
         mode !== "string" &&
         mode !== "amount" &&
         mode !== "tabular-data" &&
         mode !== "commonsMedia" &&
         mode !== "geo-shape" &&
         mode !== "math") {
            alert($.i18n('snac-schema/datatype-not-supported-yet'));
      }
   }

   var acceptDraggableColumn = function(column) {
      input.hide();
      input.val("");
      var columnDiv = $('<div></div>').appendTo(inputContainer);
      column.appendTo(columnDiv);
      var origText = column.text();
      column.text("");
      column.append($('<div></div>').addClass('wbs-restricted-column-name').text(origText));
      var deleteButton = SNACSchemaAlignmentDialog._makeDeleteButton(true).appendTo(column);
      deleteButton.attr('alt', $.i18n('snac-schema/remove-column'));
      deleteButton.click(function (e) {
         columnDiv.remove();
         input.show();
         inputContainer.data("jsonValue", null);
         changedCallback();
         e.preventDefault();
      });
   };

   // Make it droppable
   var acceptClass = ".wbs-draggable-column";
   var wbVariableType = "wbstringvariable";
   if (mode === "wikibase-item" || mode === "unit") {
      acceptClass = ".wbs-reconciled-column";
      wbVariableType = "wbitemvariable";
   } else if (mode === "time") {
      wbVariableType = "wbdatevariable";
   } else if (mode === "globe-coordinate") {
      wbVariableType = "wblocationvariable";
   } else if (mode === "monolingualtext" || mode === "quantity") {
      wbVariableType = null; // not droppable directly
   } else if (mode === "language") {
      wbVariableType = "wblanguagevariable";
   }

   if (wbVariableType) {
      inputContainer.droppable({
         accept: acceptClass,
      }).on("drop", function (evt, ui) {
         var column = ui.draggable.clone();
         acceptDraggableColumn(column);
         inputContainer.data("jsonValue", {
            type : wbVariableType,
            columnName: ui.draggable.text(),
         });
         changedCallback();
         return true;
      }).on("dropactivate", function(evt, ui) {
         input.addClass("wbs-accepting-input");
      }).on("dropdeactivate", function(evt, ui) {
         input.removeClass("wbs-accepting-input");
      });
   }

   // Init with the provided initial value.
   if (initialValue) {
      if (initialValue.type === "wbitemconstant" || initialValue.type === "wbpropconstant") {
         input.val(initialValue.label);
         input.addClass("wbs-validated-input");
      } else if (initialValue.type == "wbitemvariable") {
         var cell = SNACSchemaAlignmentDialog._createDraggableColumn(initialValue.columnName, true);
         acceptDraggableColumn(cell);
      } else if (initialValue.type === "wbstringconstant" ||
         initialValue.type === "wbdateconstant" ||
         initialValue.type === "wblocationconstant") {
            input.val(initialValue.value);
      } else if (initialValue.type === "wblanguageconstant") {
         input.val(initialValue.id);
         input.addClass("wbs-validated-input");
      } else if (initialValue.type === "wbstringvariable" ||
         initialValue.type === "wbdatevariable" ||
         initialValue.type === "wblocationvariable" ||
         initialValue.type === "wblanguagevariable") {
            var cell = SNACSchemaAlignmentDialog._createDraggableColumn(initialValue.columnName, false);
            acceptDraggableColumn(cell);
      }
      inputContainer.data("jsonValue", initialValue);
   }
}

SNACSchemaAlignmentDialog.setupStringInputValidation = function(input, regex) {
   input.focus(function() {
      input.removeClass('wbs-unvalidated-input');
   }).blur(function() {
      var currentValue = input.val();
      if (regex.test(currentValue)) {
         input.addClass('wbs-validated-input');
      } else {
         input.addClass('wbs-unvalidated-input');
      }
   });
}

SNACSchemaAlignmentDialog._inputContainerToJSON = function (inputContainer) {
   var data = inputContainer.data();
   if (data && 'jsonValue' in data) {
      return data.jsonValue;
   } else {
      return null;
   }
};

SNACSchemaAlignmentDialog._removeStatement = function(statement) {
   var statementGroup = statement.parents('.wbs-statement-group, .wbs-qualifier').first();
   statement.remove();
   var remainingStatements = statementGroup.find('.wbs-statement').length;
   if (remainingStatements === 0) {
      statementGroup.remove();
   }
   SNACSchemaAlignmentDialog._hasChanged();
}

SNACSchemaAlignmentDialog.getJSON = function() {
   var list = new Array();
   var itemsDom = $('#schema-alignment-statements-container .wbs-item');
   itemsDom.each(function () {
      var itemJSON = SNACSchemaAlignmentDialog._itemToJSON($(this));
      if (itemJSON !== null) {
         list.push(itemJSON);
      }
   });
   if (list.length === itemsDom.length) {
      return {
         'itemDocuments': list,
         'wikibasePrefix': this._wikibasePrefix,
      };
   } else {
      return null;
   }
};

// Update everything when schema has changed
SNACSchemaAlignmentDialog._hasChanged = function() {
   SNACSchemaAlignmentDialog._hasUnsavedChanges = true;
   SNACSchemaAlignmentDialog.issues();
   SNACSchemaAlignmentDialog.preview();
   SNACSchemaAlignmentDialog._unsavedIndicator.show();
   SNACSchemaAlignmentDialog._schemaElmts.saveButton
      .prop('disabled', false)
      .removeClass('disabled');
   SNACSchemaAlignmentDialog._schemaElmts.discardButton
      .prop('disabled', false)
      .removeClass('disabled');
   $('.wbs-copy-reference-button')
      .text($.i18n('snac-schema/copy-reference'));
   $('.wbs-copy-reference')
      .removeClass('wbs-copied-reference');
}

SNACSchemaAlignmentDialog.updateNbEdits = function(nb_edits) {
   this._previewElmts.previewExplanation.text(
      nb_edits);
}

/*************************
 *  ISSUES TAB RENDERING *
 *************************/
/*global variable used by menu-bar-extension.js in order to check whether or not there are issues in the schema.
If there are errors, user will not be able to upload to SNAC*/
var validationCount = 0;

SNACSchemaAlignmentDialog.issues = function() {
   this.issueSpinner.show();
   var schema = this.getJSON();

   if(schema == null){
      return;
   }
   this.issueSpinner.hide();
   $('.invalid-schema-warning').hide();
   if(error_fields.length != 0){
      this._updateWarnings(error_fields, error_fields.length);
      validationCount = this._updateWarnings(error_fields, error_fields.length);
      error_fields = [];
   } else {
      validationCount = this._updateWarnings([],0);
      this._updateWarnings([],0);
   }
}

/*************************
 * PREVIEW TAB RENDERING *
 *************************/

SNACSchemaAlignmentDialog.preview = function() {
  var self = this;

  this._previewPanes.empty();
  this.updateNbEdits(0);
  this.previewSpinner.show();
  var schema = this.getJSON();
  if (schema === null) {
    $('.invalid-schema-warning').show();
    return;
  }
  $.get(
      "command/snac/preview-snac-schema", //+ $.param({ project: theProject.id }),
      function(data) {
        self.previewSpinner.hide();
        self.updateNbEdits(data.SNAC_preview);
        console.log("edits should be made here");

        var list = []; //Empty Array
        var line = data.SNAC_preview.split('\n'); //Split the preview string into lines
        var building = line[0] + "<br>"; //First element in preview string (should be "Inserting 500 new Resources into SNAC.")
        line.shift(); //remove that first element ("Inserting 500 new Resources into SNAC.")

        //Remove any empty strings
        line = line.filter(function(str) {
           return /\S/.test(str);
        });

        //Fill the list array with each line in HTML list form
        for(var i = 0; i<line.length; i++) {
           var line_parts = line[i].split(/:(.+)/); //Split on the first colon
           list[i] = "<li><b>" + line_parts[0] + ":</b> " + line_parts[1] + "</li>";
        }

        //Find the max length of items in the list[] array
        var max = list.reduce((r,s) => r > s.length ? r : s.length, 0);

        //Construct a divder string of "-" to be the size of the longest element in list[]
        var divider = "";
        for(var i=9; i<max/2; i++) {
           divider += "—";
        }

        //Insert that divider at every (list.length/2 + 1) position to split each resource (of total/2 bullets)
        //More dynamic based on how many csv columns were paired in the editing SNAC schema
        var pos = 0, interval = list.length/2 + 1;
        while (pos < list.length) {
           list.splice(pos, 0, divider);
           pos += interval;
        }

        //Build the string for the HTML list items
        for(var i = 0; i<list.length; i++) {
           building += list[i];
        }

        //Update the string into the preview tab
        self.updateNbEdits(data.SNAC_preview);

        //Get the HTML id element of where the list should be added
        var element = document.getElementById("preview-here");
        element.innerHTML = building; //Replace the empty HTML area with the list
        console.log("hello");
     });
};

Refine.registerUpdateFunction(function(options) {
   // Inject tabs in any project where the schema has been defined
   if(theProject.overlayModels.wikibaseSchema && !SNACSchemaAlignmentDialog.isSetUp()) {
       SNACSchemaAlignmentDialog.setUpTabs();
   }
   if (SNACSchemaAlignmentDialog.isSetUp() && (options.everythingChanged || options.modelsChanged ||
      options.rowsChanged || options.rowMetadataChanged || options.cellsChanged || options.engineChanged)) {
         if (!SNACSchemaAlignmentDialog._hasUnsavedChanges) {
            SNACSchemaAlignmentDialog._discardChanges();
         }
         SNACSchemaAlignmentDialog.updateColumns();
         SNACSchemaAlignmentDialog.issues();
         SNACSchemaAlignmentDialog.preview();
   }
});

/*************************
 * WARNINGS RENDERING *
 *************************/

SNACSchemaAlignmentDialog._updateWarnings = function(warnings, totalCount) {
   var mainDiv = $('#snac-issues-panel');
   var countsElem = this.issuesTabCount;

   // clear everything
   mainDiv.empty();
   countsElem.hide();

   // Add any warnings
   var table = $('<table></table>').appendTo(mainDiv);
   for (const warning of warnings) {
      var tr = $('<tr></tr>').addClass('wb-warning');
      var bodyTd = $('<td></td>')
         .addClass('wb-warning-body')
         .appendTo(tr);
      var h1 = $('<h1></h1>')
         .html(warning.title)
         .appendTo(bodyTd);
      var p = $('<p></p>')
         .html(warning.body)
         .appendTo(bodyTd);
      var countTd = $('<td></td>')
         .addClass('wb-warning-count')
         .appendTo(tr);
      tr.appendTo(table);
   }

   // update the warning counts
   if (totalCount) {
      countsElem.text(totalCount);
      countsElem.show();
   }
   return totalCount;
}
