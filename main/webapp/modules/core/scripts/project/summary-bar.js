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

function SummaryBar(div) {
  this._div = div;
  this._initializeUI();
}

SummaryBar.prototype._initializeUI = function() {

};

SummaryBar.prototype.updateResultCount = function() {
  var summaryText;
  var units = theProject.rowModel.mode == "row-based" ? $.i18n('core-views/rows') : $.i18n('core-views/records');
  var rowModel = theProject.rowModel;
  if (rowModel.filtered == rowModel.total) {
    summaryText = $.i18n(theProject.rowModel.mode == "row-based" ? 'core-views/total-rows' : 'core-views/total-records', rowModel.total);
  } else if (rowModel.processed == rowModel.total) {
    summaryText = $.i18n(theProject.rowModel.mode == "row-based" ? 'core-views/total-matching-rows' : 'core-views/total-matching-records', rowModel.filtered, rowModel.total);
  } else {
    var percentage = 100;
    if (rowModel.processed > 0) {
        percentage = Math.round(1000 * rowModel.filtered / rowModel.processed) / 10;
    }
    summaryText = $.i18n(theProject.rowModel.mode == "row-based" ? 'core-views/approx-matching-rows' : 'core-views/approx-matching-records', percentage, rowModel.total);
  }

  $('<span>').html(summaryText).appendTo(this._div.empty());
};
