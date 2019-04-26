/*

Copyright 2017, Owen Stephens
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
    * Neither the name of the copyright holder nor the names of its
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

function HttpHeadersDialog(title, headers, onDone) {
    this._onDone = onDone;
    var self = this;
    
    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    var html = $(HttpHeadersDialog.generateWidgetHtml()).appendTo(body);
    this._elmts = DOM.bind(html);
        
    this._httpHeadersWidget = new HttpHeadersDialog.Widget(
        this._elmts, 
        headers
    );
}

HttpHeadersDialog.generateWidgetHtml = function() {
    var html = DOM.loadHTML("core", "scripts/dialogs/http-headers-dialog.html");
    var httpheaderOptions = [];

    for (var headerLabel in theProject.httpHeaders) {
        if (theProject.httpHeaders.hasOwnProperty(headerLabel)) {
            var info = theProject.httpHeaders[headerLabel];
            httpheaderOptions.push('<label for="' +
                                    headerLabel +
                                    '">' +
                                    info.header +
                                    ': </label><input type="text" id="' +
                                    headerLabel +
                                    '" name="' +
                                    headerLabel +
                                    '" value="' +
                                    info.defaultValue +
                                    '" /></option><br />');
        }
    }

    return html.replace("$HTTP_HEADER_OPTIONS$", httpheaderOptions.join(""));
};

