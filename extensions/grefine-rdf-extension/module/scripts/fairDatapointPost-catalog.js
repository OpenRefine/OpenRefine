/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var fairDataPointPostCatalog = {};

function fairDataPointPostCatalogDialog(callback){
    this._createDialog();
    this._callback = callback;
    this.fairDataPointPostCatalog = fairDataPointPostCatalog;
};

fairDataPointPostCatalogDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("300px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add new catalog to FAIR Data Point").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    this._constructFooter(footer);
    this._constructBody(body);
    this._level = DialogSystem.showDialog(frame);
    this._body = body;
    this._renderBody(body);
};

fairDataPointPostCatalogDialog.prototype._constructBody = function(body) {
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

    var homepage_html = $('<p><span class="emphasized">homepage</span> <span bind="homepageSpan" >http://</span> <a href="#" bind="editHomepage">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(homepage_html);
    this._homepageSpan = elmts.homepageSpan;
    elmts.editHomepage.click(function(evt){
        evt.preventDefault();
        self._editHomepage($(evt.target));
    });
    

    var language_html = $('<p><span class="emphasized">language </span></p>');
    var language_html_select = $('<select class="languages"></select>').appendTo(language_html);
    
    $.get('command/rdf-extension/get-languages',function(data){
        data.content.forEach(function(element){
            $('<option></option>').attr('value',element[1]).text(element[0]).appendTo(language_html_select);
        });
    });
    
    language_html_select.change(function(evt){
        if ($(evt.target).val()){
            self.fairDataPointPostCatalog._language = $(evt.target).val();
        }
    }).change();
    
    language_html.appendTo(body);
    
    var theme_html = $('<p><span class="emphasized">theme </span><span bind="themeSpan" >http://</span> <a href="#" bind="editTheme">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(theme_html);
    this._themeSpan = elmts.themeSpan;
    elmts.editTheme.click(function(evt) {
        evt.preventDefault();
        self._editTheme($(evt.target));        
    });

};

fairDataPointPostCatalogDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._callback(self.fairDataPointPostCatalog);
    }).appendTo(footer);
    
    $('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

fairDataPointPostCatalogDialog.prototype._editIdentifier = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newIdentifier" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newIdentifier.val(fairDataPointPostCatalog.newIdentifier).focus().select();
    elmts.applyButton.click(function() {
        var newIdentifier = elmts.newIdentifier.val();
        self.fairDataPointPostCatalog._identifier = newIdentifier;
        self._identifierSpan.empty().text(newIdentifier);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostCatalogDialog.prototype._editLabel = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newLabel" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newLabel.val(fairDataPointPostCatalog.newLabel).focus().select();
    elmts.applyButton.click(function() {
        var newLabel = elmts.newLabel.val();
        self.fairDataPointPostCatalog._label = newLabel;
        self._labelSpan.empty().text(newLabel);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostCatalogDialog.prototype._editVersion = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newVersion" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newVersion.val(fairDataPointPostCatalog.newVersion).focus().select();
    elmts.applyButton.click(function() {
        var newVersion = elmts.newVersion.val();
        self.fairDataPointPostCatalog._version = newVersion;
        self._versionSpan.empty().text(newVersion);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostCatalogDialog.prototype._editTitle = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newTitle" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newTitle.val(fairDataPointPostCatalog.newTitle).focus().select();
    elmts.applyButton.click(function() {
        var newTitle = elmts.newTitle.val();
        self.fairDataPointPostCatalog._title = newTitle;
        self._titleSpan.empty().text(newTitle);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostCatalogDialog.prototype._editHomepage = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newHomepage" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newHomepage.val(fairDataPointPostCatalog.newHomepage).focus().select();
    elmts.applyButton.click(function() {
        var newHomepage = elmts.newHomepage.val();
        self.fairDataPointPostCatalog._homepage = newHomepage;
        if(!newHomepage || !newHomepage.substring(7)=='http://'){
            alert('Theme URI should start with http://');
            return;
        }        
        self._homepageSpan.empty().text(newHomepage);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostCatalogDialog.prototype._editTheme = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newTheme" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newTheme.val(fairDataPointPostCatalogDialog.newTheme).focus().select();
    elmts.applyButton.click(function() {
        var newTheme = elmts.newTheme.val();
        self.fairDataPointPostCatalog._theme = newTheme;
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

fairDataPointPostCatalogDialog.prototype._renderBody = function(body) {
    var self = this;
};
