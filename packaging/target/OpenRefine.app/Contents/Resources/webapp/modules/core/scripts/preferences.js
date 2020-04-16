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

var preferenceUIs = [];

var Refine = {
};

// Requests a CSRF token and calls the supplied callback
// with the token
Refine.wrapCSRF = function(onCSRF) {
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
Refine.postCSRF = function(url, data, success, dataType, failCallback) {
   return Refine.wrapCSRF(function(token) {
      var fullData = data || {};
      if (typeof fullData == 'string') {
         fullData = fullData + "&" + $.param({csrf_token: token});
      } else {
         fullData['csrf_token'] = token;
      }
      var req = $.post(url, fullData, success, dataType);
      if (failCallback !== undefined) {
         req.fail(failCallback);
      }
   });
};


var lang = (navigator.language|| navigator.userLanguage).split("-")[0];
var dictionary = "";
$.ajax({
url : "command/core/load-language?",
type : "POST",
async : false,
data : {
module : "core",
//lang : lang
},
success : function(data) {
dictionary = data['dictionary'];
lang = data['lang'];
}
});
$.i18n().load(dictionary, lang);
//End internationalization

function deDupUserMetaData(arrObj)  {
    var result = _.uniq(JSON.parse(arrObj), function(x){
        return x.name;
    });
    
    return JSON.stringify(result).replace(/"/g, '\"');
}

function PreferenceUI(tr, key, value) {
  var self = this;

  var td0 = tr.insertCell(0);
  $(td0).text(key);

  var td1 = tr.insertCell(1);
  $(td1).text((value !== null) ? value : "");

  var td2 = tr.insertCell(2);

  $('<button class="button">').text($.i18n('core-index/edit')).appendTo(td2).click(function() {
    var newValue = window.prompt($.i18n('core-index/change-value')+" " + key, value);
    if (newValue !== null) {
      if (key === "userMetadata")  {
          newValue = deDupUserMetaData(newValue);
      }
      $(td1).text(newValue);
      
      Refine.postCSRF(
        "command/core/set-preference",
        {
          name : key,
          value : JSON.stringify(newValue)
        },
        function(o) {
          if (o.code == "error") {
            alert(o.message);
          }
        },
        "json"
      );
    }
  });

  $('<button class="button">').text($.i18n('core-index/delete')).appendTo(td2).click(function() {
    if (window.confirm($.i18n('core-index/delete-key')+" " + key + "?")) {
      Refine.postCSRF(
        "command/core/set-preference",
        {
          name : key
        },
        function(o) {
          if (o.code == "ok") {
            $(tr).remove();

            for (var i = 0; i < preferenceUIs.length; i++) {
              if (preferenceUIs[i] === self) {
                preferenceUIs.splice(i, 1);
                break;
              }
            }
          } else if (o.code == "error") {
            alert(o.message);
          }
        },
        "json"
      );
    }
  });
}

function populatePreferences(prefs) {
  var body = $("#body-info").empty();

  $("#or-proj-starting").text($.i18n('core-project/starting')+"...");
  $('<h1>').text($.i18n('core-index/preferences')).appendTo(body);

  var table = $('<table>')
  .addClass("list-table")
  .addClass("preferences")
  .html('<tr><th>'+$.i18n('core-index/key')+'</th><th>'+$.i18n('core-index/value')+'</th><th></th></tr>')
  .appendTo(body)[0];

  for (var k in prefs) {
    var tr = table.insertRow(table.rows.length);
    preferenceUIs.push(new PreferenceUI(tr, k, prefs[k]));
  }

  var trLast = table.insertRow(table.rows.length);
  var tdLast0 = trLast.insertCell(0);
  trLast.insertCell(1);
  trLast.insertCell(2);
  $('<button class="button">').text($.i18n('core-index/add-pref')).appendTo(tdLast0).click(function() {
    var key = window.prompt($.i18n('core-index/add-pref'));
    if (key) {
      var value = window.prompt($.i18n('core-index/pref-key'));
      if (value !== null) {
        var tr = table.insertRow(table.rows.length - 1);
        preferenceUIs.push(new PreferenceUI(tr, key, value));
        
        if (key === "userMetadata")  {
            value = deDupUserMetaData(value);
        }
        
        Refine.postCSRF(
          "command/core/set-preference",
          {
            name : key,
            value : JSON.stringify(value)
          },
          function(o) {
            if (o.code == "error") {
              alert(o.message);
            }
          },
          "json"
        );
      }
    }
  });
}

function onLoad() {
  $.post(
      "command/core/get-all-preferences",
      null,
      populatePreferences,
      "json"
  );
}

$(onLoad);
