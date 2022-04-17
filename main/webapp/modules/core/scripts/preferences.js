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


/* * * * * * * * * *       Core       * * * * * * * * * */

var Core = {};
Core.Debugging = true;

Core.i18n = function(key, defaultValue) {
  if(!key && Core.Debugging) { console.log("Error: Core.i18n() failed. No key."); }

  var translatedMessage = $.i18n(key);
  
  if(translatedMessage == "" || translatedMessage == key) {
    if(Core.Debugging) { console.log("Error: $.i18n() failed. No key: "+ key); }
    
    translatedMessage = defaultValue ? defaultValue : key;
  }
  
  return translatedMessage;
}
keyTest = "core-index/prefs-loading-failed"; resultTest = Core.i18n(keyTest, "UnhandledValue");
if(resultTest != "" && resultTest ) 
   console.log("Core.i18n translated "+ keyTest +".");
else 
  console.log("Error when assigning ValAB to variable test-preference");


Core.alertDialog = function(alertText) {
  window.alert(alertText);
  if(Core.Debugging) { debugger; }
}
// Core.alertDialog("Test of a dialog.");


/* * * * * * * * * *      TESTS       * * * * * * * * * *

API.GET(true, "command/core/load-language")
  .then((language) => { console.log("Language: "+ language); } )
  .catch((err)     => { console.log("Error API.GET() can't read load-language"); });

// API.GET(false, "command/core/load-language")

// API.POST(true, "command/core/load-language").then((data) => { console.log(data); } );


API.Core.GetCsrfToken(true)
  .then((token) => { console.log("Token: "+ token); })
  .catch((err)  => { console.log("Error API.CORE.GetCsrfToken() can't get token"); });

/* * * * * * * * * *       API       * * * * * * * * * */

var API = { Core: {} }; 

API.NewError = function(err) {
  if(Core.Degugging) console.log("An error has occured.");
  return new Error(err);
}

API.NewPromise = function(apiCommand, promiseDef) {
  apiPromise = new Promise(promiseDef);
  apiPromise.command = apiCommand;
  
  return apiPromise;
}

API.Reject = function(reject, data) {
  if(Core.Degugging) console.log("An error has occured.");
  reject(data);
}

API.fail = function(reject, jqXHR, textStatus, errorThrown) {
  if(typeof errorThrown != "object") { errorThrown = API.NewError(errorThrown); }
        
  errorThrown.jqXHR = jqXHR; 
  errorThrown.textStatus = textStatus;          
  API.Reject(reject, errorThrown);
}

API.error = function(reject, event, jqxhr, settings, thrownError) {
  console.log({ event, jqxhr, settings, thrownError } );
  API.Reject(reject, thrownError);
}

API.GET = function(isAsync, url, queryData) { 
  if(isAsync) return new Promise((resolve, reject) => {
    $.get(url, queryData, ( response, textStatus, jqXHR ) => { resolve( jqXHR ) }, "json" )
      .fail(  ( jqXHR, textStatus, errorThrown )      => API.fail(reject, jqXHR, textStatus, errorThrown) )
      .error( ( event, jqxhr, settings, thrownError ) => API.error(reject, event, jqxhr, settings, thrownError) )
  });
  
  ajaxResult = $.ajax({
    async: true,
	 url: url,
	 method: "get",
	 data: queryData,
	 dataType: "json"
  });
  
  
}

API.POST = function(isAsync, url, queryData, postData) {
  if(isAsync) return new Promise((resolve, reject) => {
    var fullUrl = queryData ? url +"?"+ $.param(queryData) : url;
    
    $.post(fullUrl, postData, function( response, textStatus, jqXHR ) { resolve( jqXHR ) }, "json" )
      .fail(  ( jqXHR, textStatus, errorThrown )      => API.fail(reject, jqXHR, textStatus, errorThrown) )
      .error( ( event, jqxhr, settings, thrownError ) => API.error(reject, event, jqxhr, settings, thrownError) )
  });
  
  return 
}

API.Core.GetCommand = function(isAsync, command, queryData) {
  return API.GET("command/core/"+ command, queryData);
}
API.Core.GetCommand(true, "load-language")
  .then((data) => { console.log("Data: "+ data); } )
  .catch((err) => { console.log("Error API.CORE.GetCommand() can't read load-language"); });

API.Core.PostCommand = function(isAsync, command, queryData, postData) {
  if(isAsync) return API.AsyncPOST("command/core/"+ command, queryData, postData);
  API.SyncPOST("command/core/"+ command, queryData, postData);
}
//API.Core.GetCommand(true, "load-language").then((data) => { console.log(data); } );

API.Core.GetCsrfToken = function(isAsync) {
  const apiCommand = "get-csrf-token";
  
  if(isAsync) return API.NewPromise(apiCommand, (resolve, reject) => {
    API.Core.GetCommand( , {} )
      .then( (data) => { resolve( data['token'] ); } )
      .catch( (err) => { reject(err); } );
  });
}

API.Core.PostCommandCsrf = function(command, queryData, postData) {
  return new Promise((resolve, reject) => {
    API.Core.GetCsrfToken()
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
//API.Core.GetCommand("load-language").then((data) => { console.log(data); } );

API.Core.GetAllPreferences = function() {
  const apiCommand = "get-csrf-token";

  return new Promise((resolve, reject) => {
    API.Core.PostCommand( "get-all-preferences", {} )
      .then( (jqXHR) => { resolve(jqXHR.response); } )
      .catch( (err) => { reject(err); } );
  })
}
API.Core.GetAllPreferences()
  .then((prefs) => { console.log("Preferences: "+ prefs); })
  .catch((err)  => { console.log("Error API.CORE.GetAllPreferences() can't get preferences"); });

API.Core.SetPreferences = function(key, newValue) {
  return new Promise((resolve, reject) => {
    API.Core.PostCommandCsrf( "set-preference", $.param({ name: key }), { value: JSON.stringify(newValue) } )
      .then( (jqXHR) => { resolve(); } )
      .catch( (err) => { reject(err); } );
  });
}
API.Core.SetPreferences("test-preference", "ValAB")
  .then(() => { console.log("test-preference a maintenant la valeur ValAB"); } )
  .catch(() => { console.log("Error when assigning ValAB to variable test-preference"); });

API.Core.LoadLanguage = function(lang) {
  return new Promise((resolve, reject) => {
    API.Core.PostCommand( "load-language", {}, { module : "core", lang } )
      .then( (data) => { resolve(data); } )
      .catch( (err) => { reject(err); } );
  })
}


/* * * * * * * * * *   Preferences   * * * * * * * * * */

var Preferences = {};

Preferences.Load = function() {
  return new Promise((resolve, reject) => {
    API.Core.GetAllPreferences()
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
Preferences.Load()
  .then(() => { console.log("The preferences were loaded."); } )
  .catch(() => { console.log("Error while loading the preferences."); });

Preferences.getValue = function(key, defaultValue) { 
  if(!Preferences.values.hasOwnProperty(key)) { return defaultValue; }

  return Preferences.values[key];
}
 
Preferences.setValue = function(key, newValue) { 
	return new Promise((resolve, reject) => {
		API.Core.SetPreferences(key, newValue)
			.then( () => { Preferences.values[key] = newValue; resolve(); } )
			.catch( (err) => { Core.alertDialog("Can save value."); reject(err); } );
	});
}
API.Core.SetPreferences("test-preference", "ValAB")
  .then(() => { console.log("test-preference a maintenant la valeur ValAB"); } )
  .catch(() => { console.log("Error when assigning ValAB to variable test-preference"); });


/* * * * * * * * * *   Languages   * * * * * * * * * */

var Languages = {};

Languages.i18n = function(key, defaultValue) {
  if(!key && Core.Debugging) { console.log("Error: Core.i18n() failed. No key."); }

  var translatedMessage = $.i18n(key);
  
  if(translatedMessage == "" || translatedMessage == key) {
    if(Core.Debugging) { console.log("Error: $.i18n() failed. No key: "+ key); }
    
    translatedMessage = defaultValue ? defaultValue : key;
  }
  
  return translatedMessage;
}
keyTest = "core-index/prefs-loading-failed"; resultTest = Core.i18n(keyTest, "UnhandledValue");
if(resultTest != "" && resultTest ) 
   console.log("Languages.i18n translated "+ keyTest +".");
else 
  console.log("Error of Languages.i18n: when assigning ValAB to variable test-preference");

Languages.Load = function() {
  return new Promise((resolve, reject) => {
    API.Core.LoadLanguage()
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
Languages.Load()
  .then(() => { console.log("The language were loaded."); } )
  .catch(() => { console.log("Error while loading the language."); });


Languages.setDefaultLanguage = function() {
  Languages.lang = (navigator.language || navigator.userLanguage).split("-")[0];
  // Languages.Load();
  
  $.i18n().load(Languages.dictionary, Languages.lang);
  $.i18n().locale = Languages.lang;
}


/* * * * * * * * * *       UI       * * * * * * * * * */

var preferenceUIs = [];
Languages.setDefaultLanguage();
Languages.Load();

Core.alertDialog = function(alertText) { window.alert(alertText); }

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
  $(td1).text((initialValue !== null) ? initialValue : "");

  var td2 = tr.insertCell(2);

  $('<button class="button">').text(Core.i18n('core-index/edit')).appendTo(td2).click(function() {
    var newValue = window.prompt(Core.i18n('core-index/change-value')+" " + key, $(td1).text());
    if (newValue == null) { return; } // @todo old behavior kept, but should be handled.
    
	newValue = (key === "userMetadata") ? deDupUserMetaData(newValue) : newValue;        

	Preferences.setValue(key, newValue);

	$(td1).text(newValue);
  });

  $('<button class="button">').text(Core.i18n('core-index/delete')).appendTo(td2).click(function() {
    if (!window.confirm(Core.i18n('core-index/delete-key')+" " + key + "?")) { return }
    Preferences.setValue(key);
      
    $(tr).remove();
	for (var i = 0; i < preferenceUIs.length; i++) {
      if (preferenceUIs[i] !== self) { continue; }
        
      preferenceUIs.splice(i, 1);
      break;
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
		
	value = (key === "userMetadata") ? deDupUserMetaData(value) : value;        
		
	Preferences.setValue(key, value);
  });
}

function onLoad() { Preferences.Load().then( (data) => { populatePreferences(); }); }

$(onLoad);
