/*

Copyright 2011, Google Inc.
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

Refine.CreateProjectUI = function(elmt) {
    var self = this;
    
    this._elmt = elmt;
    this._sourceSelectionUIs = [];
    this._customPanels = [];
    this._controllers = [];
    
    $.post(
        "/command/core/get-importing-configuration",
        null,
        function(data) {
            Refine.importingConfig = data.config;
            self._initializeUI();
        },
        "json"
    );
};

Refine.CreateProjectUI.controllers = [];

Refine.CreateProjectUI.prototype._initializeUI = function() {
    this._sourceSelectionElmt =
        $(DOM.loadHTML("core", "scripts/index/create-project-ui-source-selection.html")).appendTo(this._elmt);

    this._sourceSelectionElmts = DOM.bind(this._sourceSelectionElmt);
    
    for (var i = 0; i < Refine.CreateProjectUI.controllers.length; i++) {
        this._controllers.push(new Refine.CreateProjectUI.controllers[i](this));
    }
};

Refine.CreateProjectUI.prototype.addSourceSelectionUI = function(sourceSelectionUI) {
    var self = this;
    
    var headerContainer = $('#create-project-ui-source-selection-tabs');
    var bodyContainer = $('#create-project-ui-source-selection-tab-bodies');

    sourceSelectionUI._divBody = $('<div>')
        .addClass('create-project-ui-source-selection-tab-body')
        .appendTo(bodyContainer)
        .hide();

    sourceSelectionUI._divHeader = $('<div>')
        .addClass('create-project-ui-source-selection-tab')
        .text(sourceSelectionUI.label)
        .appendTo(headerContainer)
        .click(function() { self.selectImportSource(sourceSelectionUI.id); });

    sourceSelectionUI.ui.attachUI(sourceSelectionUI._divBody);

    this._sourceSelectionUIs.push(sourceSelectionUI);
    
    if (this._sourceSelectionUIs.length == 1) {
      self.selectImportSource(sourceSelectionUI.id);
    }
};

Refine.CreateProjectUI.prototype.selectImportSource = function(id) {
    for (var i = 0; i < this._sourceSelectionUIs.length; i++) {
        var sourceSelectionUI = this._sourceSelectionUIs[i];
        if (sourceSelectionUI.id == id) {
            $('.create-project-ui-source-selection-tab-body').hide();
            $('.create-project-ui-source-selection-tab').removeClass('selected');

            sourceSelectionUI._divBody.show();
            sourceSelectionUI._divHeader.addClass('selected');
            
            sourceSelectionUI.ui.focus();
            
            break;
        }
    }
};

Refine.CreateProjectUI.prototype.addCustomPanel = function() {
    var div = $('<div>')
        .addClass('create-project-ui-panel')
        .appendTo(this._elmt);
    
    var innerDiv = $('<div>')
        .addClass('relative-frame')
        .appendTo(div);
    
    this._customPanels.push(div);
    
    return innerDiv;
};

Refine.CreateProjectUI.prototype.showCustomPanel = function(div) {
    var parent = div.parent();
    for (var i = 0; i < this._customPanels.length; i++) {
        var panel = this._customPanels[i];
        if (panel[0] === parent[0]) {
            $('.create-project-ui-panel').css('visibility', 'hidden');
            this._sourceSelectionElmt.css('visibility', 'hidden');
            panel.css('visibility', 'visible');
            break;
        }
    }
};

Refine.CreateProjectUI.prototype.showSourceSelectionPanel = function() {
    $('.create-project-ui-panel').css('visibility', 'hidden');
    this._sourceSelectionElmt.css('visibility', 'visible');
};

Refine.actionAreas.push({
  id: "create-project",
  label: "Create Project",
  uiClass: Refine.CreateProjectUI
});
