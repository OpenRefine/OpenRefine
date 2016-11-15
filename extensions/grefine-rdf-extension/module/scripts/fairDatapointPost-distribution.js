/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var fairDataPointPostDistribution = {};

function fairDataPointPostDistributionDialog(callback){
    this._createDialog();
    this._callback = callback;
    this.fairDataPointPostDistribution = fairDataPointPostDistribution;
    this.fairDataPointPostDistribution._accessUrl = "http://";
};

fairDataPointPostDistributionDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("300px");
    
    var header = $('<div></div>').addClass("dialog-header").text("Add new distribution to FAIR Data Point").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    this._constructFooter(footer);
    this._constructBody(body);
    this._level = DialogSystem.showDialog(frame);
    this._body = body;
    this._renderBody(body);
};

fairDataPointPostDistributionDialog.prototype._constructBody = function(body) {
    var self = this;  
    var identifier_html = $('<p><span class="emphasized">identifier</span> <span bind="identifierSpan" ></span> <a href="#" bind="editIdentifier">edit</a></p>').appendTo(body);
    var elmts = DOM.bind(identifier_html);
    this._identifierSpan = elmts.identifierSpan;
    elmts.editIdentifier.click(function(evt){
    	evt.preventDefault();
    	self._editIdentifier($(evt.target));
    });

    var title_html = $('<p><span class="emphasized">title</span> <span bind="titleSpan" ></span> <a href="#" bind="editTitle">edit</a></p>').appendTo(body);
    var elmts = DOM.bind(title_html);
    this._titleSpan = elmts.titleSpan;
    elmts.editTitle.click(function(evt){
        evt.preventDefault();
        self._editTitle($(evt.target));
    });

    var version_html = $('<p><span class="emphasized">version</span> <span bind="versionSpan" ></span> <a href="#" bind="editVersion">edit</a></p>').appendTo(body);
    var elmts = DOM.bind(version_html);
    this._versionSpan = elmts.versionSpan;
    elmts.editVersion.click(function(evt){
        evt.preventDefault();
        self._editVersion($(evt.target));
    });

    var mediatype_html = $('<p><span class="emphasized">mediatype</span> <span bind="mediatypeSpan" ></span> <a href="#" bind="editMediatype">edit</a></p>').appendTo(body);
    var elmts = DOM.bind(mediatype_html);
    this._mediatypeSpan = elmts.mediatypeSpan;
    elmts.editMediatype.click(function(evt){
        evt.preventDefault();
        self._editMediatype($(evt.target));
    });
    
    var accessUrl_html = $('<p><span class="emphasized">Access URL </span><span bind="accessUrlSpan" >http://</span> <a href="#" bind="editAccessUrl">edit</a></p>').appendTo(body);    
    var elmts = DOM.bind(accessUrl_html);
    this._accessUrlSpan = elmts.accessUrlSpan;
    elmts.editAccessUrl.click(function(evt) {
        evt.preventDefault();
        self._editAccessUrl($(evt.target));        
    });
    
    var license_html = $('<p><span class="emphasized">license </span></p>');
    var license_html_select = $('<select class="licenses"></select>').appendTo(license_html);
    
    $.get('command/rdf-extension/get-licenses',function(data){
        data.content.forEach(function(element){
            if (typeof self.fairDataPointPostDistribution._license === 'undefined') {
                self.fairDataPointPostDistribution._license = element[0];
            }
            $('<option></option>').attr('value',element[0]).text(element[1]).appendTo(license_html_select);
        });
    });
    
    license_html_select.change(function(evt){
        if ($(evt.target).val()){
            self.fairDataPointPostDistribution._license = $(evt.target).val();
        }
    }).change();
    
    license_html.appendTo(body);
};

fairDataPointPostDistributionDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
        self._callback(self.fairDataPointPostDistribution);
    }).appendTo(footer);
    
    $('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};

fairDataPointPostDistributionDialog.prototype._editIdentifier = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newIdentifier" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newIdentifier.val(fairDataPointPostDistribution.newIdentifier).focus().select();
    elmts.applyButton.click(function() {
        var newIdentifier = elmts.newIdentifier.val();
        self.fairDataPointPostDistribution._identifier = newIdentifier;
        self._identifierSpan.empty().text(newIdentifier);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDistributionDialog.prototype._editTitle = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newTitle" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newTitle.val(fairDataPointPostDistribution.newTitle).focus().select();
    elmts.applyButton.click(function() {
        var newTitle = elmts.newTitle.val();
        self.fairDataPointPostDistribution._title = newTitle;
        self._titleSpan.empty().text(newTitle);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDistributionDialog.prototype._editMediatype = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newMediatype" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newMediatype.val(fairDataPointPostDistribution.newMediatype).focus().select();
    elmts.applyButton.click(function() {
        var newMediatype = elmts.newMediatype.val();
        self.fairDataPointPostDistribution._mediatype = newMediatype;
        self._mediatypeSpan.empty().text(newMediatype);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDistributionDialog.prototype._editVersion = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newVersion" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newVersion.val(fairDataPointPostDistribution.newVersion).focus().select();
    elmts.applyButton.click(function() {
        var newVersion = elmts.newVersion.val();
        self.fairDataPointPostDistribution._version = newVersion;
        self._versionSpan.empty().text(newVersion);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDistributionDialog.prototype._editAccessUrl = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newAccessUrl" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newAccessUrl.val(fairDataPointPostDataset.newAccessUrl).focus().select();
    elmts.applyButton.click(function() {
        var newAccessUrl = elmts.newAccessUrl.val();
        self.fairDataPointPostDistribution._accessUrl = newAccessUrl;
        if(!newAccessUrl || !newAccessUrl.substring(7)=='http://'){
            alert('Access URI should start with http://');
            return;
        }
        self._accessUrlSpan.empty().text(newAccessUrl);
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDistributionDialog.prototype._renderBody = function(body) {
    var self = this;
};
