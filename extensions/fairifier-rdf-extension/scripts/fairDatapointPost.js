/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var fairDataPointPost = {};
var fairDataPointPostCatalogDialog = null;
var fairDataPointPostDatasetDialog = null;
var fairDataPointPostDistributionDialog = null;
var project = theProject;

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
  var css = document.createElement("style");
  css.type = "text/css";
  css.innerHTML = ""+
  '.progress {'+
    'border: 16px solid #f3f3f3; /* Light grey */'+
    'border-top: 16px solid #3498db; /* Blue */'+
    'border-radius: 50%;'+
    'width: 120px;'+
    'height: 120px;'+
    'animation: spin 2s linear infinite;'+
  '}'+

  '@keyframes spin {'+
    '0% { transform: rotate(0deg); }'+
    '100% { transform: rotate(360deg); }'+
  '}';

  document.body.appendChild(css);


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
    this._pushtoResourceDiv = $('<div></div>');
    elmts.baseUriSpan.text(fairDataPointPost.baseUri);
    elmts.editBaseUriLink.click(function(evt){
      evt.preventDefault();
      self._editBaseUri($(evt.target));
    });
};

fairDataPointPostDialog.prototype._constructFooter = function(footer) {
    var self = this;

    $('<div style="hposition:absolute;visibility:visible;margin-left: 100px "></div>').addClass("progress").hide().appendTo(footer);
    
    
    $('<button></button>').addClass('button').html("OK").click(function() {
       var rdf = '';
        $.ajax({
            type: "POST",
            url : "command/rdf-extension/get-project-rdf",
            data: {project: theProject.id},
            dataTYpe:"json",
            async: false,
            success : function(text){
                rdf = text.data;
            }
        });
	      var xhr = new XMLHttpRequest();
	      xhr.upload.addEventListener("progress", function(e){ $(".progress").show(); }, false);

	      xhr.addEventListener('load', function(e) {
	        var ret = JSON.parse(this.responseText);
	        if (ret.code === "ok"){
	          $(".progress").hide();
	          alert("FAIR data pushed"); 
	          DialogSystem.dismissAll();
	        } else{
	          $(".progress").hide();
	          alert("upload error");
	        }
	      }, false);
	      xhr.open('post', "command/rdf-extension/post-fdp-info", true);
	      xhr.send(JSON.stringify({'metadata':self.fairDataPointPost, 'data':rdf}));
    }).appendTo(footer);
    
    
    $('<button></button>').addClass('button').text("Cancel").click(function() {
        DialogSystem.dismissAll();
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
                self._distributionDiv.html('');
                self._pushtoResourceDiv.html('');
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
        self.hasCatalogs = false;
        
        data.content.forEach(function(element){
            $('<option></option>').attr('value',element.uri.namespace + element.uri.localName).text(element.uri.namespace + element.uri.localName + " - " + element.title.label).appendTo(add_cat_available_html);
            self.hasCatalogs = true;
        });
        
        elmts.addCatalog.click(function(evt){
            evt.preventDefault();
            fairDataPointPostCatalogDialog = new FairDataPointPostCatalogDialog(function(catalog){
                $('<option></option>').attr('value',catalog['http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier'].url).text(catalog['http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier'].url+" - "+catalog['http://purl.org/dc/terms/title']).appendTo(add_cat_available_html); 
                self._datasetDiv.html('');
                self._distributionDiv.html('');
                self._pushtoResourceDiv.html('');
                catalog._exists = false;
                self.fairDataPointPost.catalog = catalog;
                getFairDatasets(catalog['http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier'].url, self);
            });
        });
        
       add_cat_available_html.click(function(){
           self._datasetDiv.html('');
           self._distributionDiv.html('');
           self._pushtoResourceDiv.html('');
           if (self.hasCatalogs){
               data.content.forEach(function(element){
	               self.fairDataPointPost.catalog = {
	                      // 'http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier': element.uri.namespace + element.uri.localName,
	                      // 'http://purl.org/dc/terms/title': element.title.label,
	                      // 'http://purl.org/dc/terms/hasVersion': element.version.label,
	                      // 'http://purl.org/dc/terms/publisher': element.publisher,
	                      // 'http://www.w3.org/ns/dcat#themeTaxonomy': element.themeTaxonomy,
	                      // 'http://xmlns.com/foaf/0.1/homepage': element.homepage,
	                      // 'http://xmlns.com/foaf/0.1/description': element.homepage,
	                      // 'http://purl.org/dc/terms/issued':element.catalogIssued,
	                      // 'http://purl.org/dc/terms/language':element.language,
	                      // 'http://purl.org/dc/terms/license':element.license,
	                      // 'http://purl.org/dc/terms/modified':element.modified.label,
	                      // 'http://purl.org/dc/terms/rights':element.rights.localName,
	                      // 'http://www.w3.org/ns/dcat#dataset':element.datasets,
	                      _exists: true
	               }; 
               });
           }
           getFairDatasets($('select.catalogs option:selected').val(), self);
       }).change();
       
       add_cat_available_html.appendTo(self._catalogDiv);
       self._catalogDiv.appendTo(self._body);

       if (self.hasCatalogs){
           add_cat_available_html.click();
       }
              
    }).fail(function(xhr, status, error) {
    });
};

getFairDatasets = function(url, self){
    $.post('command/rdf-extension/get-fdp-info', {"uri" : url, "layer": "dataset"},function(data){
        $('<h2>datasets</var idh2>').appendTo(self._datasetDiv);
        var add_dat_html = $('<p><a href="#" bind="addDataset">+ </a><span>add dataset</span></p>').appendTo(self._datasetDiv);
        var elmts = DOM.bind(add_dat_html);
        var add_dat_available_html = $('<select class="datasets"></select>');
        self.hasDatasets = false;

       data.content.forEach(function(element){
                $('<option></option>').attr('value',element.identifier.identifier.label).text(element.uri.namespace + element.uri.localName + " - " + element.title.label).appendTo(add_dat_available_html);
                self.hasDatasets = true;
        });

      elmts.addDataset.click(function(evt){
          evt.preventDefault();
          fairDataPointPostDatasetDialog = new FairDataPointPostDatasetDialog(function(dataset){
              $('<option></option>').attr('value',dataset['http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier'].url).text(dataset['http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier'].url+" - "+dataset['http://purl.org/dc/terms/title']).appendTo(add_dat_available_html); 
              self._distributionDiv.html('');
              self._pushtoResourceDiv.html('');
              dataset._exists = false;
              self.fairDataPointPost.dataset = dataset;
              addFairDistribution(self);
          });
      });

      add_dat_available_html.click(function(){
            self._distributionDiv.html('');
            self._pushtoResourceDiv.html('');
            if(self.hasDatasets){
                data.content.forEach(function(element){
                  self.fairDataPointPost.dataset = {
                      // 'http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier': element.uri.namespace + element.uri.localName,
                      'http://purl.org/dc/terms/title' : element.title.label,
                      'http://purl.org/dc/terms/hasVersion' : element.version.label,
                      // 'http://purl.org/dc/terms/description' : element.description,
                      // "http://www.w3.org/ns/dcat#keyword" : element.keywords,
                      // "http://www.w3.org/ns/dcat#landingPage" : element.landingPage,
                      // "http://purl.org/dc/terms/publisher" : element.publisher,
                      // "http://www.w3.org/ns/dcat#theme": element.themes, 
                      // "http://purl.org/dc/terms/issued": element.datasetIssued,
                      // "http://purl.org/dc/terms/language": element.language,
                      // "http://purl.org/dc/terms/license": element.license,
                      // "http://purl.org/dc/terms/modified": element.datasetModified,
                      // "http://purl.org/dc/terms/rights": element.rights,
                      // "http://www.w3.org/ns/dcat#contactPoint": element.contactPoint,
                      // "http://www.w3.org/ns/dcat#distribution": element.distribution,
                      _exists : true
                  }
              });
            }
            addFairDistribution(self);            
        }).change();
    
        add_dat_available_html.appendTo(self._datasetDiv);
        self._datasetDiv.appendTo(self._body);
        
        if (self.hasDatasets){
            add_dat_available_html.click();
        }
    
    }).fail(function(xhr, status, error) {});
};

addFairDistribution = function(self){
    $('<h2>distribution</h2>').appendTo(self._distributionDiv);
    var add_dist_html = $('<p><a href="#" bind="addDistribution">+ </a><span>add distribution</span><br><span id="distribution" bind="distribution"></span></p>').appendTo(self._distributionDiv);
    var elmts = DOM.bind(add_dist_html);
    elmts.addDistribution.click(function(evt){
        evt.preventDefault();
        self._pushtoResourceDiv.html('');
        fairDataPointPostDistributionDialog = new FairDataPointPostDistributionDialog(function(distribution){
			var virtuoso_html = $('<input type="radio" value="virtuoso" class="virtuosoRadio" bind="virtuoso"><span>push FAIRified data to Virtuoso</span><br>').appendTo(self._pushtoResourceDiv);
			self.virtuoso_form = $("<div class='virtuoso'></div>").appendTo(self._pushtoResourceDiv);
			var virtuoso_elmts = DOM.bind(virtuoso_html);
			self.fairDataPointPost.distribution = distribution;
			var ftp_html = $('<input type="radio" value="ftp" class="ftpRadio" bind="ftp"><span>push FAIRified data to FTP</span><br>').appendTo(self._pushtoResourceDiv); 
			self.ftp_form = $("<div class='ftp'></div>").appendTo(self._pushtoResourceDiv);
			var ftp_elmts = DOM.bind(ftp_html);

			var ftp_host_html = $('<p><span>host</span> <span bind="hostSpan" ></span> <a href="#" bind="editFtpHost">edit</a></p>').appendTo(self.ftp_form);
			var elmts = DOM.bind(ftp_host_html);
			self._ftpHostSpan = elmts.hostSpan;
			elmts.editFtpHost.click(function(evt){
			    evt.preventDefault();
			    self._editFtpHost($(evt.target));
			});

			var ftp_directory_html = $('<p><span>directory</span> <span bind="directorySpan" ></span> <a href="#" bind="editDirectory">edit</a></p>').appendTo(self.ftp_form);
			var elmts = DOM.bind(ftp_directory_html);
			self._directorySpan = elmts.directorySpan;
			elmts.editDirectory.click(function(evt){
				evt.preventDefault();
				self._editDirectory($(evt.target));
			});

			var ftp_host_html = $('<p><span>username</span> <span bind="usernameSpan" ></span> <a href="#" bind="editUsername">edit</a></p>').appendTo(self.ftp_form);
			var elmts = DOM.bind(ftp_host_html);
			self._usernameSpan = elmts.usernameSpan;
			elmts.editUsername.click(function(evt){
				evt.preventDefault();
				self._editUsername($(evt.target));
			});

			var ftp_host_html = $('<p><span>password</span> <span bind="passwordSpan" ></span> <a href="#" bind="editPassword">edit</a></p>').appendTo(self.ftp_form);
			var elmts = DOM.bind(ftp_host_html);
			self._passwordSpan = elmts.passwordSpan;
			elmts.editPassword.click(function(evt){
				evt.preventDefault();
				self._editPassword($(evt.target));
			});

			self._pushtoResourceDiv.appendTo(self._body);

			var virtuoso_host_html = $('<p><span>host</span> <span bind="hostSpan" ></span> <a href="#" bind="editVirtuosoHost">edit</a></p>').appendTo(self.virtuoso_form);
			self.fairDataPointPost.uploadtype = "virtuoso";
			var elmts = DOM.bind(virtuoso_host_html);
			self._virtuosoHostSpan = elmts.hostSpan;
			elmts.editVirtuosoHost.click(function(evt){
				evt.preventDefault();
				self._editVirtuosoHost($(evt.target));
			});

			var virtuoso_host_html = $('<p><span>username</span> <span bind="usernameSpan" ></span> <a href="#" bind="editVirtuosoUsername">edit</a></p>').appendTo(self.virtuoso_form);
			var elmts = DOM.bind(virtuoso_host_html);
			self._virtuosoUsernameSpan = elmts.usernameSpan;
			elmts.editVirtuosoUsername.click(function(evt){
				evt.preventDefault();
				self._editVirtuosoUsername($(evt.target));
			});

			var virtuoso_host_html = $('<p><span>password</span> <span bind="passwordSpan" ></span> <a href="#" bind="editVirtuosoPassword">edit</a></p>').appendTo(self.virtuoso_form);
			var elmts = DOM.bind(virtuoso_host_html);
			self._virtuosoPasswordSpan = elmts.passwordSpan;
			elmts.editVirtuosoPassword.click(function(evt){
				evt.preventDefault();
				self._editVirtuosoPassword($(evt.target));
			});

			$("#distribution").text(self.fairDataPointPost.distribution['http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier'].url + " - " + self.fairDataPointPost.distribution['http://purl.org/dc/terms/title']);
			var ftpshown = false;
			var virtuososhown = false;
			ftp_elmts.ftp.click(function(){
				$(".virtuoso").hide();
				$(".ftp").show();
				self.fairDataPointPost.uploadtype = "ftp";
			  	$('input.virtuosoRadio').removeAttr('checked');
			});
			virtuoso_elmts.virtuoso.click(function(){
				$(".ftp").hide();
				$(".virtuoso").show();
				self.fairDataPointPost.uploadtype = "virtuoso";
				$('input.ftpRadio').removeAttr('checked');
			});
	    });
   	});
    add_dist_html.appendTo(self._distributionDiv);
    self._distributionDiv.appendTo(self._body);
};

fairDataPointPostDialog.prototype._editVirtuosoHost = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newVirtuosoHost" size="50"><br/>'+
        '<button class="button" bind="applyButton">Apply</button>' + 
        '<button class="button" bind="cancelButton">Cancel</button></div>'
    );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newVirtuosoHost.val(self._newVirtuosoost).focus().select();
    elmts.applyButton.click(function() {
        var newVirtuosoHost = elmts.newVirtuosoHost.val();
        self._virtuosoHost = newVirtuosoHost;
        self._virtuosoHostSpan.empty().text(newVirtuosoHost);
        self.fairDataPointPost.virtuosoHost = newVirtuosoHost.replace(/(^\w+:|^)\/\//, '');
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDialog.prototype._editVirtuosoPassword = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="password" bind="newVirtuosoPassword" size="50"><br/>'+
        '<button class="button" bind="applyButton">Apply</button>' + 
        '<button class="button" bind="cancelButton">Cancel</button></div>'
    );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newVirtuosoPassword.val(self._newVirtuosoPassword).focus().select();
    elmts.applyButton.click(function() {
        var newVirtuosoPassword = elmts.newVirtuosoPassword.val();
        self._virtuosoPassword = newVirtuosoPassword;
        self._virtuosoPasswordSpan.empty().text("******");
        self.fairDataPointPost.virtuosoPassword = newVirtuosoPassword;
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDialog.prototype._editVirtuosoUsername = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newVirtuosoUsername" size="50"><br/>'+
        '<button class="button" bind="applyButton">Apply</button>' + 
        '<button class="button" bind="cancelButton">Cancel</button></div>'
    );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newVirtuosoUsername.val(self._newVirtuosoUsername).focus().select();
    elmts.applyButton.click(function() {
        var newVirtuosoUsername = elmts.newVirtuosoUsername.val();
        self._virtuosoUsername = newVirtuosoUsername;
        self._virtuosoUsernameSpan.empty().text(newVirtuosoUsername);
        self.fairDataPointPost.virtuosoUsername = newVirtuosoUsername;
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDialog.prototype._editFtpHost = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newFtpHost" size="50"><br/>'+
        '<button class="button" bind="applyButton">Apply</button>' + 
        '<button class="button" bind="cancelButton">Cancel</button></div>'
    );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newFtpHost.val(self._newFtpHost).focus().select();
    elmts.applyButton.click(function() {
        var newFtpHost = elmts.newFtpHost.val();
        self._ftpHost = newFtpHost;
        self._ftpHostSpan.empty().text(newFtpHost);
        self.fairDataPointPost.ftpHost = newFtpHost.replace(/(^\w+:|^)\/\//, '');
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDialog.prototype._editDirectory = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newDirectory" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newDirectory.val(self._directory).focus().select();
    elmts.applyButton.click(function() {
        var newDirectory = elmts.newDirectory.val();
        self._directory = newDirectory;
        self._directorySpan.empty().text(newDirectory);
        self.fairDataPointPost.directory = newDirectory;
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};

fairDataPointPostDialog.prototype._editUsername = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="text" bind="newUsername" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newUsername.val(self._username).focus().select();
    elmts.applyButton.click(function() {
        var newUsername = elmts.newUsername.val();
        self._username = newUsername;
        self._usernameSpan.empty().text(newUsername);
        self.fairDataPointPost.username = newUsername;
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};


fairDataPointPostDialog.prototype._editPassword = function(src){
    var self = this;
    var menu = MenuSystem.createMenu().width('400px');
    menu.html('<div class="schema-alignment-link-menu-type-search"><input type="password" bind="newPassword" size="50"><br/>'+
                    '<button class="button" bind="applyButton">Apply</button>' + 
                    '<button class="button" bind="cancelButton">Cancel</button></div>'
            );
    MenuSystem.showMenu(menu,function(){});
    MenuSystem.positionMenuLeftRight(menu, src);
    var elmts = DOM.bind(menu);
    elmts.newPassword.val(self._password).focus().select();
    elmts.applyButton.click(function() {
        var newPassword = elmts.newPassword.val();
        self._password = newPassword;
        self._passwordSpan.empty().text("*****");
        self.fairDataPointPost.password = newPassword;
        MenuSystem.dismissAll();
    });
    elmts.cancelButton.click(function() {
            MenuSystem.dismissAll();
    });
};
