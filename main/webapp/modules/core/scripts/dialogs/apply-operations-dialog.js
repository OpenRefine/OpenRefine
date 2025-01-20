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

function ApplyOperationsDialog() {
  var self = this;
  var frame = $(DOM.loadHTML("core", "scripts/dialogs/apply-operations-dialog.html"));
  var elmts = DOM.bind(frame);
  
  elmts.dialogHeader.html($.i18n('core-project/apply-operation'));
  elmts.or_proj_pasteJson.html($.i18n('core-project/paste-json'));

  elmts.operationJsonButton.on('click', async function() {
    const fileInput = elmts.operationJsonButton[0];
    fileInput.accept = '.json';
    fileInput.onchange = async function() {
      const file = fileInput.files[0];
      const reader = new FileReader();
      reader.onload = function(e) {
        try {
          const fileContent = JSON.parse(e.target.result);
          const textAreaElement = elmts.textarea[0];
          if (textAreaElement) {
            textAreaElement.textContent = JSON.stringify(fileContent, null, 2)
          }
        } catch (error) {
          elmts.errorContainer.text($.i18n('core-project/json-invalid', e.message));   
        }
      };
      reader.readAsText(file);
    };
    fileInput.click();
  });
  elmts.textarea.on('change', function() {
     elmts.errorContainer.empty();
  });
  
  elmts.applyButton.html($.i18n('core-buttons/perform-op'));
  elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  elmts.operationJsonButton.html($.i18n('core-buttons/select'));

  var fixJson = function(json) {
    json = json.trim();
    if (!json.startsWith("[")) {
      json = "[" + json;
    }
    if (!json.endsWith("]")) {
      json = json + "]";
    }

    return json.replace(/\}\s*\,\s*\]/g, "} ]").replace(/\}\s*\{/g, "}, {");
  };

  elmts.applyButton.on('click',function() {
    var json;

    try {
      json = elmts.textarea[0].value;
      json = fixJson(json);
      json = JSON.parse(json);
    } catch (e) {
      elmts.errorContainer.text($.i18n('core-project/json-invalid', e.message));   
      return;
    }

    Refine.postCoreProcess(
        "apply-operations",
        {},
        { operations: JSON.stringify(json) },
        { everythingChanged: true },
        {
          onDone: function(o) {
            if (o.code == "pending") {
              // Something might have already been done and so it's good to update
              Refine.update({ everythingChanged: true });
            }
            DialogSystem.dismissUntil(level - 1);
          },
          onError: function(e) {
             elmts.errorContainer.text($.i18n('core-project/json-invalid', e.message));   
          },
        }
    );
  });

  elmts.cancelButton.on('click',function() {
    DialogSystem.dismissUntil(level - 1);
  });

  var level = DialogSystem.showDialog(frame);

  elmts.textarea.trigger('focus');
};

