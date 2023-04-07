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

I18NUtil.init("core");

Refine.wrapCSRF = CSRFUtil.wrapCSRF;
Refine.postCSRF = CSRFUtil.postCSRF;

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
  $(td2).css('white-space','nowrap');

  $('<button class="button">').text($.i18n('core-index/edit')).appendTo(td2).on('click',function() {
    var newValue = window.prompt($.i18n('core-index/change-value')+" " + key, $(td1).text());
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

  $('<button class="button">').text($.i18n('core-index/delete')).css('margin-left','5px')
      .appendTo(td2).on('click',function() {
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
  $('#app-home-button').attr('title', $.i18n('core-index/navigate-home'));

  var table = $('<table>')
  .addClass("list-table")
  .addClass("preferences")
  .html('<tr><th>'+$.i18n('core-index/key')+'</th><th>'+$.i18n('core-index/value')+'</th><th>Actions</th></tr>')
  .appendTo(body)[0];

  for (var k in prefs) {
    var tr = table.insertRow(table.rows.length);
    preferenceUIs.push(new PreferenceUI(tr, k, prefs[k]));
  }

  var trLast = table.insertRow(table.rows.length);
  var tdLast0 = trLast.insertCell(0);
  trLast.insertCell(1);
  trLast.insertCell(2);
  $('<button class="button">').text($.i18n('core-index/add-pref')).appendTo(tdLast0).on('click',function() {
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
