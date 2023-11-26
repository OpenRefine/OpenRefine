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

var URLUtil = {
  schemes: { // 1 means followed by ://, 0 means followed by just :
    "callto":0,
    "chrome":1,
    "file":1,
    "ftp":1,
    "geo":0,
    "http":1,
    "https":1,
    "imap":1,
    "info":0,
    "irc":1,
    "jar":0,
    "javascript":0,
    "lastfm":1,
    "ldap":1, 
    "ldaps":1,
    "mailto":0, 
    "news":0,
    "nntp":1,
    "pop":1,
    "sftp":1,
    "skype":0,
    "smb":1,
    "ssh":1,
    "svn":1,
    "svn+ssh":1,
    "telnet":1,
    "view-source":0
  }
};
(function() {
  var minLength = 100;
  var maxLength = 0;

  for (var n in URLUtil.schemes) {
    minLength = Math.min(minLength, n.length);
    maxLength = Math.max(maxLength, n.length);
  }

  URLUtil.minSchemeLength = minLength;
  URLUtil.maxSchemeLength = maxLength;
})();

URLUtil.getParameters = function() {
  var r = {};

  var params = window.location.search;
  if (params.length > 1) {
    params = params.substring(1).split("&");
    $.each(params, function() {
      pair = this.split("=");
      r[pair[0]] = decodeURIComponent(pair[1]);
    });
  }

  return r;
};

URLUtil.looksLikeUrl = function(s) {
  if (s.length > URLUtil.minSchemeLength + 1) {
    var sep = s.substring(0, URLUtil.maxSchemeLength + 3).indexOf(":");
    if (sep >= URLUtil.minSchemeLength) {
      var scheme = s.substring(0, sep).toLowerCase();
      if (scheme in URLUtil.schemes) {
        return (URLUtil.schemes[scheme] === 0) || (s.substring(sep + 1, sep + 3) == "//");
      }
    }
  }
  return false;
};

