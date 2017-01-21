/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var fairDataPointPostDataset = {};

function fairDataPointPostDatasetDialog(callback){
    this._createDialog();
    this._callback = callback;
    this.fairDataPointPostDataset = fairDataPointPostDataset;
    this.fairDataPointPostDataset._theme = "http://";
    this.fairDataPointPostDataset._landingPage = "http://";
};

fairDataPointPostDatasetDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("300px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add new dataset to FAIR Data Point").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    this._constructFooter(footer);
    this._constructBody(body);
    this._level = DialogSystem.showDialog(frame);
    this._body = body;
    this._renderBody(body);
};

fairDataPointPostDatasetDialog.prototype._constructBody = function(body) {
    var self = this;  
    var identifier_html = $('<p><span class="emphasized">identifier</span> <span bind="identifierSpan" ></span> <a href="#" bind="editIdentifier">edit</a></p>').appendTo(body);
    var elmts = DOM.bind(identifier_html);
    this._identifierSpan = elmts.identifierSpan;
    elmts.editIdentifier.click(function(evt){
    	evt.preventDefault();
    	self._editIdentifier($(evt.target));
    });
    var label_html = $('<p><span class="emphasized">title</span> <span bind="titleSpan" ></span> <a href="#" bind="editTitle">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(label_html);
    this._titleSpan = elmts.titleSpan;
    elmts.editTitle.click(function(evt){
        evt.preventDefault();
        self._editTitle($(evt.target));
    });
    var label_html = $('<p><span class="emphasized">label</span> <span bind="labelSpan" ></span> <a href="#" bind="editLabel">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(label_html);
    this._labelSpan = elmts.labelSpan;
    elmts.editLabel.click(function(evt){
        evt.preventDefault();
        self._editLabel($(evt.target));
    });
    var version_html = $('<p><span class="emphasized">version</span> <span bind="versionSpan" ></span> <a href="#" bind="editVersion">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(version_html);
    this._versionSpan = elmts.versionSpan;
    elmts.editVersion.click(function(evt){
        evt.preventDefault();
        self._editVersion($(evt.target));
    });
    
    var description_html = $('<p><span class="emphasized">description</span> <span bind="descriptionSpan" ></span> <a href="#" bind="editDescription">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(description_html);
    this._descriptionSpan = elmts.descriptionSpan;
    elmts.editDescription.click(function(evt){
        evt.preventDefault();
        self._editDescription($(evt.target));
    });

    var keyword_html = $('<p><span class="emphasized">keywords</span> <span bind="keywordSpan" ></span> <a href="#" bind="editKeyword">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(keyword_html);
    this._keywordSpan = elmts.keywordSpan;
    elmts.editKeyword.click(function(evt){
        evt.preventDefault();
        self._editKeyword($(evt.target));
    });
    
    var landingPage_html = $('<p><span class="emphasized">landingpage </span><span bind="landingPageSpan" >http://</span> <a href="#" bind="editLandingpage">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(landingPage_html);
    this._landingPageSpan = elmts.landingPageSpan;
    elmts.editLandingpage.click(function(evt) {
        evt.preventDefault();
        self._editLandingpage($(evt.target));        
    });

    var theme_html = $('<p><span class="emphasized">theme </span><span bind="themeSpan" >http://</span> <a href="#" bind="editTheme">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(theme_html);
    this._themeSpan = elmts.themeSpan;
    elmts.editTheme.click(function(evt) {
        evt.preventDefault();
        self._editTheme($(evt.target));        
    });

    var publisher_html = $('<p><span class="emphasized">publisher</span> <span bind="publisherSpan" ></span> <a href="#" bind="editPublisher">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(publisher_html);
    this._publisherSpan = elmts.publisherSpan;
    elmts.editPublisher.click(function(evt){
        evt.preventDefault();
        self._editPublisher($(evt.target));
    });

//    var language_html = $('<p><span class="emphasized">language </span></p>');
//    var language_html_select = $('<select class="languages"></select>').appendTo(language_html);
//    
//    $.get('command/rdf-extension/get-languages',function(data){
//        data.content.forEach(function(element){
//            if (typeof self.fairDataPointPostDataset._language === 'undefined') {
//                self.fairDataPointPostDataset._language = element[1];
//            }
//            $('<option></option>').attr('value',element[1]).text(element[0]).appendTo(language_html_select);
//        });
//    });
//    
//    language_html_select.change(function(evt){
//        if ($(evt.target).val()){
//            self.fairDataPointPostDataset._language = $(evt.target).val();
//        }
//    }).change();
//        language_html.appendTo(body);
};

fairDataPointPostDatasetDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._callback(self.fairDataPointPostDataset);
    }).appendTo(footer);
    
    $('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

fairDataPointPostDatasetDialog.prototype._editIdentifier = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newIdentifier" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newIdentifier.val(fairDataPointPostDataset.newIdentifier).focus().select();
    elmts.applyButton.click(function() {
        var newIdentifier = elmts.newIdentifier.val();
        self.fairDataPointPostDataset._identifier = newIdentifier;
        self._identifierSpan.empty().text(newIdentifier);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._editLabel = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newLabel" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newLabel.val(fairDataPointPostDataset.newLabel).focus().select();
    elmts.applyButton.click(function() {
        var newLabel = elmts.newLabel.val();
        self.fairDataPointPostDataset._label = newLabel;
        self._labelSpan.empty().text(newLabel);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._editVersion = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newVersion" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newVersion.val(fairDataPointPostDataset.newVersion).focus().select();
    elmts.applyButton.click(function() {
        var newVersion = elmts.newVersion.val();
        self.fairDataPointPostDataset._version = newVersion;
        self._versionSpan.empty().text(newVersion);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._editTitle = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newTitle" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newTitle.val(fairDataPointPostDataset.newTitle).focus().select();
    elmts.applyButton.click(function() {
        var newTitle = elmts.newTitle.val();
        self.fairDataPointPostDataset._title = newTitle;
        self._titleSpan.empty().text(newTitle);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._editDescription = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newDescription" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newDescription.val(fairDataPointPostDataset.newDescription).focus().select();
    elmts.applyButton.click(function() {
        var newDescription = elmts.newDescription.val();
        self.fairDataPointPostDataset._description = newDescription;
        self._descriptionSpan.empty().text(newDescription);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._editKeyword = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newKeyword" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newKeyword.val(fairDataPointPostDataset.newKeyword).focus().select();
    elmts.applyButton.click(function() {
        var newKeyword = elmts.newKeyword.val();
        self.fairDataPointPostDataset._keyword = newKeyword;
        self._keywordSpan.empty().text(newKeyword);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._editLandingpage = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newLandingpage" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newLandingpage.val(fairDataPointPostDataset.newLandingpage).focus().select();
    elmts.applyButton.click(function() {
        var newLandingpage = elmts.newLandingpage.val();
        self.fairDataPointPostDataset._landingpage = newLandingpage;
        if(!newLandingpage || !newLandingpage.substring(7)=='http://'){
            alert('Landingpage URI should start with http://');
            return;
        }        
        self._landingPageSpan.empty().text(newLandingpage);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._editTheme = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newTheme" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newTheme.val(fairDataPointPostDataset.newTheme).focus().select();
    elmts.applyButton.click(function() {
        var newTheme = elmts.newTheme.val();
        self.fairDataPointPostDataset._theme = newTheme;
        if(!newTheme || !newTheme.substring(7)=='http://'){
            alert('Theme URI should start with http://');
            return;
        }        
        self._themeSpan.empty().text(newTheme);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};


fairDataPointPostDatasetDialog.prototype._editPublisher = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newPublisher" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newPublisher.val(fairDataPointPostDataset.newPublisher).focus().select();
    elmts.applyButton.click(function() {
        var newPublisher = elmts.newPublisher.val();
        self.fairDataPointPostDataset._publisher = newPublisher;
        self._publisherSpan.empty().text(newPublisher);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDatasetDialog.prototype._renderBody = function(body) {
    var self = this;
};
