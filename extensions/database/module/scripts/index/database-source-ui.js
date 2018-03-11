/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

$(function(){
    $.contextMenu({
        selector: '.context-menu-one',
        trigger: 'left',
        build: function($trigger, e) {

            return {
                callback: function(key, options) {
                    var m = "clicked: " + key;
                    DatabaseExtension.handleSavedConnectionClicked(key,  $(this).text());

                },

                items: {
                    "edit":        {name: " Edit "},
                    "sep0": "",
                    "delete":   {name: " Delete "},
                    "sep1": "---------",
                    "connect": {name: " Connect "},
                    "dummy":  {name: "", icon: ""}
                }
            };
        }
    });
});

Refine.DatabaseSourceUI = function(controller) {
  this._controller = controller;
};

Refine.DatabaseSourceUI.prototype.attachUI = function(body) {
  this._body = body;
  
  this._body.html(DOM.loadHTML("database", "scripts/index/database-import-form.html"));
  this._elmts = DOM.bind(this._body);
  var self = this;
  
  $('#database-title').text($.i18n._("database-import")["title"]); 
  $('#connectionNameLabel').html($.i18n._("database-source")["connectionNameLabel"]);
  $('#databaseTypeLabel').html($.i18n._("database-source")["databaseTypeLabel"]);
  $('#databaseHostLabel').text($.i18n._("database-source")["databaseHostLabel"]);
  $('#databasePortLabel').text($.i18n._("database-source")["databasePortLabel"]);
  $('#databaseUserLabel').text($.i18n._("database-source")["databaseUserLabel"]);
  $('#databasePasswordLabel').text($.i18n._("database-source")["databasePasswordLabel"]);
  $('#databaseNameLabel').text($.i18n._("database-source")["databaseNameLabel"]);
  $('#databaseSchemaLabel').text($.i18n._("database-source")["databaseSchemaLabel"]);
  $('#databaseTestButton').text($.i18n._("database-source")["databaseTestButton"]);
  $('#databaseSaveButton').text($.i18n._("database-source")["databaseSaveButton"]);
  $('#databaseConnectButton').text($.i18n._("database-source")["databaseConnectButton"]);
  $('#newConnectionButtonDiv').text($.i18n._("database-source")["newConnectionButtonDiv"]);
  $('#savedConnectionSpan').text($.i18n._("database-source")["savedConnectionSpan"]);
 
  
  this._elmts.newConnectionButton.click(function(evt) {
      self._resetDatabaseImportForm();
      $( "#newConnectionDiv" ).show();
      $( "#sqlEditorDiv" ).hide();
    //   self._body.find('.newConnectionDiv').show();  
    //   self._body.find('.sqlEditorDiv').hide();
     
  });
  
  
  this._elmts.databaseTypeSelect.change(function(event) {
      var type = $( "#databaseTypeSelect" ).val(); 
      if(type === "postgresql"){ 
          $( "#databaseUser" ).val("postgres");
          $( "#databasePort" ).val("5432");	  
      }else if(type === "mysql"){	  
          $( "#databaseUser" ).val("root");
          $( "#databasePort" ).val("3306");	  
      }else if(type === "mariadb"){
          $( "#databaseUser" ).val("root");
           $( "#databasePort" ).val("3306");	  
      }else{
          $( "#databaseUser" ).val("root");
          $( "#databasePort" ).val("3306");
      }
  });
  
  this._elmts.testDatabaseButton.click(function(evt) {
      
          if(self._validateNewConnectionForm() === true){
             self._testDatabaseConnect(self._getConnectionInfo());
          }
       
   });
  
  this._elmts.databaseConnectButton.click(function(evt) {
      
      if(self._validateNewConnectionForm() === true){
             self._connect(self._getConnectionInfo());
      }

   
 });
  
 this._elmts.saveConnectionButton.click(function(evt) {
      
       if(self._validateNewConnectionForm() == true){
               var connectionNameInput = $.trim(self._elmts.connectionNameInput[0].value);
            if (connectionNameInput.length === 0) {
                window.alert($.i18n._('database-source')["alert-connection-name"]);   
            } else{
                    self._saveConnection(self._getConnectionInfo());
            }
             
       }

 });
  
  this._elmts.executeQueryButton.click(function(evt) {
        var jdbcQueryInfo = {};
        jdbcQueryInfo.connectionName = $( "#currentConnectionNameInput" ).val();
        jdbcQueryInfo.databaseType = $( "#currentDatabaseTypeInput" ).val();
        jdbcQueryInfo.databaseServer = $( "#currentDatabaseHostInput" ).val();
        jdbcQueryInfo.databasePort = $( "#currentDatabasePortInput" ).val();  
        jdbcQueryInfo.databaseUser = $( "#currentDatabaseUserInput" ).val();
        jdbcQueryInfo.databasePassword = $( "#currentDatabasePasswordInput" ).val();
        jdbcQueryInfo.initialDatabase = $( "#currentInitialDatabaseInput" ).val();
        jdbcQueryInfo.query = $.trim($( "#queryTextArea" ).val()); 
        
//	    if(jdbcQueryInfo.query && jdbcQueryInfo.query.length > 0 ) {
//      	   self._executeQuery(jdbcQueryInfo);
//	    }else{
//	    	  window.alert($.i18n._('database-source')["alert-query"]);
//	    }
        
        if(self.validateQuery(jdbcQueryInfo.query)) {
                self._executeQuery(jdbcQueryInfo);
        }
        
        
        
        
  });


  
  this._elmts.editConnectionButton.click(function(evt) {
      
      if(self._validateNewConnectionForm() == true){
               var connectionNameInput = $.trim(self._elmts.connectionNameInput[0].value);
            if (connectionNameInput.length === 0) {
                window.alert($.i18n._('database-source')["alert-connection-name"]);   
            } else{
                    self._editConnection(self._getConnectionInfo());
            }
             
       }
    
 });
  
 this._elmts.cancelEditConnectionButton.click(function(evt) {
     self._resetDatabaseImportForm();
        
 });
  
  //load saved connections from settings file in user home directory
  self._loadSavedConnections();
 
};//end Refine.createUI


Refine.DatabaseSourceUI.prototype.focus = function() {
};


Refine.DatabaseSourceUI.prototype.validateQuery = function(query) {
     //alert("query::" + query);
     if(!query || query.length <= 0 ) {
         window.alert($.i18n._('database-source')["alert-query"]);
         return false;
     }
    
     var allCapsQuery = query.toUpperCase();
    
     if(allCapsQuery.indexOf('DROP') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " DROP");
         return false;
     }else if(allCapsQuery.indexOf('TRUNCATE') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " TRUNCATE");
         return false;
     }else if(allCapsQuery.indexOf('DELETE') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " DELETE");
         return false;
     }else if(allCapsQuery.indexOf('ROLLBACK') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " ROLLBACK");
         return false;
     }else if(allCapsQuery.indexOf('SHUTDOWN') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " SHUTDOWN");
         return false;
     }else if(allCapsQuery.indexOf('INSERT') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " INSERT");
         return false;
     }else if(allCapsQuery.indexOf('ALTER') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " ALTER");
         return false;
     }else if(allCapsQuery.indexOf('UPDATE') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " UPDATE");
         return false;
     }else if(allCapsQuery.indexOf('LIMIT') > -1){
         window.alert($.i18n._('database-source')["alert-invalid-query-keyword"] + " LIMIT");
         return false;
     }
     
//	 if ((allCapsQuery.indexOf('DROP') > -1)  || (allCapsQuery.indexOf('TRUNCATE') > -1) ||
//			 (allCapsQuery.indexOf('DELETE') > -1) || (allCapsQuery.indexOf('ROLLBACK') > -1) 
//			 || (allCapsQuery.indexOf('SHUTDOWN') > -1) || (allCapsQuery.indexOf('INSERT') > -1)
//			 || (allCapsQuery.indexOf('ALTER') > -1) || (allCapsQuery.indexOf('UPDATE') > -1))
//	 {
//		 window.alert($.i18n._('database-source')["alert-invalid-query-keyword"]);
//		 return false;
//	 }
     
     if(!allCapsQuery.startsWith('SELECT')) {
         window.alert($.i18n._('database-source')["alert-invalid-query-select"]);
         return false;
     }

     return true;
};

Refine.DatabaseSourceUI.prototype._editConnection = function(connectionInfo) {	
    //alert("database user:" + connectionInfo.databaseUser);
    var self = this;
    $.ajax({
        url: 'command/database/saved-connection',
        type: 'PUT',
        contentType:'application/x-www-form-urlencoded',
        data: connectionInfo,
        success: function(settings) {
             if(settings){
              $( "#menuListUl" ).empty();
              var items = [];
              $.each(settings.savedConnections,function(index,savedConnection){
//				  items.push('<a href="#" class="list-group-item list-group-item-action">'
//						    + '<span class="context-menu-one context-menu-text" >' + savedConnection.connectionName + '</span>'
//					        + '<span class="sc-context-more-vert pull-right"> </span> </a>');
                  
                  items.push('<li class="pure-menu-item sc-list"><a href="#" class="pure-menu-link context-menu-one">'
                    + '<span class="context-menu-text" >' + savedConnection.connectionName + '</span>'
                    + '<span class="sc-context-more-vert pull-right"> </span></a></li>');
               })
              
              $( "#menuListUl" ).append(items.join(''));
              window.alert($.i18n._('database-source')["alert-connection-edit"]);   
          }
        }
    }).fail(function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });

};

Refine.DatabaseSourceUI.prototype._executeQuery = function(jdbcQueryInfo) {
    var self = this;
    //remove start line
    
    var dismiss = DialogSystem.showBusy($.i18n._('database-import')["checking"]);
    //$("#executeQueryBtn").text('Please wait ...').attr('disabled','disabled');
    
    $.post(
      "command/database/test-query",
      jdbcQueryInfo,
      function(jdbcConnectionResult) {
         // $("#executeQueryBtn").text('Preview Query Result').removeAttr('disabled');
          dismiss();
          self._controller.startImportingDocument(jdbcQueryInfo);
              
      },
      "json"
    ).fail(function( jqXhr, textStatus, errorThrown ){
        //$("#executeQueryBtn").text('Preview Query Result').removeAttr('disabled');
        dismiss();
        alert( textStatus + ':' + errorThrown );
    });
    //remove end line
    
    //self._controller.startImportingDocument(jdbcQueryInfo);
}

Refine.DatabaseSourceUI.prototype._saveConnection = function(jdbcConnectionInfo) {	
    var self = this;
    $.post(
      "command/database/saved-connection",
      jdbcConnectionInfo,
      function(settings) {
          if(settings){
              
//			  self._elmts.scListGroupDiv.empty();
              self._elmts.menuListUl.empty();
              var items = [];
              $.each(settings.savedConnections,function(index,savedConnection){
                                                   
//				  items.push('<a href="#" class="list-group-item list-group-item-action">'
//						    + '<span class="context-menu-one context-menu-text" >' + savedConnection.connectionName + '</span>'
//					        + '<span class="sc-context-more-vert pull-right"> </span> </a>');
                  
                  items.push('<li class="pure-menu-item sc-list"><a href="#" class="pure-menu-link context-menu-one">'
                    + '<span class="context-menu-text" >' + savedConnection.connectionName + '</span>'
                    + '<span class="sc-context-more-vert pull-right"> </span></a></li>');
               })
              
              self._elmts.menuListUl.append(items.join(''));
          }
    
      },
      "json"
    ).fail(function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });

};

Refine.DatabaseSourceUI.prototype._loadSavedConnections = function() {	
    var self = this;
    $.get(
      "command/database/saved-connection",
      null,
      function(settings) {
          if(settings){
    
              self._elmts.menuListUl.empty();
                //self._elmts.scListGroupDiv.empty();
              var items = [];
              $.each(settings.savedConnections,function(index,savedConnection){
                  
//				  items.push('<a href="#" class="list-group-item list-group-item-action context-menu-one">'
//				    + '<span class="context-menu-text" >' + savedConnection.connectionName + '</span>'
//			        + '<span class="sc-context-more-vert pull-right"> </span> </a>');
                      
                  items.push('<li class="pure-menu-item sc-list"><a href="#" class="pure-menu-link context-menu-one">'
                    + '<span class="context-menu-text" >' + savedConnection.connectionName + '</span>'
                    + '<span class="sc-context-more-vert pull-right"> </span></a></li>');

               })
             
              self._elmts.menuListUl.append(items.join(''));
            //  self._elmts.scListGroupDiv.append(items.join(''));
          }
        
      },
      "json"
    );

};

Refine.DatabaseSourceUI.prototype._testDatabaseConnect = function(jdbcConnectionInfo) {
    
    var self = this;
    $.post(
      "command/database/test-connect",
      jdbcConnectionInfo,
      function(jdbcConnectionResult) {
          if(jdbcConnectionResult && jdbcConnectionResult.connectionResult == true){
              window.alert("Test Connection Succeeded!");
          }else{
              window.alert("Unable to establish connection to database");
          }
              
      },
      "json"
    ).fail(function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });
};

Refine.DatabaseSourceUI.prototype._connect = function(jdbcConnectionInfo) {
    
    var self = this;
    $.post(
      "command/database/connect",
      jdbcConnectionInfo,
      function(databaseInfo) {
          
          if(databaseInfo){
              $( "#currentConnectionNameInput" ).val(jdbcConnectionInfo.connectionName);
              $( "#currentDatabaseTypeInput" ).val(jdbcConnectionInfo.databaseType);
              $( "#currentDatabaseUserInput" ).val(jdbcConnectionInfo.databaseUser);
              $( "#currentDatabasePasswordInput" ).val(jdbcConnectionInfo.databasePassword);
              $( "#currentDatabaseHostInput" ).val(jdbcConnectionInfo.databaseServer);
              $( "#currentDatabasePortInput" ).val(jdbcConnectionInfo.databasePort);
              $( "#currentInitialDatabaseInput" ).val(jdbcConnectionInfo.initialDatabase);
              
              var connectionParam = "Connection :: "
                      + "jdbc:"
                    + jdbcConnectionInfo.databaseType + "://"
                    + jdbcConnectionInfo.databaseServer + ":"
                    + jdbcConnectionInfo.databasePort + "/"
                    + jdbcConnectionInfo.initialDatabase;
              
              //alert("connectionParam::" + connectionParam);
              $( "#connectionParameterSpan" ).text(connectionParam);
            //   self._body.find('.newConnectionDiv').hide();
            // 	self._body.find('.sqlEditorDiv').show();
                $( "#newConnectionDiv" ).hide();
                $( "#sqlEditorDiv" ).show();
            
          }else{
              window.alert("Unable to establish connection to database");
              return;
          }
              
      },
      "json"
    ).fail(function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });

};

Refine.DatabaseSourceUI.prototype._getConnectionInfo = function() {
         var self = this;
        var jdbcConnectionInfo = {};
         jdbcConnectionInfo.connectionName = $.trim(self._elmts.connectionNameInput[0].value);
         jdbcConnectionInfo.databaseType = $.trim(self._elmts.databaseTypeSelect[0].value);
         jdbcConnectionInfo.databaseServer = $.trim(self._elmts.databaseHostInput[0].value);
         jdbcConnectionInfo.databasePort = $.trim(self._elmts.databasePortInput[0].value);
         jdbcConnectionInfo.databaseUser = $.trim(self._elmts.databaseUserInput[0].value);
         jdbcConnectionInfo.databasePassword = $.trim(self._elmts.databasePasswordInput[0].value); 
         jdbcConnectionInfo.initialDatabase = $.trim(self._elmts.initialDatabaseInput[0].value);
         jdbcConnectionInfo.initialSchema = $.trim(self._elmts.initialSchemaInput[0].value);
         return jdbcConnectionInfo;
    
}

Refine.DatabaseSourceUI.prototype._validateNewConnectionForm = function() {
    
        var self = this;
        var connectionNameInput = $.trim(self._elmts.connectionNameInput[0].value);
        var databaseTypeSelect = $.trim(self._elmts.databaseTypeSelect[0].value);
        var databaseHostInput = $.trim(self._elmts.databaseHostInput[0].value);
        var databasePortInput = $.trim(self._elmts.databasePortInput[0].value);
        var databaseUserInput = $.trim(self._elmts.databaseUserInput[0].value);
        var databasePasswordInput = $.trim(self._elmts.databasePasswordInput[0].value);   
        var initialDatabaseInput = $.trim(self._elmts.initialDatabaseInput[0].value);
        var initialSchemaInput = $.trim(self._elmts.initialSchemaInput[0].value);
        
        if (databaseHostInput.length === 0) {
            window.alert($.i18n._('database-source')["alert-server"]);
            return false;
        }else if(databasePortInput.length === 0){
                window.alert($.i18n._('database-source')["alert-port"]);
                return false;
        }else if(databaseUserInput.length === 0){
                window.alert($.i18n._('database-source')["alert-user"]);
                return false;
        }else if(initialDatabaseInput.length === 0){
                window.alert($.i18n._('database-source')["alert-initial-database"]);
                return false;
        }
        else{
                return true;

        }    
    
       return true;
};	

Refine.DatabaseSourceUI.prototype._resetDatabaseImportForm = function() {	
    var self = this;
    $( "#connectionName" ).val("127.0.0.1");
    $( "#databaseTypeSelect" ).val("postgresql");
    $( "#databaseHost" ).val("127.0.0.1");
    $( "#databasePort" ).val("5432");
    $( "#databaseUser" ).val("postgres");
    $( "#databasePassword" ).val("");
    $( "#initialDatabase" ).val("");
    $( "#initialSchema" ).val("");
    
    $( "#editConnectionControlDiv" ).hide();
    $( "#newConnectionControlDiv" ).show();
    $('#connectionName').removeAttr('readonly');
    
};
