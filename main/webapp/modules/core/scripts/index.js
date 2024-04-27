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

I18NUtil.init("core");

Refine.wrapCSRF = CSRFUtil.wrapCSRF;
Refine.postCSRF = CSRFUtil.postCSRF;

Refine.selectActionArea = function(id) {
  $('.action-area-tab').removeClass('selected');
  $('.action-area-tab-body').css('visibility', 'hidden').css('z-index', '50');

  for (var i = 0; i < Refine.actionAreas.length; i++) {
    var actionArea = Refine.actionAreas[i];
    if (id == actionArea.id) {
      actionArea.tabElmt.addClass('selected');
      actionArea.bodyElmt.css('visibility', 'visible').css('z-index', '55');
      window.location.hash = id;
    }
  }
};

$(function() {
  var isThereNewRelease = function(thisVer,latestVer) {
    // Assumes Semantic Version numbering format of form X.Y.Z where X, Y, and Z are non-negative integer
    // Ignores any trailing letters e.g. 2.7.1-rc.1 will be interpreted as equivalent to 2.7.1
    if(thisVer == latestVer) {
      return false;
    }
    var thisVerParts = thisVer.split(".");
    var latestVerParts = latestVer.split(".");
    while(thisVerParts.length < 3)
      thisVerParts.push(0);
    while(latestVerParts.length < 3)
      latestVerParts.push(0);

    for(var i=0; i<3; i++) {
      var thisVerPartInt = parseInt(thisVerParts[i],10);
      var latestVerPartInt = parseInt(latestVerParts[i],10);
      if(thisVerPartInt == latestVerPartInt) {
        continue;
      } else {
        return !(thisVerPartInt > latestVerPartInt);
      }
    }
    return false;
  };

  var maybeShowNotifications = function() {
    $.getJSON(
        "command/core/get-version",
        null,
        function(data) {
          OpenRefineVersion = data;

          $("#openrefine-version").text($.i18n('core-index/refine-version', OpenRefineVersion.full_version));
          $("#java-runtime-version").text(OpenRefineVersion.java_runtime_name + " " + OpenRefineVersion.java_runtime_version);
          if (OpenRefineVersion.display_new_version_notice === "true") {
            showNotifications();
          }
        }
    );
  };

  var storeNotificationStatus = function(notificationStatus) {
     Refine.wrapCSRF(function(csrfToken) {
        $.post("command/core/set-preference",
          {
            name: 'notification.status',
            value: JSON.stringify(notificationStatus),
            csrf_token: csrfToken
          });
     });
  };

  var showNotifications = function() {
    $.get("command/core/get-preference",
      { "name": "notification.status" },
      function(data) {
        let notificationStatus = data.value;
        if (notificationStatus === null) {
          // this is the first time we are starting OpenRefine on this workspace.
          // Let's not prompt the user yet, wait for next time instead.
          storeNotificationStatus("promptUser");
        } else if (notificationStatus == "promptUser") {
          var container = $('<div id="notification-container">')
          var hideNotification = function() {
            container.remove();
          };
          var notification = $('<div id="notification">')
            .text($.i18n('core-index/notification-opt-in'))
            .appendTo(container);
          $('<a>')
            .addClass("notification-action")
            .attr('href', '#')
            .text($.i18n('core-buttons/yes'))
            .appendTo(notification)
            .on('click', function() {
              storeNotificationStatus("enabled");
              hideNotification();
            });
          $('<a>')
            .addClass("notification-action")
            .attr('href', '#')
            .text($.i18n('core-buttons/no'))
            .appendTo(notification)
            .on('click', function() {
              storeNotificationStatus("disabled");
              hideNotification();
            });
          var privacySpan = $('<span>')
            .addClass("notification-action")
            .text('(')
            .appendTo(notification);
          $('<a>')
            .attr('href', 'https://openrefine.org/privacy')
            .attr('target', '_blank')
            .text($.i18n('core-index/notification-privacy-info'))
            .appendTo(privacySpan);
          privacySpan.append(')');
          container
            .appendTo(document.body);
        } else if (notificationStatus == "enabled") {
            $.getJSON("https://openrefine.org/versions.json",
                function (data) {
                  if (data.events) {
                    var latestEvent = data.events[0];
                    var container = $('<div id="notification-container">')
                        .appendTo(document.body);
                    var notification = $('<div id="notification">')
                        .appendTo(container);
                    var link = $('<a/>')
                        .attr("href", latestEvent.link)
                        .attr("target", "_blank")
                        .text(latestEvent.text)
                        .appendTo(notification);
                    return; 
                  }

                  var stableReleases = data.releases.filter(release => release.stable);
                  if (!stableReleases) {
                    return;
                  }
                  var latestStableRelease = stableReleases[0];
                  var latestVersion = latestStableRelease.version;
                  var thisVersion = OpenRefineVersion.version;

                  if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                  }

                  if (isThereNewRelease(thisVersion, latestVersion)) {
                    var container = $('<div id="notification-container">')
                        .appendTo(document.body);
                    var notification = $('<div id="notification">')
                        .text($.i18n('core-index/new-version') + ' ')
                        .appendTo(container);
                    $('<a>')
                        .addClass('notification-action')
                        .attr("href", "https://openrefine.org/download")
                        .attr("target", "_blank")
                        .text($.i18n('core-index/new-version-available', latestVersion))
                        .appendTo(notification);
                  }
                });
        }
      }
    );
  };

  var resize = function() {
    for (var i = 0; i < Refine.actionAreas.length; i++) {
      if (Refine.actionAreas[i].ui.resize) {
        Refine.actionAreas[i].ui.resize();
      }
    }
  };

  var renderActionArea = function(actionArea) {
    actionArea.bodyElmt = $('<div>')
    .addClass('action-area-tab-body')
    .appendTo('#right-panel-body');

    actionArea.tabElmt = $('<li>')
    .addClass('action-area-tab')
    .append(
      $('<a>')
      .attr('href', '#' + actionArea.id)
      .text(actionArea.label)
      .on('click', function() {
        Refine.selectActionArea(actionArea.id);
        // clear action area specific query parameters
        var url = new URL(location.href);
        url.search = '';
        window.history.replaceState('', '', url);
      })
    )
    .appendTo($('#action-area-tabs'));

    actionArea.ui = new actionArea.uiClass(actionArea.bodyElmt);
  };

  for (var i = 0; i < Refine.actionAreas.length; i++) {
    renderActionArea(Refine.actionAreas[i]);
  }

  // check for url hash and select the appropriate action area
  var hash = window.location.hash;
  if (hash.length > 0) {
    hash = hash.substring(1);
    for (var i = 0; i < Refine.actionAreas.length; i++) {
      if (Refine.actionAreas[i].id === hash) {
        Refine.selectActionArea(hash);
        break;
      }
    }
  } else {
    Refine.selectActionArea('create-project');
  }

  $("#slogan").text($.i18n('core-index/slogan')+".");
  $("#or-index-pref").text($.i18n('core-index/preferences'));
  $("#or-index-help").text($.i18n('core-index/help'));
  $("#or-index-about").text($.i18n('core-index/about'));
  $("#or-index-noProj").text($.i18n('core-index/no-proj')+".");
  $("#or-index-try").text($.i18n('core-index/try-these'));
  $("#or-index-sample").text($.i18n('core-index/sample-data'));

  maybeShowNotifications();

  $(window).on("resize", resize);
  resize();
});
