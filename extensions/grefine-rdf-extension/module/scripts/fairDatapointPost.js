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
    
    frame.width("500px");
    
    var header = $('<div></div>').addClass("dialog-header").text("POST to Fair Data Point").appendTo(frame);
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
        'The created RDF schema provided can now be uploaded to a Fair Data Point. ' +
    '</p>').appendTo(body);
    
    var html = $('<p class="base-uri-space"><span class="emphasized">Base URI </span> <span bind="baseUriSpan" ></span> <a href="#" bind="editBaseUriLink">edit</a></p>').appendTo(body);
    var elmts = DOM.bind(html);
    this._baseUriSpan = elmts.baseUriSpan;
    this._catalogDiv = $('<div></div>');
    this._datasetDiv = $('<div></div>');
    this._distributionDiv = $('<div></div>');
    elmts.baseUriSpan.text(fairDataPointPost.baseUri);
    elmts.editBaseUriLink.click(function(evt){
    	evt.preventDefault();
    	self._editBaseUri($(evt.target));
    });
};

fairDataPointPostDialog.prototype._constructFooter = function(footer) {
    var self = this;
    
    $('<button></button>').addClass('button').html("&nbsp;&nbsp;OK&nbsp;&nbsp;").click(function() {
        $.post("command/rdf-extension/post-fdp-info", {"fdp": self.fairDataPointPost},function(response){
            alert(reponse);
        });
        
        window.open(self._baseUriSpan.text());
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
                self._datasetDiv.html('');
                self.uri = self._baseUriSpan.text();
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
        $('').val()
    }
};

fairDataPointPostDialog.prototype._renderBody = function(body) {
    var self = this;
};

getFairCatalogs = function(rootUrl, self){
    $.post('command/rdf-extension/get-fdp-info', {"uri" : rootUrl, "layer": "catalog"},function(data){
        $('<h2>catalogs</h2>').appendTo(self._catalogDiv);
        var add_cat_html = $('<p><a href="#" bind="addCatalog">+ </a><span>add catalog</span></p>').appendTo(self._catalogDiv);
        var elmts = DOM.bind(add_cat_html);
        var add_cat_available_html = $('<select class="catalogs"></select>');
       
        data.content.forEach(function(element){
            $('<option></option>').attr('value',element).text(element).appendTo(add_cat_available_html);
        });
        
        elmts.addCatalog.click(function(evt){
            evt.preventDefault();
            new fairDataPointPostCatalogDialog(function(catalog){
               if (catalog._title && catalog._identifier){
                   $('<option></option>').attr('value',JSON.stringify(catalog)).text(catalog._identifier+" - "+catalog._title).appendTo(add_cat_available_html); 
                   self._datasetDiv.html('');
                   self._distributionDiv.html('');
                   self.fairDataPointPost.catalog = catalog;
                   getFairDatasets(self.fairDataPointPost.baseUri + "/" + catalog._identifier, self);
               }
            });
        });
        
        add_cat_available_html.change(function(evt){
            if ($(evt.target).val()){
                self._datasetDiv.html('');
                self._distributionDiv.html('');
                getFairDatasets(self.fairDataPointPost.baseUri + "/" + JSON.parse($(evt.target).val())._identifier, self);
            }
        }).change();
        
        add_cat_available_html.appendTo(self._catalogDiv);
        self._catalogDiv.appendTo(self._body);
    }).fail(function() {
        alert( "Failed to retrieve data from Fair DataPoint" );
    });
};

getFairDatasets = function(url, self){
    $('<h2>datasets</h2>').appendTo(self._datasetDiv);
    var add_dat_html = $('<p><a href="#" bind="addDataset">+ </a><span>add dataset</span></p>').appendTo(self._datasetDiv);
    var elmts = DOM.bind(add_dat_html);
    var add_dat_available_html = $('<select class="datasets"></select>');
    
    $.post('command/rdf-extension/get-fdp-info', {"uri" : url, "layer": "dataset"},function(data){
        data.content.forEach(function(element){
            $('<option></option>').attr('value',element).text(element).appendTo(add_dat_available_html);
        });
    });
    
    elmts.addDataset.click(function(evt){
        evt.preventDefault();
        new fairDataPointPostDatasetDialog(function(dataset){
           if (dataset._title && dataset._identifier){
               $('<option></option>').attr('value',JSON.stringify(dataset)).text(dataset._identifier+" - "+dataset._title).appendTo(add_dat_available_html); 
               self._distributionDiv.html('');
               self.fairDataPointPost.dataset = dataset;
               addFairDistribution(self);
           }
        });
    });
    
    add_dat_available_html.change(function(evt){
        if ($(evt.target).val()){
            self._distributionDiv.html('');
            addFairDistribution(self);
        }
    }).change();
    
    add_dat_available_html.appendTo(self._datasetDiv);
    self._datasetDiv.appendTo(self._body);
};

addFairDistribution = function(self){
    $('<h2>distribution</h2>').appendTo(self._distributionDiv);
    var add_dist_html = $('<p><a href="#" bind="addDistribution">+ </a><span>add distribution</span><br><span bind="distribution"></span></p>').appendTo(self._distributionDiv);
    var elmts = DOM.bind(add_dist_html);
    elmts.addDistribution.click(function(evt){
        evt.preventDefault();
        new fairDataPointPostDistributionDialog(function(distribution){
            elmts.distribution.text(distribution._identifier + " - " + distribution._title);
            self.fairDataPointPost.distribution = distribution;
        });
    });
    add_dist_html.appendTo(self._distributionDiv);
    self._distributionDiv.appendTo(self._body);
};