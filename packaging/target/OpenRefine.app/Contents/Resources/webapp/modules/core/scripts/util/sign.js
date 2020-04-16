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

if (typeof window.Sign == 'undefined') {
  window.Sign = {
    window_position: function() {
      var position = {};

      if (typeof(window.innerWidth) == 'number') {
        // Non-IE
        position.width = window.outerWidth;
        position.height = window.outerHeight;
        position.top = window.screenY;
        position.left = window.screenX;
      } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
        // IE 6+ in 'standards compliant mode'
        position.width = document.body.clientWidth;
        position.height = document.body.clientHeight;
        position.top = window.screenTop;
        position.left = window.screenLeft;
      }

      return position;
    },

    popup : function(url, width, height, windowname) {
      width = width || 700;
      height = height || 500;

      var pos = window.Sign.window_position();
      var left = Math.floor((pos.width - width) / 2) + pos.left;
      var top = Math.floor((pos.height - height) / 2) + pos.top;

      // Chrome might fix this bug, but until then add some padding
      //  to the height of the popup for the urlbar
      var is_chrome = /chrome/.test(navigator.userAgent.toLowerCase());
      if (is_chrome) {
        height += 50;
      }

      var params = {
        width: width,
        height: height,
        top: top,
        left: left,
        directories: 'no',
        location: 'no',
        menubar: 'no',
        resizable: 'no',
        scrollbars: 'yes',
        status: 'no',
        toolbar: 'no'
      };

      var params_list = [];
      for (var key in params) {
        if (params.hasOwnProperty(key)) {
          params_list.push(key + "=" + params[key]);
        }
      }

      return window.open(url, windowname || "", params_list.join(","));
    },

    signin : function(success, provider, check_authorization_url, width, height) {
      var newwin = window.Sign.popup("command/core/authorize/" + provider, width, height);

      if (newwin !== null) {
        newwin.opener = window;
      }

      window.onauthorization = function() {
        if (typeof success == 'undefined') {
          window.location.reload();
        } else {
          $.ajax({
            url: check_authorization_url,
            dataType: "json",
            success: function(data) {
              window.user = data;
              if (typeof success == 'function') success();
            }
          });
        }
      };

      if (window.focus && newwin !== null) {
        newwin.focus();
      }

      return false;
    },

    signout : function(success,provider) {
      $.ajax({
        url: "command/core/deauthorize/" + provider,
        success: function() {
          if (typeof success == 'undefined') {
            window.location.reload();
          } else {
            if (typeof success == 'function') success();
          }
        }
      });
    }
  };
}
