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

Refine.GDataImportingController = function(createProjectUI) {
  this._createProjectUI = createProjectUI;

  createProjectUI.addSourceSelectionUI({
    label: "Google Data",
    id: "gdata-source",
    ui: new Refine.GDataSourceUI(this)
  });
};
Refine.CreateProjectUI.controllers.push(Refine.GDataImportingController);

Refine.GDataSourceUI = function(controller) {
  this._controller = controller;
  
  var self = this;
  window.addEventListener(
    "message",
    function(evt) {
      var url = document.location.href;
      var slash = url.indexOf('/', url.indexOf('//') + 2);
      var origin = url.substring(0, slash);
      if (origin == evt.origin) {
        var prefix = 'gdata:authsub_token=';
        if (evt.data.startsWith(prefix) && evt.data.length > prefix.length) {
          self._listDocuments();
        } else {
          this._body.find('.gdata-page').hide();
          this._elmts.signinPage.show();
        }
      }
    },
    false);
};

Refine.GDataSourceUI.prototype.attachUI = function(body) {
  this._body = body;
  
  this._body.html(DOM.loadHTML("gdata", "scripts/index/import-from-gdata-form.html"));
  this._elmts = DOM.bind(this._body);
  
  this._body.find('.gdata-page').hide();
  this._elmts.signinPage.show();
  
  this._body.find('.gdata-signin.button').click(function() {
    window.open(
      "/command/gdata/authorize",
      "google-refine-gdata-signin",
      "resizable=1,width=600,height=450"
    );
  });
};

Refine.GDataSourceUI.prototype.focus = function() {
};

Refine.GDataSourceUI.prototype._listDocuments = function() {
  this._body.find('.gdata-page').hide();
  this._elmts.progressPage.show();
  
  var self = this;
  $.post(
    "/command/core/importing-controller?" + $.param({
      "controller": "gdata/gdata-importing-controller",
      "subCommand": "list-documents"
    }),
    null,
    function(o) {
      self._renderDocuments(o);
    },
    "json"
  );
};

Refine.GDataSourceUI.prototype._renderDocuments = function(o) {
  this._elmts.listingContainer.empty();
  
  var table = $(
    '<table><tr>' +
      '<th></th>' + // starred
      '<th>Title</th>' +
      '<th>Authors</th>' +
      '<th>Last Edited</th>' +
      '<th>Last Viewed</th>' +
    '</tr></table>'
  ).appendTo(this._elmts.listingContainer)[0];
  
  var renderDocument = function(doc) {
    var tr = table.insertRow(table.rows.length);
    
    var td = tr.insertCell(tr.cells.length);
    if (doc.isStarred) {
      $('<img>').attr('src', 'images/star.png').appendTo(td);
    }

    td = tr.insertCell(tr.cells.length);
    var title = $('<a>')
    .addClass('gdata-doc-title')
    .attr('href', 'javascript:{}')
    .text(doc.title)
    .appendTo(td);
    
    $('<a>')
    .addClass('gdata-doc-preview')
    .attr('href', doc.docLink)
    .attr('target', '_blank')
    .text('preview')
    .appendTo(td);
    
    td = tr.insertCell(tr.cells.length);
    $('<span>')
    .text(doc.authors.join(', '))
    .appendTo(td);
    
    td = tr.insertCell(tr.cells.length);
    $('<span>')
    .addClass('gdata-doc-date')
    .text(formatRelativeDate(doc.edited))
    .attr('title', doc.edited)
    .appendTo(td);
    
    var alreadyViewed = false;
    
    td = tr.insertCell(tr.cells.length);
    if (doc.lastViewed) {
      if (new Date(doc.lastViewed).getTime() - new Date(doc.edited).getTime() > -60000) {
        alreadyViewed = true;
      }
      
      $('<span>')
      .addClass('gdata-doc-date')
      .text(formatRelativeDate(doc.lastViewed))
      .attr('title', doc.lastViewed)
      .appendTo(td);
    }
    
    if (!alreadyViewed) {
      title.addClass('gdata-doc-unread');
    }
  };
  for (var i = 0; i < o.documents.length; i++) {
    renderDocument(o.documents[i]);
  }
  
  this._body.find('.gdata-page').hide();
  this._elmts.listingPage.show();
};
