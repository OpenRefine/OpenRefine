/*

Copyright 2011, Google Inc.
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

var Encoding = {};

Encoding.selectEncoding = function(input, onDone) {
  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/util/select-encoding-dialog.html"));
  var elmts = DOM.bind(frame);
  var level = DialogSystem.showDialog(frame);
  
  $("#select-encodings-tabs").tabs();
  
  elmts.dialogHeader.text($.i18n('core-util-enc/select-enc'));
  elmts.cancelButton.text($.i18n('core-buttons/cancel'));
  $('#or-enc-common').text($.i18n('core-util-enc/common'));
  $('#or-enc-all').text($.i18n('core-util-enc/all'));
  
  var pickEncoding = function(encoding) {
    input[0].value = encoding.code;
    DialogSystem.dismissUntil(level - 1);
    
    if (onDone) {
      onDone();
    }
  };
  var renderEncoding = function(table, encoding) {
    var tr = table.insertRow(table.rows.length);
    $('<a>')
      .text(encoding.name)
      .attr('href', 'javascript:{}')
      .on('click',function() {
        return pickEncoding(encoding);
      })
      .appendTo(tr.insertCell(0));
    $(tr.insertCell(1)).text(encoding.aliases.join(', '));
  };
  var generateEncodingList = function(container, filter) {
    var table = $('<table>').html('<tr><th>'+$.i18n('core-util-enc/encoding')+'</th><th>'+$.i18n('core-util-enc/aliases')+'</th></tr>').appendTo(container)[0];
    $.each(Refine.encodings, function() {
      if (filter === null || this.code in filter) {
        renderEncoding(table, this);
      }
    });
  };
  generateEncodingList(elmts.commonList,
    { 'US-ASCII':1, 'ISO-8859-1':1, 'UTF-8':1, 'UTF-16BE':1, 'UTF-16LE':1, 'UTF-16':1, 'windows-1252':1 });
  generateEncodingList(elmts.allList, null);
  
  elmts.cancelButton.on('click',function() {
    DialogSystem.dismissUntil(level - 1);
  });
};
