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

var DOM = {};

DOM.bind = function(elmt) {
    var map = {};
    
    for (var i = 0; i < elmt.length; i++) {
        DOM._bindDOMElement(elmt[i], map);
    }
    
    return map;
};

DOM._bindDOMElement = function(elmt, map) {
    var bind = elmt.getAttribute("bind");
    if (bind !== null && bind.length > 0) {
        map[bind] = $(elmt);
    }
    
    if (elmt.hasChildNodes()) {
        DOM._bindDOMChildren(elmt, map);
    }
};

DOM._bindDOMChildren = function(elmt, map) {
    var node = elmt.firstChild;
    while (node !== null) {
        var node2 = node.nextSibling;
        if (node.nodeType == 1) {
            DOM._bindDOMElement(node, map);
        }
        node = node2;
    }
};

DOM._loadedHTML = {};
DOM.loadHTML = function(module, path) {
    var fullPath = ModuleWirings[module] + path;
    if (!(fullPath in DOM._loadedHTML)) {
        $.ajax({
            async: false,
            url: fullPath,
            dataType: "html",
            success: function(html) {
                DOM._loadedHTML[fullPath] = html;
            }
        })
    }
    return DOM._loadedHTML[fullPath];
};

DOM.getHPaddings = function(elmt) {
    return elmt.outerWidth() - elmt.width();
};

DOM.getVPaddings = function(elmt) {
    return elmt.outerHeight() - elmt.height();
};
