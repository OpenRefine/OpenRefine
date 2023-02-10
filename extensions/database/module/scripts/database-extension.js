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

var DatabaseExtension = {};

DatabaseExtension.currentConnection = {};

DatabaseExtension.handleSavedConnectionClicked = function(menuKey, connectionName) {
    var jdbcConnectionInfo = {};
    jdbcConnectionInfo.connectionName = connectionName;
    
    if(menuKey === "edit"){
        DatabaseExtension.handleEditConnectionClicked(connectionName);
        
    }else if(menuKey === "delete"){
        DatabaseExtension.handleDeleteConnectionClicked(connectionName);
        
    }else if(menuKey === "connect"){
        DatabaseExtension.handleConnectClicked(connectionName);
    }

};


DatabaseExtension.handleConnectClicked = function(connectionName) {
    
     $.get(
             "command/database/saved-connection" + '?' + $.param({"connectionName": connectionName}),
             null,
             
             function(savedDatabaseConfig) {
                 
                 if(savedDatabaseConfig){
                   
                     var savedConfig = savedDatabaseConfig.savedConnections[0];
                     var databaseConfig = {};
                     databaseConfig.connectionName = savedConfig.connectionName;
                     databaseConfig.databaseType = savedConfig.databaseType;
                     databaseConfig.databaseServer = savedConfig.databaseHost;
                     databaseConfig.databasePort = savedConfig.databasePort;
                     databaseConfig.databaseUser = savedConfig.databaseUser;
                     databaseConfig.databasePassword = savedConfig.databasePassword;
                     databaseConfig.initialDatabase = savedConfig.databaseName;
                     databaseConfig.initialSchema = savedConfig.databaseSchema;
                    
                        Refine.postCSRF(
                                "command/database/connect",
                                 databaseConfig,
                                 
                                 function(databaseInfo) {
                                    
                                    if(databaseInfo){
                                        DatabaseExtension.currentConnection.databaseInfo;
                                        $( "#currentConnectionNameInput" ).val(databaseConfig.connectionName);
                                  $( "#currentDatabaseTypeInput" ).val(databaseConfig.databaseType);
                                  $( "#currentDatabaseUserInput" ).val(databaseConfig.databaseUser);
                                  $( "#currentDatabasePasswordInput" ).val(databaseConfig.databasePassword);
                                  $( "#currentDatabaseHostInput" ).val(databaseConfig.databaseServer);
                                  $( "#currentDatabasePortInput" ).val(databaseConfig.databasePort);
                                  $( "#currentInitialDatabaseInput" ).val(databaseConfig.initialDatabase);
                                  
                                  var connectionParam = "Connection[" + databaseConfig.connectionName + "] :: "
                                          + "jdbc:"
                                        + databaseConfig.databaseType + "://"
                                        + databaseConfig.databaseServer + ":"
                                        + databaseConfig.databasePort + "/"
                                        + databaseConfig.initialDatabase;
                                 
                                  $( "#connectionParameterSpan" ).text(connectionParam);
                                        $( "#newConnectionDiv" ).hide();
                                        $( "#sqlEditorDiv" ).show();
                                       
                                    }else{
                                        window.alert("Unable to establish connection to database");
                                    }
                                        
                                },
                                "json",
                                function( jqXhr, textStatus, errorThrown ){
                                    alert( textStatus + ':' + errorThrown );
                     });
                   
                 }
                     
             },
             "json"
   );
};

DatabaseExtension.handleDeleteConnectionClicked = function(connectionName) {
    $.ajax({
        url: 'command/database/saved-connection' + '?' + $.param({"connectionName": connectionName}),
        type: 'DELETE',
        contentType:'application/json',
        data: null,
        success: function(settings) {
             if(settings){
                  
                  $( "#menuListUl" ).empty();
                  var items = [];
                  $.each(settings.savedConnections,function(index,savedConnection){					  
                      items.push('<li class="sc-list"><a href="#" class="database-menu-item context-menu-one">'
                        + '<span class="context-menu-text" >' + savedConnection.connectionName + '</span>'
                        + '<span class="sc-context-more-vert pull-right"> </span></a></li>');
                   })
                  
                  $( "#menuListUl" ).append(items.join(''));
              }
        }
    }).fail(function( jqXhr, textStatus, errorThrown ){
        alert( textStatus + ':' + errorThrown );
    });
}

DatabaseExtension.handleEditConnectionClicked = function(connectionName) {
  $.get(
    "command/database/saved-connection" + '?' + $.param({ "connectionName": connectionName }),
    null,
    function(savedDatabaseConfig) {
      if (savedDatabaseConfig) {
        var savedConfig = savedDatabaseConfig.savedConnections[0];

        $( "#connectionName" ).val(savedConfig.connectionName);
        $( "select#databaseTypeSelect" ).val(savedConfig.databaseType);
        Refine.DatabaseSourceUI.prototype._updateDatabaseType(savedConfig.databaseType);
        
        $( "#databaseHost" ).val(savedConfig.databaseHost);
        $( "#databasePort" ).val(savedConfig.databasePort);
        $( "#databaseUser" ).val(savedConfig.databaseUser);
        $( "#databasePassword" ).val(savedConfig.databasePassword);
        $( "#initialDatabase" ).val(savedConfig.databaseName);
        $( "#initialSchema" ).val(savedConfig.databaseSchema);
        $( "#newConnectionControlDiv" ).hide();
        $( "#editConnectionControlDiv" ).show();
        $( "#newConnectionDiv" ).show();
        $('#sqlEditorDiv').hide();
        $("#connectionName").attr('readonly', 'readonly');
      }
    },
    "json"
  );
}
