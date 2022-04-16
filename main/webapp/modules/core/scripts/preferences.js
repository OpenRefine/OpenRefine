/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

var Refine = {};
var Core = {};
var Languages = {};
var Preferences = {};
var API = { CORE: {} }; 

Core.Debugging = false;


API.EncodeQueryData = function(queryData) {
  return Object.entries(queryData).map(kv => kv.map(encodeURIComponent).join("=")).join("&");
}

API.GET = function(url, queryData) {
  return new Promise((resolve, reject) => {
    $.get(url, queryData, function( response, textStatus, jqXHR ) { resolve( jqXHR ) }, "json" )
      .fail(function( jqXHR, textStatus, errorThrown ) {
        if(typeof errorThrown != "object") { errorThrown = new Error(errorThrown); }
        
        errorThrown.jqXHR = jqXHR; 
        errorThrown.textStatus = textStatus;          
        reject(errorThrown);
      });
  });
}

API.CORE.GetCommand = function(command, queryData) {
  return API.GET("command/core/"+ command, queryData);
}

API.CORE.GetCsrfToken = function() {
  return new Promise((resolve, reject) => {
    API.CORE.GetCommand( "get-csrf-token", {} )
      .then( (jqXHR) => { resolve(jqXHR.response['token']); } )
      .catch( (err) => { reject(err); } );
  })
}

API.POST = function(url, queryData, postData) {
  return new Promise((resolve, reject) => {
    fullUrl = queryData ? url +"?"+ API.EncodeQueryData(queryData) : url;
    
    $.post(url, postData, function( response, textStatus, jqXHR ) { resolve( jqXHR ) }, "json" )
			.fail(function( jqXHR, textStatus, errorThrown ) {
				if(typeof errorThrown != "object") { errorThrown = new Error(errorThrown); }
		
				errorThrown.jqXHR = jqXHR; 
				errorThrown.textStatus = textStatus;          
				reject(errorThrown);
			});
	});
}

API.CORE.PostCommand = function(command, queryData, postData) {
  return API.POST("command/core/"+ command, queryData, postData);
}

API.CORE.PostCommandCsrf = function(command, queryData, postData) {
  return new Promise((resolve, reject) => {
    API.CORE.GetCsrfToken()
      .then( (token) => {
        if (typeof postData == 'string') {
          postData += "&" + $.param({csrf_token: token});
        } else {
          postData['csrf_token'] = token;
        }

        API.PostCommand(command, queryData, postData)
          .then( (resultPostData) => { resolve(resultPostData); } )
          .catch( (err) => { reject(err); } ); 
      }) 
      .catch(  (err) => { reject(err); } );
  });
}

API.CORE.GetAllPreferences = function() {
  return new Promise((resolve, reject) => {
    API.CORE.PostCommand( "get-all-preferences", {} )
      .then( (jqXHR) => { resolve(jqXHR.response); } )
      .catch( (err) => { reject(err); } );
  })
}

API.CORE.SetPreferences = function(key, newValue) {
  return new Promise((resolve, reject) => {
    API.CORE.PostCommandCsrf( "set-preference", $.param({ name: key }), { value: JSON.stringify(newValue) } )
      .then( (jqXHR) => { resolve(); } )
      .catch( (err) => { reject(err); } );
  });
}




/*
// Requests a CSRF token and calls the supplied callback
// with the token
Core.wrapCSRF = function(onCSRF) {
   $.get(
      "command/core/get-csrf-token",
      {},
      function(response) {
         onCSRF(response['token']);
      },
      "json"
   );
};

// Performs a POST request where an additional CSRF token
// is supplied in the POST data. The arguments match those
// of $.post().
Core.postCSRF = function(url, data, success, dataType, failCallback) {
   return Core.wrapCSRF(function(token) {
      var fullData = data || {};
      
      if (typeof fullData == 'string') {
         fullData = fullData + "&" + $.param({csrf_token: token});
      } else {
         fullData['csrf_token'] = token;
      }
      var req = $.post(url, fullData, success, dataType);
      
      if (failCallback !== undefined) { req.fail(failCallback); }
   });
};
*/

Core.i18n = function(key, defaultValue) {
  // const errNoKey = $.i18n("key");
  // const errNoKey = $.i18n("key");

  if(!key && Core.Debugging) { console.log("Error: Core.i18n() failed. No key."); }

  var errorMessage = $.i18n(key);
  
  if(!errorMessage || errorMessage == key) {
    if(Core.Debugging) { console.log("Error: $.i18n() failed. No key: "+ key); }
    
    errorMessage = defaultValue ? defaultValue : key;
  }
  
  return errorMessage;
}

Core.alertDialog = function(alertText) {
  window.alert(alertText);
  debugger;
}

Preferences.Load = function() {
  return new Promise((resolve, reject) => {
    API.CORE.GetAllPreferences()
      .then( (data) => { 
        Preferences.values = data; 
        resolve(data); 
      })
			.catch( (err) => {
			  console.log(err);
				var errorMessage = Core.i18n('core-index/prefs-loading-failed', err.textStatus +':'+ err.errorThrown);
				Core.alertDialog(errorMessage);
				reject(err);
			});
  });
}

Preferences.getValue = function(key, defaultValue) { 
  if(!Preferences.values.hasOwnProperty(key)) { return defaultValue; }

  return Preferences.values[key];
}

Preferences.setValue = function(key, newValue) { 
	return new Promise((resolve, reject) => {
		API.CORE.SetPreferences(key, newValue)
			.then( () => { Preferences.values[key] = newValue; resolve(); } )
			.catch( (err) => { Core.alertDialog("Can save value."); reject(err); } );
	});
}

API.CORE.LoadLanguage = function(lang) {
  return new Promise((resolve, reject) => {
    API.CORE.PostCommand( "load-language", {}, { module : "core", lang } )
      .then( (jqXHR) => { resolve(jqXHR.response); } )
      .catch( (err) => { reject(err); } );
  })
}

Languages.Load = function() {
  return new Promise((resolve, reject) => {
    API.CORE.LoadLanguage()
    .then( (data) => { 
			Languages.dictionary = data['dictionary'];
			Languages.lang = data['lang'];
			resolve();
		})
    .catch( (err) => {
      var errorMessage = Core.i18n('core-index/langs-loading-failed', err.textStatus +':'+ err.errorThrown);
      Core.alertDialog(errorMessage);
      reject(err);
    });
  });
}


Languages.setDefaultLanguage = function() {
  Languages.lang = (navigator.language || navigator.userLanguage).split("-")[0];
  Languages.Load();
  
	$.i18n().load(Languages.dictionary, Languages.lang);
	$.i18n().locale = Languages.lang;
}

var preferenceUIs = [];
Languages.setDefaultLanguage();

function deDupUserMetaData(arrObj)  {
    var result = _.uniq(JSON.parse(arrObj), function(x){
        return x.name;
    });
    
    return JSON.stringify(result).replace(/"/g, '\"');
}

function PreferenceUI(tr, key, initialValue) {
  var self = this;

  var td0 = tr.insertCell(0);
  $(td0).text(key);

  var td1 = tr.insertCell(1);
  $(td1).text((initialValue !== null)? initialValue : "");

  var td2 = tr.insertCell(2);

  $('<button class="button">').text(Core.i18n('core-index/edit')).appendTo(td2).click(function() {
    var newValue = window.prompt(Core.i18n('core-index/change-value')+" " + key, $(td1).text());
    if (newValue == null) { return; } // @todo old behavior kept, but should be handled.
    
		newValue = (key === "userMetadata")? deDupUserMetaData(newValue) : newValue;        

		Preferences.setValue(key, newValue);

		$(td1).text(newValue);
  });

  $('<button class="button">').text(Core.i18n('core-index/delete')).appendTo(td2).click(function() {
    if (window.confirm(Core.i18n('core-index/delete-key')+" " + key + "?")) {
      Preferences.setValue(key);
      
			$(tr).remove();
			for (var i = 0; i < preferenceUIs.length; i++) {
				if (preferenceUIs[i] === self) {
					preferenceUIs.splice(i, 1);
					break;
				}
			}
    }
  });
}

function populatePreferences() {
  var body = $("#body-info").empty();

  $("#or-proj-starting").text(Core.i18n('core-project/starting')+"...");
  $('<h1>').text(Core.i18n('core-index/preferences')).appendTo(body);

  var table = $('<table>')
  .addClass("list-table")
  .addClass("preferences")
  .html('<tr><th>'+Core.i18n('core-index/key')+'</th><th>'+Core.i18n('core-index/value')+'</th><th></th></tr>')
  .appendTo(body)[0];

  for (var k in Preferences.values) {
    var tr = table.insertRow(table.rows.length);
    preferenceUIs.push(new PreferenceUI(tr, k, Preferences.values[k]));
  }

  var trLast = table.insertRow(table.rows.length);
  var tdLast0 = trLast.insertCell(0);
  trLast.insertCell(1);
  trLast.insertCell(2);
  $('<button class="button">').text(Core.i18n('core-index/add-pref')).appendTo(tdLast0).click(function() {
    var key = window.prompt(Core.i18n('core-index/add-pref'));
    if (!key) { return; }  // @todo old behavior kept, but should be handled.
    
		var value = window.prompt(Core.i18n('core-index/pref-key'));
		if (!value === null) { return; }  // @todo old behavior kept, but should be handled.
		
		var tr = table.insertRow(table.rows.length - 1);
		preferenceUIs.push(new PreferenceUI(tr, key, value));
		
		value = key === "userMetadata" ? deDupUserMetaData(value) : value;        
		
		Preferences.setValue(key, value);
  });
}

function onLoad() { 
  Preferences.Load().then( (data) => { 
    populatePreferences(); 
  });
}

$(onLoad);
