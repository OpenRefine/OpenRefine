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
function ExporterManager(button) {
  this._button = button;
  this._initializeUI();
}

ExporterManager.handlers = {};


ExporterManager.MenuItems = [
  {
    "id": "core/export-local",
    "label": $.i18n('core-dialogs/export-to-local'),
    "click": function () { ExporterManager.handlers.exportProjectToLocal(); }
  },
  {},
  {
    "id" : "core/export-tsv",
    "label": $.i18n('core-project/tab-value'),
    "click": function() { ExporterManager.handlers.exportRows("tsv", "tsv"); }
  },
  {
    "id" : "core/export-csv",
    "label": $.i18n('core-project/comma-sep'),
    "click": function() { ExporterManager.handlers.exportRows("csv", "csv"); }
  },
  {
    "id" : "core/export-html-table",
    "label": $.i18n('core-project/html-table'),
    "click": function() { ExporterManager.handlers.exportRows("html", "html"); }
  },
  {
    "id" : "core/export-excel",
    "label": $.i18n('core-project/excel'),
    "click": function() { ExporterManager.handlers.exportRows("xls", "xls"); }
  },
  {
    "id" : "core/export-excel-xml",
    "label": $.i18n('core-project/excel-xml'),
    "click": function() { ExporterManager.handlers.exportRows("xlsx", "xlsx"); }
  },
  {
    "id" : "core/export-ods",
    "label": $.i18n('core-project/odf'),
    "click": function() { ExporterManager.handlers.exportRows("ods", "ods"); }
  },
  {},
  {
    "id" : "core/export-custom-tabular",
    "label": $.i18n('core-project/custom-tabular'),
    "click": function() { new CustomTabularExporterDialog(); }
  },
  {
    "id" : "core/export-sql",
    "label": $.i18n('core-project/sql-export'),
    "click": function() { new SqlExporterDialog(); }
  },
  {
    "id" : "core/export-templating",
    "label": $.i18n('core-project/templating'),
    "click": function() { new TemplatingExporterDialog(); }
  }
];

ExporterManager.prototype._initializeUI = function() {
  this._button.on('click',function(evt) {
    MenuSystem.createAndShowStandardMenu(
        ExporterManager.MenuItems,
        this,
        { horizontal: false }
    );

    evt.preventDefault();
    return false;
  });
};

ExporterManager.stripNonFileChars = function(name) {
    // prohibited characters in file name of linux (/) and windows (\/:*?"<>|)
    // and MacOS https://stackoverflow.com/a/47455094/167425
    return jQueryTrim(name.replace(/[\\*\/:;,?"<>|#]/g, ' ')).replace(/\s+/g, '-');
};

ExporterManager.handlers.exportRows = function(format, ext) {
  let form = ExporterManager.prepareExportRowsForm(format, true, ext);
  document.body.appendChild(form);
  form.submit();
  document.body.removeChild(form);
};

ExporterManager.prepareExportRowsForm = function(format, includeEngine, ext) {
  let name = encodeURI(ExporterManager.stripNonFileChars(theProject.metadata.name));
  let form = document.createElement("form");
  $(form)
  .css("display", "none")
  .attr("method", "post")
  .attr("action", "command/core/export-rows/" + name + ((ext) ? ("." + ext) : ""));

  $('<input />')
  .attr("name", "project")
  .val(theProject.id)
  .appendTo(form);
  $('<input />')
  .attr("name", "format")
  .val(format)
  .appendTo(form);
  $('<input />')
  .attr("name", "quoteAll")
  .appendTo(form);
  if (includeEngine) {
    $('<input />')
    .attr("name", "engine")
    .val(JSON.stringify(ui.browsingEngine.getJSON()))
    .appendTo(form);
  }
  return form;
};

ExporterManager.handlers.exportProjectToLocal = function() {
  let name = encodeURI(ExporterManager.stripNonFileChars(theProject.metadata.name));
  let form = document.createElement("form");
  $(form)
  .css("display", "none")
  .attr("method", "post")
  .attr("action", "command/core/export-project/" + name + ".openrefine.tar.gz");
  $('<input />')
  .attr("name", "project")
  .val(theProject.id)
  .appendTo(form);

  document.body.appendChild(form);
  form.submit();
  document.body.removeChild(form);
};
