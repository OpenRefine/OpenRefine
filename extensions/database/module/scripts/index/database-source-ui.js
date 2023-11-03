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
  
  self._defaultDatabaseHost = "localhost";
  self._defaultDatabaseType = $( "select#databaseTypeSelect" ).val();
  if (self._defaultDatabaseType == "") { self._defaultDatabaseType = "mysql"; }
  
  $('#database-title').text($.i18n('database-import/title')); 
  $('#new-connection-legend').text($.i18n('database-source/new-connection-legend'));
  $('#connectionNameLabel').html($.i18n('database-source/connectionNameLabel'));
  $('#databaseTypeLabel').html($.i18n('database-source/databaseTypeLabel'));
  $('#databaseHostLabel').text($.i18n('database-source/databaseHostLabel'));
  $('#databasePortLabel').text($.i18n('database-source/databasePortLabel'));
  $('#databaseUserLabel').text($.i18n('database-source/databaseUserLabel'));
  $('#databasePasswordLabel').text($.i18n('database-source/databasePasswordLabel'));
  $('#databaseNameLabel').text($.i18n('database-source/databaseNameLabel'));
  $('#databaseSchemaLabel').text($.i18n('database-source/databaseSchemaLabel'));
  $('#databaseTestButton').text($.i18n('database-source/databaseTestButton'));
  $('#databaseSaveButton').text($.i18n('database-source/databaseSaveButton'));
  $('#databaseConnectButton').text($.i18n('database-source/databaseConnectButton'));
  $('#newConnectionButtonDiv').text($.i18n('database-source/newConnectionButtonDiv'));
  $('#savedConnectionSpan').text($.i18n('database-source/savedConnectionSpan'));
 
  $('input#connectionName').attr('placeholder', $.i18n('database-source/connectionNamePlaceholder'));
  $('input#databaseHost').attr('placeholder', $.i18n('database-source/databaseHostPlaceholder'));
  $('input#databasePort').attr('placeholder', $.i18n('database-source/databasePortPlaceholder'));
  $('input#databaseUser').attr('placeholder', $.i18n('database-source/databaseUserPlaceholder'));
  $('input#databasePassword').attr('placeholder', $.i18n('database-source/databasePasswordPlaceholder'));
  $('input#initialDatabase').attr('placeholder', $.i18n('database-source/databaseNamePlaceholder'));
  $('input#initialSchema').attr('placeholder', $.i18n('database-source/databaseSchemaPlaceholder'));

  $('input#connectionName').val($.i18n('database-source/connectionNameDefaultValue'));

  this._elmts.newConnectionButton.on('click',function(evt) {
      self._resetDatabaseImportForm();
      $( "#newConnectionDiv" ).show();
      $( "#sqlEditorDiv" ).hide();
    //   self._body.find('.newConnectionDiv').show();  
    //   self._body.find('.sqlEditorDiv').hide();
  });
  
  this._elmts.databaseTypeSelect.on('change',function(event) {
    var type = $( "select#databaseTypeSelect" ).val();

    self._updateDatabaseType(type);
  });
  
  var defaultDatabase = $( "select#databaseTypeSelect" ).val();
  self._updateDatabaseType(defaultDatabase);

  this._elmts.testDatabaseButton.on('click',function(evt) {
      
          if(self._validateNewConnectionForm() === true){
             self._testDatabaseConnect(self._getConnectionInfo());
          }
       
   });
  
  this._elmts.databaseConnectButton.on('click',function(evt) {
      
      if(self._validateNewConnectionForm() === true){
             self._connect(self._getConnectionInfo());
      }

   
 });
  
 this._elmts.saveConnectionButton.on('click',function(evt) {
      
       if(self._validateNewConnectionForm() == true){
            var connectionNameInput = jQueryTrim(self._elmts.connectionNameInput[0].value);
            if (connectionNameInput.length === 0) {
                window.alert($.i18n('database-source/alert-connection-name'));   
            } else{
                self._saveConnection(self._getConnectionInfo());
            }
             
       }

 });
  
  this._elmts.executeQueryButton.on('click',function(evt) {
        var jdbcQueryInfo = {};
        jdbcQueryInfo.connectionName = $( "#currentConnectionNameInput" ).val();
        jdbcQueryInfo.databaseType = $( "#currentDatabaseTypeInput" ).val();
        jdbcQueryInfo.databaseServer = $( "#currentDatabaseHostInput" ).val();
        jdbcQueryInfo.databasePort = $( "#currentDatabasePortInput" ).val();  
        jdbcQueryInfo.databaseUser = $( "#currentDatabaseUserInput" ).val();
        jdbcQueryInfo.databasePassword = $( "#currentDatabasePasswordInput" ).val();
        jdbcQueryInfo.initialDatabase = $( "#currentInitialDatabaseInput" ).val();
        jdbcQueryInfo.query = jQueryTrim($( "#queryTextArea" ).val()); 
      
        if(self.validateQuery(jdbcQueryInfo.query)) {
                self._executeQuery(jdbcQueryInfo);
        }
  
        
  });


  
  this._elmts.editConnectionButton.on('click',function(evt) {
      
      if(self._validateNewConnectionForm() == true){
               var connectionNameInput = jQueryTrim(self._elmts.connectionNameInput[0].value);
            if (connectionNameInput.length === 0) {
                window.alert($.i18n('database-source/alert-connection-name'));   
            } else{
                    self._editConnection(self._getConnectionInfo());
            }
             
       }
    
 });
  
 this._elmts.cancelEditConnectionButton.on('click',function(evt) {
     self._resetDatabaseImportForm();
        
 });
  
  //load saved connections from settings file in user home directory
  self._loadSavedConnections();
 
};//end Refine.createUI


Refine.DatabaseSourceUI.prototype.focus = function() {
};

Refine.DatabaseSourceUI.prototype.validateQuery = function(query) {
     if(!query || query.length <= 0 ) {
         window.alert($.i18n('database-source/alert-query'));
         return false;
     }
     if(!query.toUpperCase().startsWith('SELECT')) {
         window.alert($.i18n('database-source/alert-invalid-query-select'));
         return false;
     }
     return true;
};

Refine.DatabaseSourceUI.prototype._updateDatabaseType = function(databaseType) {
  if(databaseType === "postgresql") {
      $( "#databaseUser" ).val("postgres");
      $( "#databasePort" ).val("5432");
    
  } else if(databaseType === "mysql") {
      $( "#databaseUser" ).val("root");
      $( "#databasePort" ).val("3306");
    
  } else if(databaseType === "mariadb") {
      $( "#databaseUser" ).val("root");
      $( "#databasePort" ).val("3306");
    
  } else if(databaseType === "sqlite") {
      $( "#databaseUser" ).val("na");
      $( "#databasePort" ).val("0");
      $( "#databaseHost" ).val("na");
    
  } else {
      $( "#databaseUser" ).val("root");
      $( "#databasePort" ).val("3306");
      databaseType = "mysql";
  }
  
  $("div.dbtype-options").hide();
  $("div.dbtype-options.dbt-"+databaseType).show();
  
  if (databaseType == "sqlite") {
    $('#databaseNameLabel').text($.i18n('database-source/databaseFileNameLabel'));
    $('input#initialDatabase').attr('placeholder', $.i18n('database-source/databaseFileNamePlaceholder'));
    
  } else {
    $('#databaseNameLabel').text($.i18n('database-source/databaseNameLabel'));
    $('input#initialDatabase').attr('placeholder', $.i18n('database-source/databaseNamePlaceholder'));
  }
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
              var menuList = $('#menuListUl');
              var items = [];
              $.each(settings.savedConnections,function(index,savedConnection){

                 var li = $('<li class="sc-list"></li>').appendTo(menuList);
                 var a = $('<a href="#" class="database-menu-item context-menu-one"></a>').appendTo(li);
                 $('<span class="context-menu-text"></span>').text(savedConnection.connectionName)
                      .appendTo(a);
                 $('<span class="sc-context-more-vert pull-right"> </span>').appendTo(a);
               })
              
              $( "#menuListUl" ).append(items.join(''));
              window.alert($.i18n('database-source/alert-connection-edit'));   
          }
        }
    }).fail(function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });

};

Refine.DatabaseSourceUI.prototype._executeQuery = function(jdbcQueryInfo) {
    var self = this;
    //remove start line
    
    var dismiss = DialogSystem.showBusy($.i18n('database-import/checking'));

    Refine.postCSRF(
      "command/database/test-query",
      jdbcQueryInfo,
      function(jdbcConnectionResult) {
         
          dismiss();
          self._controller.startImportingDocument(jdbcQueryInfo);
              
      },
      "json",
      function( jqXhr, textStatus, errorThrown ){
       
        dismiss();
        alert( textStatus + ':' + errorThrown );
    });

}

Refine.DatabaseSourceUI.prototype._saveConnection = function(jdbcConnectionInfo) {	
    var self = this;
    Refine.postCSRF(
      "command/database/saved-connection",
      jdbcConnectionInfo,
      function(settings) {
          if(settings){
    
              self._elmts.menuListUl.empty();
              var items = [];
              $.each(settings.savedConnections,function(index,savedConnection){
             
                  items.push('<li class="sc-list"><a href="#" class="database-menu-item context-menu-one">'
                    + '<span class="context-menu-text" >' + savedConnection.connectionName + '</span>'
                    + '<span class="sc-context-more-vert pull-right"> </span></a></li>');
               })
              
              self._elmts.menuListUl.append(items.join(''));
          }
    
      },
      "json",
      function( jqXhr, textStatus, errorThrown ){
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
             
              var items = [];
              $.each(settings.savedConnections,function(index,savedConnection){
                  
                  items.push('<li class="sc-list"><a href="#" class="database-menu-item context-menu-one">'
                    + '<span class="context-menu-text" >' + savedConnection.connectionName + '</span>'
                    + '<span class="sc-context-more-vert pull-right"> </span></a></li>');

               })
             
              self._elmts.menuListUl.append(items.join(''));
        
          }
        
      },
      "json"
    );

};

Refine.DatabaseSourceUI.prototype._testDatabaseConnect = function(jdbcConnectionInfo) {
    
    var self = this;
    Refine.postCSRF(
      "command/database/test-connect",
      jdbcConnectionInfo,
      function(jdbcConnectionResult) {
          if(jdbcConnectionResult && jdbcConnectionResult.connectionResult == true){
              window.alert("Test Connection Succeeded!");
          }else{
              window.alert("Unable to establish connection to database");
          }
              
      },
      "json",
      function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });
};

Refine.DatabaseSourceUI.prototype._connect = function(jdbcConnectionInfo) {
    
    var self = this;
    Refine.postCSRF(
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
              
             
              $( "#connectionParameterSpan" ).text(connectionParam);
              $( "#newConnectionDiv" ).hide();
              $( "#sqlEditorDiv" ).show();
            
          }else{
              window.alert("Unable to establish connection to database");
              return;
          }
              
      },
      "json",
      function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });

};

Refine.DatabaseSourceUI.prototype._getConnectionInfo = function() {
     var self = this;
     var jdbcConnectionInfo = {};
     jdbcConnectionInfo.connectionName = jQueryTrim(self._elmts.connectionNameInput[0].value);
     jdbcConnectionInfo.databaseType = jQueryTrim(self._elmts.databaseTypeSelect[0].value);
     jdbcConnectionInfo.databaseServer = jQueryTrim(self._elmts.databaseHostInput[0].value);
     jdbcConnectionInfo.databasePort = jQueryTrim(self._elmts.databasePortInput[0].value);
     jdbcConnectionInfo.databaseUser = jQueryTrim(self._elmts.databaseUserInput[0].value);
     jdbcConnectionInfo.databasePassword = jQueryTrim(self._elmts.databasePasswordInput[0].value); 
     jdbcConnectionInfo.initialDatabase = jQueryTrim(self._elmts.initialDatabaseInput[0].value);
     jdbcConnectionInfo.initialSchema = jQueryTrim(self._elmts.initialSchemaInput[0].value);
     return jdbcConnectionInfo;
    
}


Refine.DatabaseSourceUI.prototype._validateNewConnectionForm = function() {
    
        var self = this;
        var connectionNameInput = jQueryTrim(self._elmts.connectionNameInput[0].value);
        var databaseTypeSelect = jQueryTrim(self._elmts.databaseTypeSelect[0].value);
        var databaseHostInput = jQueryTrim(self._elmts.databaseHostInput[0].value);
        var databasePortInput = jQueryTrim(self._elmts.databasePortInput[0].value);
        var databaseUserInput = jQueryTrim(self._elmts.databaseUserInput[0].value);
        var databasePasswordInput = jQueryTrim(self._elmts.databasePasswordInput[0].value);   
        var initialDatabaseInput = jQueryTrim(self._elmts.initialDatabaseInput[0].value);
        var initialSchemaInput = jQueryTrim(self._elmts.initialSchemaInput[0].value);
        
        var alphaNumRE = /^[a-zA-Z0-9._-]*$/;
        var numRE = /^[0-9]*$/;
        
        var alphaNumConnNameTestResult = alphaNumRE.test(connectionNameInput);
        var databaseHostTestResult = alphaNumRE.test(databaseHostInput);
        var databasePortTestResult = numRE.test(databasePortInput);
        var databaseUserTestResult = alphaNumRE.test(databaseUserInput);

        if(alphaNumConnNameTestResult == false){
        	window.alert($.i18n('database-source/alert-conn-name-invalid-character'));
        	return false;
        }else if (databaseHostInput.length === 0) {
            window.alert($.i18n('database-source/alert-server'));
            return false;
        }else if(databasePortInput.length === 0){
            window.alert($.i18n('database-source/alert-port'));
            return false;
        }else if(databaseUserInput.length === 0){
            window.alert($.i18n('database-source/alert-user'));
            return false;
        }else if(initialDatabaseInput.length === 0){
            window.alert($.i18n('database-source/alert-initial-database'));
            return false;
        }else if(databasePortTestResult == false){
        	window.alert($.i18n('database-source/alert-db-port-invalid-character'));
        	return false;
        	
        }
        else{
            return true;

        }    
    
       return true;
};	

Refine.DatabaseSourceUI.prototype._resetDatabaseImportForm = function() {
  var self = this;
  
  $( "#databaseHost" ).val(self._defaultDatabaseHost);

  $('input#connectionName').val($.i18n('database-source/connectionNameDefaultValue'));
  $( "select#databaseTypeSelect" ).val(self._defaultDatabaseType);
  self._updateDatabaseType(self._defaultDatabaseType);
  
  $( "#databasePassword" ).val("");
  $( "#initialDatabase" ).val("");
  $( "#initialSchema" ).val("");

  $( "#editConnectionControlDiv" ).hide();
  $( "#newConnectionControlDiv" ).show();
  $('#connectionName').prop('readonly',false);
};
