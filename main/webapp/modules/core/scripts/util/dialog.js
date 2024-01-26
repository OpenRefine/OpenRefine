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

DialogSystem = {
    _layers: []
};

var escapeKey = function(event) {
  var level = DialogSystem._layers.length;
  if (event.key == "Escape") {
      DialogSystem.dismissUntil(level - 1);
  }
}

DialogSystem.showDialog = function(elmt, onCancel) {
  var overlay = $('<div>&nbsp;</div>')
  .addClass("dialog-overlay")
  .css("z-index", 101 + DialogSystem._layers.length * 2)
  .appendTo(document.body);

  var container = $('<div></div>')
  .addClass("dialog-container")
  .css("z-index", 102 + DialogSystem._layers.length * 2)
  .appendTo(document.body);

  elmt.css("visibility", "hidden").appendTo(container);
// Dialogs should be in the upper half of the window
// Offset by 10% of window height to separate from browser bar
// Unless dialog is as big or bigger than the window, then set top to 0 + 5px;
  var top1 = overlay.height()/10;
  var top2 = Math.round((overlay.height() - elmt.height()) / 2);
  var top = (top1 < top2) ? top1 : top2;
  container.css("top", Math.round((top < 0 ) ? 5 : top) + "px");
  elmt.css("visibility", "visible");

  container.draggable({ handle: '.dialog-header', containment: [ -32768, 0, 32768, 32768 ], cursor: 'move' });

  var layer = {
    overlay: overlay,
    container: container,
    onCancel: onCancel
  };
  DialogSystem._layers.push(layer);

  var level = DialogSystem._layers.length;

  DialogSystem.setupEscapeKeyHandling();

  elmt.attr("aria-role", "dialog");
  var dialogHeader = elmt.find(".dialog-header");
  if (dialogHeader.length && dialogHeader[0].id) {
    elmt.attr("aria-labeledby", dialogHeader[0].id);
  }

  elmt.attr("tabindex", -1);
  elmt.trigger('focus');

  return level;
};


DialogSystem.pauseEscapeKeyHandling = function() {
  $(window).off('keydown',escapeKey);
}

DialogSystem.setupEscapeKeyHandling = function() {
  $(window).on('keydown',escapeKey);
}

DialogSystem.dismissLevel = function(level) {
    var layer = DialogSystem._layers[level];

    if (layer) {
      $(document).off("keydown", layer.keyHandler);

      layer.overlay.remove();
      layer.container.remove();
      layer.container.off();

      if (layer.onCancel) {
        try {
          layer.onCancel();
        } catch (e) {
          Refine.reportException(e);
        }
      }
    }
};

DialogSystem.dismissAll = function() {
  DialogSystem.dismissUntil(0);
};

DialogSystem.dismissUntil = function(level) {
  for (var i = DialogSystem._layers.length - 1; i >= level; i--) {
	  DialogSystem.dismissLevel(i);
  }
  $(window).off('keydown', escapeKey);
  DialogSystem._layers = DialogSystem._layers.slice(0, level);
};

DialogSystem.createDialog = function() {
  return $('<div></div>').addClass("dialog-frame");
};

DialogSystem.showBusy = function(message) {
  var frame = DialogSystem.createDialog();
  frame.addClass("dialog-busy");

  var body = $('<div>').attr('id', 'loading-message').appendTo(frame);
  $('<img>').attr("src", "images/large-spinner.gif").appendTo(body);
  $('<span>').html(" " + (message || $.i18n('core-util-enc/working')+"...")).appendTo(body);

  var level = DialogSystem.showDialog(frame);

  return function() {
    DialogSystem.dismissUntil(level - 1);
  };
};

DialogSystem.alert = function (error) {

    let errorMessage = '';
    if (typeof error === 'object') {
        try {
            errorMessage = JSON.stringify(error, null, 4); // Indent with 4 spaces for readability
        } catch (e) {
            errorMessage = $.i18n('core-dialogs/could-not-stringify');
        }
    } else {
        errorMessage = error;
    }
    errorMessage = errorMessage.replace(/\r\n/g, "\n");

    let frame = DialogSystem.createDialog();
    frame.css("max-width", "50em")
    let header = $('<div></div>')
            .addClass("dialog-header")
            .append($('<span>', {
                'class': 'ui-icon ui-icon-alert',
                'style': 'float:left; margin:0 7px 5px 0;'
            }))
            .append(document.createTextNode($.i18n('core-dialogs/error')))
            .appendTo(frame);
    let body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    let footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

    let errorContent;
    if (typeof errorMessage === 'string' && !errorMessage.includes('\n')) {
        errorContent = $('<div>').text(errorMessage);
    } else {
        errorContent = $('<pre>').css({
            'white-space': 'pre-wrap',
            'word-break': 'break-all'
        }).text(errorMessage);
    }
    body.append($('<p>')).append(errorContent);

    let okButton = $('<button></button>').html($.i18n('core-buttons/ok')).on({
        'click': () => {
            DialogSystem.dismissUntil(this._level - 1);
        }
    }).css({
        'float': 'right',
        'margin': '5px'
    });
    footer.append(okButton);

    this._level = DialogSystem.showDialog(frame);
}

