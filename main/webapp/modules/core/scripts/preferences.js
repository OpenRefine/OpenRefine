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

// Core.Debugging = false;

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

Core.i18n = function(key, defaultValue) {
  var errorMessage = $.i18n(key);
  
  if(!errorMessage && errorMessage != key) {
    if(Core.Debugging) { console.log("Error: $.i18n() failed. No key: "+ key); }
    
    errorMessage = defaultValue ? defaultValue : key;
  }
  
  return errorMessage;
}

Core.alertDialog = function(alertText) {
  window.alert(alertText);
}

Preferences.loadAll = function() { 
  $.post(
      "command/core/get-all-preferences",
      null,
      function(data) { Preferences.values = data; populatePreferences(); },
      "json"
  );
}

Preferences.getValue = function(key, defaultValue) { 
  if(!Preferences.values.hasOwnProperty(key)) { return defaultValue; }

  return Preferences.values[key];
}

Preferences.setValue = function(key, newValue) { 
  Preferences.values[key] = newValue;

  Core.wrapCSRF(function(token) {
    $.ajax({
      async: false,
      type: "POST",
      url: "command/core/set-preference?" + $.param({ name: key }),
      data: {
        "value" : JSON.stringify(newValue), 
        csrf_token: token
      },
      success: function(data) { },
      dataType: "json"
    });
  });
}

Languages.loadAll = function() {
  $.ajax({
    url : "command/core/load-language?",
    type : "POST",
    async : false,
    data : {
      module : "core",
      //lang : Languages.lang
    },
    success : function(data) {
      Languages.dictionary = data['dictionary'];
      Languages.lang = data['lang'];
    }
  }).fail(function( jqXhr, textStatus, errorThrown ) {
    var errorMessage = Core.i18n('core-index/langs-loading-failed', textStatus +':'+ errorThrown);

    Core.alertDialog(errorMessage); 
  });
}

Languages.setDefaultLanguage = function() {
  Languages.lang = (navigator.language || navigator.userLanguage).split("-")[0];
  Languages.loadAll();
  
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
  $(td1).text((initialValue !== null) ? initialValue : "");

  var td2 = tr.insertCell(2);

  $('<button class="button">').text(Core.i18n('core-index/edit')).appendTo(td2).click(function() {
    var newValue = window.prompt(Core.i18n('core-index/change-value')+" " + key, $(td1).text());
    if (newValue == null) { return; } // @todo old behavior kept, but should be handled.
    
		newValue = key === "userMetadata" ? deDupUserMetaData(newValue) : newValue;        

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

function onLoad() { Preferences.loadAll(); }

$(onLoad);
