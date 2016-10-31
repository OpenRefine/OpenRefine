/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var fairDataPointPostCatalog = {};

function fairDataPointPostCatalogDialog(schema){
    this._schema = cloneDeep(schema); // this is what can be munched on
    this._createDialog();
    
    this._licenses = $.get("command/rdf-extension/get-licenses", function(data){
        var parser = N3.Parser();
        alert(JSON.stringify( parser.parse(data.content, function(error, triple, prefixes){
            if (!error){
                if (triple) {
                    if(triple.predicate === "http://www.w3.org/2000/01/rdf-schema#label"){
                        return {'label':triple.object, 'url': triple.subject};
                    };
                };
            };
        })
        ));
        return licenses;
    });
    
    this.fairDataPointPostCatalog = fairDataPointPostCatalog;
};

fairDataPointPostCatalogDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("500px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add new catalog to FAIRDataPoint").appendTo(frame);
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
    var description_html = $('<p><span class="emphasized">description</span> <span bind="descriptionSpan" ></span> <a href="#" bind="editDescription">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(description_html);
    this._descriptionSpan = elmts.descriptionSpan;
    elmts.editDescription.click(function(evt){
        evt.preventDefault();
        self._editDescription($(evt.target));
    });
    var license_html = $('<p><span class="emphasized">license</span> <span bind="LicenseSpan" ></span> <a href="#" bind="editLicense">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(license_html);
    this._licenseSpan = elmts.licenseSpan;
    elmts.editLicense.click(function(evt){
        evt.preventDefault();
        self._editLicense($(evt.target));
    });
};

fairDataPointPostCatalogDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        form.submit();
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

fairDataPointPostCatalogDialog.prototype._editDescription = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newDescription" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newDescription.val(fairDataPointPostCatalog.newDescription).focus().select();
    elmts.applyButton.click(function() {
        var newDescription = elmts.newDescription.val();
        self.fairDataPointPostCatalog._description = newDescription;
        self._descriptionSpan.empty().text(newDescription);
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

fairDataPointPostCatalogDialog.prototype._editLicense = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search licenses"><input type="text" bind="newLicense" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newLicense.val(fairDataPointPostCatalog.newLicense).focus().select();
    elmts.applyButton.click(function() {
        var newLicense = elmts.newLicense.val();
        self.fairDataPointPostCatalog._license = newLicense;
        self._licenseSpan.empty().text(newLicense);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostCatalogDialog.prototype._renderBody = function(body) {
    var self = this;
};
