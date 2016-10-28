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
  container.css("top", Math.round((overlay.height() - elmt.height()) / 2) + "px");
  elmt.css("visibility", "visible");

  container.draggable({ handle: '.dialog-header', cursor: 'move' });

  var layer = {
    overlay: overlay,
    container: container,
    onCancel: onCancel
  };
  DialogSystem._layers.push(layer);

  var level = DialogSystem._layers.length;

  return level;
};

DialogSystem.dismissLevel = function(level) {
    var layer = DialogSystem._layers[level];

    $(document).unbind("keydown", layer.keyHandler);

    layer.overlay.remove();
    layer.container.remove();
    layer.container.unbind();

    if (layer.onCancel) {
      try {
        layer.onCancel();
      } catch (e) {
        Refine.reportException(e);
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
  $('<span>').html(" " + (message || $.i18n._('core-util-enc')["working"]+"...")).appendTo(body);

  var level = DialogSystem.showDialog(frame);

  return function() {
    DialogSystem.dismissUntil(level - 1);
  };
};

