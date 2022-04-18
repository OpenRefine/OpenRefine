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


/* * * * * * * * * *      TESTS       * * * * * * * * * *

//* * *   CORE   * * *
// Core.alertDialog("Test of a dialog.");

// keyTest = "core-index/prefs-loading-failed"; resultTest = Core.i18n(keyTest, "UnhandledValue");
// if(resultTest != "" && resultTest )
//    Core.Log("Core.i18n translated "+ keyTest +".");
// else
//   Core.Log("Error when assigning ValAB to variable test-preference");


//* * *   API   * * *
API.GET("command/core/load-language")
  .then((language) => { Core.Log("Language: "+ language); } )
  .catch((err)     => { Core.Log("Error API.GET() can't read load-language"); });

API.GET("command/core/load-language", {}, true);

API.POST("command/core/load-language", {}, {}, true)
     .then((data) => { Core.Log(data); } );

API.Core.GetCsrfToken()
  .then((token) => { Core.Log("Token: "+ token); })
  .catch((err)  => { Core.Log("Error API.CORE.GetCsrfToken() can't get token"); });

API.Core.GetCommand(true, "load-language")
  .then((data) => { Core.Log("Data: "+ data); } )
  .catch((err) => { Core.Log("Error API.CORE.GetCommand() can't read load-language"); });

// API.Core.PostCommand(true, "load-language").then((data) => { Core.Log(data); } );

// API.Core.PostCommandCsrf("load-language").then((data) => { Core.Log(data); } );

API.Core.GetAllPreferences()
  .then((prefs) => { Core.Log("Preferences: "+ prefs); })
  .catch((err)  => { Core.Log("Error API.CORE.GetAllPreferences() can't get preferences"); });

API.Core.SetPreferences("test-preference", "ValAB")
  .then(() => { Core.Log("test-preference a maintenant la valeur ValAB"); } )
  .catch(() => { Core.Log("Error when assigning ValAB to variable test-preference"); });


//* * *   PREFERENCES   * * *
Preferences.Load()
  .then(() => { Core.Log("The preferences were loaded."); } )
  .catch(() => { Core.Log("Error while loading the preferences."); });


//* * *   LANGUAGES   * * *
Languages.Load()
  .then(() => { Core.Log("The language were loaded."); } )
  .catch(() => { Core.Log("Error while loading the language."); });

keyTest = "core-index/prefs-loading-failed"; resultTest = Core.i18n(keyTest, "UnhandledValue");
if(resultTest != "" && resultTest )
   Core.Log("Languages.i18n translated "+ keyTest +".");
else
  Core.Log("Error of Languages.i18n: when assigning ValAB to variable test-preference");


/* * * * * * * * * *       CORE       * * * * * * * * * */

var Core        = {};
Core.Debugging  = true;
delete Core.Excessive;

Core.Debug      = function() { if(Core.Debugging) debugger; }

Core.Log = function(logMessage) { if(Core.Debugging) { 
  if(arguments.length > 1) { arguments.map((currentValue) => { Core.Log(currentValue); }); return; }
  console.log(logMessage);} 
}

Core.i18n = function(key, defaultValue) {
  if(!key && Core.Debugging) { Core.Log("Error: Core.i18n() failed. No key."); }

  var translatedMessage = $.i18n(key);

  if(translatedMessage == "" || translatedMessage == key) {
    if(Core.Debugging) { Core.Log("Error: $.i18n() failed. No key: "+ key); }

    translatedMessage = defaultValue ? defaultValue : key;
  }

  return translatedMessage;
}

Core.alertDialog = function(alertText) {
  if(Core.Excessive) { Core.Debug(); }
  window.alert(alertText);
}

Core.promptDialog = function(alertText) {
  if(Core.Excessive) { Core.Debug(); }
  return window.prompt(alertText);
}


/* * * * * * * * * *       API       * * * * * * * * * */

var API = { f: "json", Core: {} };

API.NewError = function(err) {
  if(Core.Debuging) Core.Log("An error has occured.");
  return new Error(err);
}

API.NewPromise = function(apiCommand, promiseDef) {
  apiPromise = new Promise(promiseDef);
  apiPromise.command = apiCommand;

  return apiPromise;
}

API.Reject = function(reject, data) {
  if(Core.Debugging) Core.Log("An error has occured.");
  reject(data);
}

API.fail = function(reject, jqXHR, textStatus, errorThrown) {
  if(typeof errorThrown != "object") { errorThrown = API.NewError(errorThrown); }

  errorThrown.jqXHR = jqXHR;
  errorThrown.textStatus = textStatus;
  API.Reject(reject, errorThrown);
}

API.error = function(reject, event, jqxhr, settings, thrownError) {
  Core.Log({ event, jqxhr, settings, thrownError } );
  API.Reject(reject, thrownError);
}

API.GET = function(url, queryData, syncMode) {
  if(syncMode === undefined) return new Promise((resolve, reject) => {
    $.get(url, queryData, ( response, textStatus, jqXHR ) => { resolve( jqXHR ) }, API.f )
      .fail(  ( jqXHR, textStatus, errorThrown )      => API.fail(reject, jqXHR, textStatus, errorThrown) )
      .error( ( event, jqxhr, settings, thrownError ) => API.error(reject, event, jqxhr, settings, thrownError) )
  });

  if(syncMode !== true) { Core.Debug(); }

  var ajaxResult = $.ajax({
       async: true,
         url: url,
      method: "get",
        data: queryData,
    dataType: API.f
  });

  Core.Log(ajaxResult);

  return;
}


API.POST = function(url, queryData, postData, syncMode) {
  if(syncMode === undefined) return new Promise((resolve, reject) => {
    var fullUrl = queryData ? url +"?"+ $.param(queryData) : url;

    $.post(fullUrl, postData, function( response, textStatus, jqXHR ) { resolve( jqXHR ) }, API.f )
      .fail(  ( jqXHR, textStatus, errorThrown )      => API.fail(reject, jqXHR, textStatus, errorThrown) )
      .error( ( event, jqxhr, settings, thrownError ) => API.error(reject, event, jqxhr, settings, thrownError) )
  });
  
  if(syncMode !== true) { Core.Debug(); }
  
  var ajaxResult = $.ajax({
       async: true,
         url: url,
      method: "post",
        data: postData,
    dataType: API.f
  });

  Core.Log(ajaxResult.responseJSON);

  return ajaxResult.responseJSON;
}

API.Core.GetCommand = function(command, queryData, syncMode) {
  if(syncMode === undefined) return API.GET("command/core/"+ command, queryData);
}

API.Core.PostCommand = function(command, queryData, postData, syncMode) {
  if(syncMode === undefined) return API.POST("command/core/"+ command, queryData, postData);
  API.SyncPOST("command/core/"+ command, queryData, postData);
}

API.Core.GetCsrfToken = function(syncMode) {
  const apiCommand = "get-csrf-token";

  if(syncMode === undefined) return API.NewPromise(apiCommand, (resolve, reject) => {
    API.Core.GetCommand(apiCommand, {} )
      .then( (data) => { resolve( data['token'] ); } )
      .catch( (err) => { reject(err); } );
  });
}

API.Core.PostCommandCsrf = function(command, queryData, postData, syncMode) {
  if(syncMode === undefined) return new Promise((resolve, reject) => {
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

API.Core.GetAllPreferences = function(syncMode) {
  const apiCommand = "get-csrf-token";

  if(syncMode === undefined) return new Promise((resolve, reject) => {
    API.Core.PostCommand( "get-all-preferences", {} )
      .then( (jqXHR) => { resolve(jqXHR.response); } )
      .catch( (err) => { reject(err); } );
  })
}

API.Core.SetPreferences = function(key, newValue, syncMode) {
  if(syncMode === undefined) return new Promise((resolve, reject) => {
    API.Core.PostCommandCsrf( "set-preference", $.param({ name: key }), { value: JSON.stringify(newValue) } )
      .then( (jqXHR) => { resolve(); } )
      .catch( (err) => { reject(err); } );
  });
}

API.Core.LoadLanguage = function(lang, syncMode) {
  if(syncMode === undefined) return new Promise((resolve, reject) => {
    API.Core.PostCommand( "load-language", {}, { module : "core", lang } )
      .then( (data) => { resolve(data); } )
      .catch( (err) => { reject(err); } );
  })
}


/* * * * * * * * * *   PREFERENCES   * * * * * * * * * */

var Preferences = {};

Preferences.Load = function(syncMode) {
  return new Promise((resolve, reject) => {
    API.Core.GetAllPreferences()
      .then( (data) => {
        Preferences.values = data;
        resolve(data);
      })
      .catch( (err) => {
        Core.Log(err);
        var errorMessage = Core.i18n('core-index/prefs-loading-failed', err.textStatus +':'+ err.errorThrown);
        Core.alertDialog(errorMessage);
        reject(err);
      });
  });
}

Preferences.getValue = function(key, defaultValue, syncMode) {
  if(!Preferences.values.hasOwnProperty(key)) { return defaultValue; }

  return Preferences.values[key];
}

Preferences.setValue = function(key, newValue, syncMode) {
  return new Promise((resolve, reject) => {
    API.Core.SetPreferences(key, newValue, syncMode)
      .then( () => { Preferences.values[key] = newValue; resolve(); } )
      .catch( (err) => { Core.alertDialog("Can save value."); reject(err); } );
  });
}


/* * * * * * * * * *   LANGUAGES   * * * * * * * * * */

var Languages = {};
Languages.UserNavigatorPref = function() { return (navigator.language || navigator.userLanguage).split("-")[0] }

Languages.i18n = function(key, defaultValue, syncMode) {
  if(!key && Core.Debugging) { Core.Log("Error: Languages.i18n() failed. No key."); }

  var translatedMessage = $.i18n(key);

  if(translatedMessage == "" || translatedMessage == key) {
    if(Core.Debugging) { Core.Log("Error: Languages.i18n() failed. No translation for key: "+ key); }

    translatedMessage = defaultValue ? defaultValue : key;
  }

  return translatedMessage;
}

Languages.Load = function(syncMode) {
  return new Promise((resolve, reject) => {
    API.Core.LoadLanguage(syncMode)
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

Languages.setDefaultLanguage = function(syncMode) {
  Languages.lang = Languages.UserNavigatorPref();
  Languages.Load(true);

  $.i18n().load(Languages.dictionary, Languages.lang);
  $.i18n().locale = Languages.lang;
}

Languages.deDupUserMetaData = function(arrObj)  {
    var result = _.uniq(JSON.parse(arrObj), function(x){
        return x.name;
    });

    return JSON.stringify(result).replace(/"/g, '\"');
}

Languages.setDefaultLanguage();
Languages.Load();

/* * * * * * * * * *       TAG       * * * * * * * * * */
var Tag = {};

Tag.Create = function(tag, attributes, parent) { 
  Tag[tag] = function(attributes, parent) { 
    return Tag.New(Tag.Attr(attributes, tag, parent)); 
  }
};

Tag.tagsName = ["body", "div", "h1", "h2", "h3", "table", "tbody", "th", "tr", "td", "form", "input", "textarea", "button"];
Tag.tags     = Tag.tagsName.map((tagName) => { Tag.Create( {}, tagName, {} ); }); // { Tag.Create(arguments[0], tagName, arguments[2]); });
// DEBUG arguments[0] : do kossé ?!
// Tag.body    = function(attributes, parent) return Tag.New(Tag.Attr(attributes, "body", parent));

/*
Tag.tags.map((object, index) => { Object.defineProperty(object, Tag.tagsName[index], { 
  get : function (value) { return Tag[object.name]; } 
//  set : Tag[object.name]  // function (value) { Tag(value) } 
}); });
*/

Tag.New = function(attributes) {
  if(this !== undefined) Core.Log(this);
  
  if(this !== undefined)
  if(arguments.length > 1) { attributes.map((newTag) => { Tag.New(newTag); }); return; }
  
  var tagParent   = parent | attributes.parent || null;
  
  if(tagParent) { parent.children.push(newTag); }
    /***  BEGIN NO ESLINT  ***/
       var newTag = new Tag;
       
     newTag.isNew = true;
      newTag.name = attributes.tag;
    newTag.parent = tagParent;
  newTag.children = [];
     newTag.class = attributes.class  || null;
        newTag.id = attributes.id     || null;
    /***  END NO ESLINT  ***/

  return newTag;
}

Tag.Attr = function(attributes, name, parent) {
  if(attributes.name === undefined) {
    if(name === undefined) { Core.Debug(); }
    attributes.name = name;
  }
  if(attributes.parent === undefined) { attributes.parent = parent || null };

  return attributes;
}


Tag.id = function(idData) {
  newTagJq  = $("#"+ idData);
  tagId     = newTag.attr("id");
  newTag    = Tag.New( Tag.Attr({ id:tagId }) );
  newTag.jq = newTagJq;
  
  return newTag;
}

DOM.body   = Tag.body;


DOM.body = Tag.id("body-info");

/* * * * * * * * * *       UI       * * * * * * * * * */

var preferenceUIs = [];

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

    newValue = (key === "userMetadata") ? Languages.deDupUserMetaData(newValue) : newValue;

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
//  var body = $("#body-info").empty();
  var bodyInfo = Tag.id("body-info");
  DOM.body.div = [ Tag.div({id:"header"}), bodyInfo, Tag.div({ id:"meetric-wrapper" }) ];

//  $("#or-proj-starting").text(Core.i18n('core-project/starting')+"...");

//  $('<h1>').text(Core.i18n('core-index/preferences')).appendTo(body);
  bodyInfo.h1 = Tag.h1({ i18n: 'core-index/preferences' });

/*
  var table = $('<table>')
  .addClass("list-table")
  .addClass("preferences")
  .html('<tr><th>'+Core.i18n('core-index/key')+'</th><th>'+Core.i18n('core-index/value')+'</th><th></th></tr>')
  .appendTo(body)[0];
*/
  prefTable = Tag.table({ 
    id:    "prefTable",
    class: [ "list-table", "preferences"],
    tr:    { th: { i18n: 'core-index/key' }, th: { i18n: 'core-index/value' }, th: {} }
  });

/*
  for (var k in Preferences.values) {
    var tr = table.insertRow(table.rows.length);
    preferenceUIs.push(new PreferenceUI(tr, k, Preferences.values[k]));
  }
*/
  // Est-ce possible de faire un map sur un JSON ? ;-) Est une Array ?
  Preferences.values.map((currentPreference) => { 
    var newRow = prefTable.tr;
    preferenceUIs.push(new PreferenceUI(newRow, currentPreference, Preferences.values[currentPreference]));
  });

//  var trLast = table.insertRow(table.rows.length);
  var trLast = prefTable.tr;
  
//  var tdLast0 = trLast.insertCell(0);
  tdLast0 = trLast.td;

//  trLast.insertCell(1); trLast.insertCell(2);
  trLast.td; trLast.td;
  
  addButton = tdLast0.button({ class: "button", i18n: 'core-index/add-pref' });
  
/*
  $('<button class="button">').text(Core.i18n('core-index/add-pref')).appendTo(tdLast0).click(function() {
    var key = window.prompt(Core.i18n('core-index/add-pref'));
    if (!key) { return; }  // @todo old behavior kept, but should be handled.

    var value = window.prompt(Core.i18n('core-index/pref-key'));
    if (value === null) { return; }  // @todo old behavior kept, but should be handled.

    var tr = table.insertRow(table.rows.length - 1);
    preferenceUIs.push(new PreferenceUI(tr, key, value));

    value = (key === "userMetadata") ? Languages.deDupUserMetaData(value) : value;

    Preferences.setValue(key, value);
*/
  addButton.click = function() {
    let prefKey = Core.promptDialog(Core.i18n('core-index/add-pref'));
    if(!prefKey) return;
    
    let value = Core.promptDialog(Core.i18n('core-index/pref-key'));
    if(!value) return; // @todo Better error management should be done.
    
    let newRow = prefTable.tr;
    preferenceUIs.push(new PreferenceUI(newRow, key, value));
    
    prefValue = (key === "userMetadata") ? Languages.deDupUserMetaData(value) : value;

    Preferences.setValue(key, prefValue);
  };
}

function onLoad() { Preferences.Load().then( (data) => { populatePreferences(data); }); }

$(onLoad);
