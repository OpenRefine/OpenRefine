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

var GdataExtension = {};

GdataExtension.isAuthorized = function() {
  // TODO: Unreliable - just means that we were authorized at one point
  return Cookies.get('oauth2_token') != null;
};

GdataExtension.showAuthorizationDialog = function(onAuthorized, onNotAuthorized) {
  if (window.name) {
    var windowName = window.name;
  } else {
    var windowName = "openrefine" + new Date().getTime();
    window.name = windowName;
  }
  
  var callbackName = "cb" + new Date().getTime();
  var callback = function(evt) {
    delete window[callbackName];
    if (GdataExtension.isAuthorized()) {
      onAuthorized();
    } else if (onNotAuthorized) {
      onNotAuthorized();
    }
    window.setTimeout(function() { win.close(); }, 100);
  };
  window[callbackName] = callback;
  
  var url = ModuleWirings['gdata'] + "authorize?winname=" + escape(windowName) + "&cb=" + escape(callbackName);
  var win = window.open(url, "openrefinegdataauth", "resizable=1,width=800,height=600");
};
