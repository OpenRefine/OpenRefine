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
  // The browser might not support all the locale identifiers that our i18n library supports.
  // For instance, firefox chokes on 'zh_Hant'.
  this._numberFormat = null;
  try {
    let supportedLocales = Intl.NumberFormat.supportedLocalesOf([ $.i18n().locale ]);
    if (supportedLocales.length > 0) {
      this._numberFormat = new Intl.NumberFormat(supportedLocales[0]);
    }
  } catch (error) {
    // leave no number formatter
  }
}

SummaryBar.prototype._initializeUI = function() {

};

SummaryBar.prototype.updateResultCount = function() {
  var summaryText;
  var locale = $.i18n().locale;
  var rowModel = theProject.rowModel;
  if (theProject.rowModel.filtered == theProject.rowModel.total) {
    summaryText = $.i18n(theProject.rowModel.mode == "row-based" ? 'core-views/total-rows' : 'core-views/total-records', this.formatNumber(rowModel.total));
  } else {
    summaryText = $.i18n(theProject.rowModel.mode == "row-based" ? 'core-views/total-matching-rows' : 'core-views/total-matching-records', this.formatNumber(rowModel.filtered), this.formatNumber(rowModel.total));
  }

  $('<span>').html(summaryText).appendTo(this._div.empty());
};

SummaryBar.prototype.formatNumber = function(number) {
  if (this._numberFormat == null) {
    return number.toString();
  } else {
    return this._numberFormat.format(number);
  }
}
