/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var fairDataPointPost = {};

function fairDataPointPostDialog(schema){
    this._schema = cloneDeep(schema); // this is what can be munched on
    this._createDialog();
    this.fairDataPointPost = fairDataPointPost;
    this.fairDataPointPost.baseUri = "http://";
    this._replaceBaseUri(fairDataPointPost.baseUri,true);
};

fairDataPointPostDialog.prototype._createDialog = function() {
    var self = this;
    var frame = DialogSystem.createDialog();
    
    frame.width("1000px");
    
    var header = $('<div></div>').addClass("dialog-header").text("POST to FairDatapoint").appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    
    this._constructFooter(footer);
    this._constructBody(body);
  
    this._level = DialogSystem.showDialog(frame);
    this._body = body;
    this._renderBody(body);
};

fairDataPointPostDialog.prototype._constructBody = function(body) {
    var self = this;
    $('<p>' +
        'The created RDF schema provided can now be uploaded to a FairDatapoint. ' +
    '</p>').appendTo(body);
    
    var html = $('<p class="base-uri-space"><span class="emphasized">Base URI </span> <span bind="baseUriSpan" ></span> <a href="#" bind="editBaseUriLink">edit</a></p>').appendTo(body);
    var elmts = DOM.bind(html);
    this._baseUriSpan = elmts.baseUriSpan;
    this._catalogDiv =$('<div></div>');
    elmts.baseUriSpan.text(fairDataPointPost.baseUri);
    elmts.editBaseUriLink.click(function(evt){
    	evt.preventDefault();
    	self._editBaseUri($(evt.target));
    });
};

fairDataPointPostDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
//    	var schema = self.getJSON();
//        JSON.stringify(schema);
        
        var form = document.createElement("form");
        $(form)
            .css("display", "none")
            .attr("method", "post")
            .attr("action", self._baseUriSpan.text());
        $('<input />')
            .attr("name", "engine")
            .attr("value", JSON.stringify(ui.browsingEngine.getJSON()))
            .appendTo(form);
        $('<input />')
            .attr("name", "project")
            .attr("value", theProject.id)
            .appendTo(form);

        document.body.appendChild(form);
        form.submit();
        window.open(self._baseUriSpan.text());
        document.body.removeChild(form);
    }).appendTo(footer);
    
    $('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissUntil(self._level - 1);
    }).appendTo(footer);
};


fairDataPointPostDialog.prototype._editBaseUri = function(src){
	var self = this;
	var menu = MenuSystem.createMenu().width('400px');
	menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newBaseUri" size="50"><br/>'+
			'<button class="button" bind="applyButton">Apply</button>' + 
			'<button class="button" bind="cancelButton">Cancel</button></div>'
                );
	MenuSystem.showMenu(menu,function(){});
	MenuSystem.positionMenuLeftRight(menu, src);
	var elmts = DOM.bind(menu);
	elmts.newBaseUri.val(fairDataPointPost.baseUri).focus().select();
	elmts.applyButton.click(function() {
	        var newBaseUri = elmts.newBaseUri.val();
                if(!newBaseUri || !newBaseUri.substring(7)=='http://'){
                    alert('Base URI should start with http://');
                    return;
                } if(self.fairDataPointPost.baseUri.length > 7){
                    self._catalogDiv.html('');
                    getFairCatalogs(self._baseUriSpan.text(), self);
                    return;
                }
            MenuSystem.dismissAll();
            self._replaceBaseUri(newBaseUri,false);
        });
	
	elmts.cancelButton.click(function() {
                MenuSystem.dismissAll();
        });
};
fairDataPointPostDialog.prototype._replaceBaseUri = function(newBaseUri,doNotSave){
    var self = this;
    var frame = DialogSystem.createDialog();
    if(doNotSave){
        self._baseUriSpan.empty().text(newBaseUri);
        self.fairDataPointPost.baseUri = newBaseUri;
    }else{
        self._baseUriSpan.empty().text(newBaseUri);
        self.fairDataPointPost.baseUri = newBaseUri;
        var self = this;
        getFairCatalogs(newBaseUri, self);
//            var catalogs = parser.parse(data, function(error, triple, prefixes){
//                if (triple) {
//                    if(triple.predicate === "http://www.w3.org/ns/ldp#contains"){
//                        var object = triple.object;
//                        {object:
//                            $.get(object, function(datasetData){
//                                parser.parse(datasetData, function(e, t, p){
//                                    if (t) {
//                                        if(t.predicate === "http://www.w3.org/ns/dcat#dataset"){
//                                            return t.object;
//                                        };
//                                    };
//                                });
//                            });
//                        };
//                    };
//                };
//            });
    }
};

fairDataPointPostDialog.prototype._renderBody = function(body) {
    var self = this;
};

getFairCatalogs = function(rootUrl, self){
    $.post('command/rdf-extension/get-fdp', {"uri" : rootUrl},function(data){
        $('<h2>catalogs</h2>').appendTo(self._catalogDiv);
        var add_cat_html = $('<p><a href="#" bind="addCatalog">+ </a><span>add catalog</span></p>').appendTo(self._catalogDiv);
        var elmts = DOM.bind(add_cat_html);
        var add_cat_available_html = $('<select></select>');
       
        elmts.addCatalog.click(function(evt){
            evt.preventDefault();
            new fairDataPointPostCatalogDialog(function(catalog){
               $('<option></option>').attr('value',JSON.stringify(catalog)).text(catalog._identifier+" - "+catalog._title).appendTo(add_cat_available_html); 
            });
        });
        add_cat_available_html.appendTo(self._catalogDiv);
        self._catalogDiv.appendTo(self._body);
    }).fail(function() {
        alert( "Failed to retrieve data from Fair DataPoint" );
    });
};

