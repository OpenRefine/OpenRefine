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

var OpenRefineVersion;

var Refine = {
  actionAreas: []
};

var lang = (navigator.language|| navigator.userLanguage).split("-")[0];
var dictionary = "";
$.ajax({
	url : "command/core/load-language?",
	type : "POST",
	async : false,
	data : {
	  module : "core",
//		lang : lang
	},
	success : function(data) {
		dictionary = data;
	}
});
$.i18n.setDictionary(dictionary);
// End internationalization

Refine.selectActionArea = function(id) {
  $('.action-area-tab').removeClass('selected');
  $('.action-area-tab-body').css('visibility', 'hidden').css('z-index', '50');

  for (var i = 0; i < Refine.actionAreas.length; i++) {
    var actionArea = Refine.actionAreas[i];
    if (id == actionArea.id) {
      actionArea.tabElmt.addClass('selected');
      actionArea.bodyElmt.css('visibility', 'visible').css('z-index', '55');
    }
  }
};

$(function() {
  var isThereNewRelease = function() {
    // TODO: This eeds to be modified to do checking based solely on version, not revision
    var thisRevision = OpenRefineVersion.revision;
    var thisVersion = OpenRefineVersion.version;

    var revision_pattern = /r([0-9]+)/;

    if (!revision_pattern.test(thisRevision)) { // probably "trunk"
      return false;
    }

    var latestRevision = OpenRefineReleases.releases[0].revision;
    var latestVersion = OpenRefineReleases.releases[0].version;

    var thisRev = parseInt(revision_pattern.exec(thisRevision)[1],10);
    var latestRev = parseInt(revision_pattern.exec(OpenRefineReleases.releases[0].revision)[1],10);

    // Parser version into main version and suffix which follows dash
    // Parse version from dot separated string into array of integers
    
    // compare left to right, including suffix
    return latestRev > thisRev;
  };

  var showVersion = function() {
    $.getJSON(
        "command/core/get-version",
        null,
        function(data) {
          OpenRefineVersion = data;

          $("#openrefine-version").text($.i18n._('core-index')["version"]+" " + OpenRefineVersion.full_version);

          // Format of releases.js fetched from server
//          var releases = {
//              "homepage" : "http://code.google.com/p/google-refine/wiki/Downloads",
//              "releases" : [
//        {
//        "description": "OpenRefine 2.6",
//        "version": "2.6-alpha1",
//    },
//                  {
//                      "description": "Google Refine 2.5",
//                      "version": "2.5",
//                      "revision": "r2407"
//                  },
//                  ]
//          }
          
          var script = $('<script></script>')
          .attr("src", "http://google-refine.googlecode.com/svn/support/releases.js")
          .attr("type", "text/javascript");
          document.body.appendChild(script[0]);

          var poll = function() {
            if ("releases" in window) {
              if (isThereNewRelease()) {
                var container = $('<div id="notification-container">')
                .appendTo(document.body);
                var notification = $('<div id="notification">')
                .text($.i18n._('core-index')["new-version"]+' ')
                .appendTo(container);
                $('<a>')
                .addClass('notification-action')
                .attr("href", releases.homepage)
                .text($.i18n._('core-index')["download"]+' ' + releases.releases[0].description + ' '+$.i18n._('core-index')["now"]+'.')
                .appendTo(notification);
              }
            } else {
              window.setTimeout(poll, 1000);
            }
          };
          window.setTimeout(poll, 1000);
        }
    );
  };

  var resize = function() {
    var leftPanelWidth = 150;
    // px
    var width = $(window).width();
    var height = $(window).height();
    var headerHeight = $('#header').outerHeight();
    var panelHeight = height - headerHeight;

    $('.main-layout-panel')
    .css("top", headerHeight + "px")
    .css("bottom", "0px")
    .css("height", panelHeight + "px")
    .css("visibility", "visible");

    $('#left-panel')
    .css("left", "0px")
    .css("width", leftPanelWidth + "px");
    var leftPanelBodyHPaddings = 10;
    // px
    var leftPanelBodyVPaddings = 0;
    // px
    $('#left-panel-body')
    .css("margin-left", leftPanelBodyHPaddings + "px")
    .css("margin-top", leftPanelBodyVPaddings + "px")
    .css("width", ($('#left-panel').width() - leftPanelBodyHPaddings) + "px")
    .css("height", ($('#left-panel').height() - leftPanelBodyVPaddings) + "px");

    $('#right-panel')
    .css("left", leftPanelWidth + "px")
    .css("width", (width - leftPanelWidth) + "px");

    var rightPanelBodyHPaddings = 5;
    // px
    var rightPanelBodyVPaddings = 5;
    // px
    $('#right-panel-body')
    .css("margin-left", rightPanelBodyHPaddings + "px")
    .css("margin-top", rightPanelBodyVPaddings + "px")
    .css("width", ($('#right-panel').width() - rightPanelBodyHPaddings) + "px")
    .css("height", ($('#right-panel').height() - rightPanelBodyVPaddings) + "px");
    
    for (var i = 0; i < Refine.actionAreas.length; i++) {
      Refine.actionAreas[i].ui.resize();
    }
  };
  $(window).bind("resize", resize);
  window.setTimeout(resize, 100); // for Chrome, give the window some time to layout first

  var renderActionArea = function(actionArea) {
    actionArea.bodyElmt = $('<div>')
    .addClass('action-area-tab-body')
    .appendTo('#right-panel-body');

    actionArea.tabElmt = $('<li>')
    .addClass('action-area-tab')
    .text(actionArea.label)
    .appendTo($('#action-area-tabs'))
    .click(function() {
      Refine.selectActionArea(actionArea.id);
    });

    actionArea.ui = new actionArea.uiClass(actionArea.bodyElmt);
  };

  for (var i = 0; i < Refine.actionAreas.length; i++) {
    renderActionArea(Refine.actionAreas[i]);
  }
  Refine.selectActionArea('create-project');
  
  $("#slogan").text($.i18n._('core-index')["slogan"]+".");
  $("#or-index-help").text($.i18n._('core-index')["help"]);
  $("#or-index-about").text($.i18n._('core-index')["about"]);
  $("#or-index-noProj").text($.i18n._('core-index')["no-proj"]+".");
  $("#or-index-try").text($.i18n._('core-index')["try-these"]);
  $("#or-index-sample").text($.i18n._('core-index')["sample-data"]);

  showVersion();
});
