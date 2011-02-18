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

var theImportJob = {};
var ui = {};

var Refine = {
};

$(function() {
  ui.elmts = DOM.bind($('#body'));

  var resize = function() {
      var header = $("#header");
      var body = $('#body');
      var bodyPadding = 10;
    
      var bodyTop = header.outerHeight() + header.offset().top;
      var bodyHeight = $(window).height() - bodyTop - bodyPadding;
      var bodyWidth = $(window).width() - 2 * bodyPadding;
      body.css("left", bodyPadding + "px")
        .css("top", bodyTop + "px")
        .css("width", bodyWidth + "px")
        .css("height", bodyHeight + "px");
    
      // File selection stage
      var fileSelectionStageControlPanelHeight = 150; // pixel
      ui.elmts.fileSelectionStageControlPanel.height(fileSelectionStageControlPanelHeight + "px");
      ui.elmts.fileSelectionStageFilePanel.height(
        (bodyHeight -
          ui.elmts.headerPanel.outerHeight() - 
          fileSelectionStageControlPanelHeight) + "px");
    
      // Data preview stage
      var dataPreviewStageDataPanelHeight = 200; // pixel
      ui.elmts.dataPreviewStageDataPanel.height(dataPreviewStageDataPanelHeight + "px");
      ui.elmts.dataPreviewStageControlPanel.height(
        (bodyHeight -
          ui.elmts.headerPanel.outerHeight() - 
          dataPreviewStageDataPanelHeight) + "px");
  };
  $(window).bind("resize", resize);
  resize();
});
