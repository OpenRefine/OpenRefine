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
  var engineStats = theProject.engineStats || {};
  var mode = engineStats['engine-mode'];
  var locale = $.i18n().locale;
  if (mode === undefined) {
    summaryText = ''; // TODO we could have a loading indicator?
  } else if (engineStats.filteredCount == engineStats.totalCount || engineStats.facets.length === 0 || engineStats.neutral) {
    summaryText = $.i18n(mode == "row-based" ? 'core-views/total-rows' : 'core-views/total-records', engineStats.totalCount.toLocaleString(locale));
  } else if (engineStats.aggregatedCount == engineStats.totalRows) {
    summaryText = $.i18n(mode == "row-based" ? 'core-views/total-matching-rows' : 'core-views/total-matching-records', engineStats.filteredCount.toLocaleString(locale), engineStats.totalCount.toLocaleString(locale));
  } else {
    var percentage = 100;
    if (engineStats.aggregatedCount > 0) {
        percentage = Math.round(1000 * engineStats.filteredCount / engineStats.aggregatedCount) / 10;
    }
    summaryText = $.i18n(mode == "row-based" ? 'core-views/approx-matching-rows' : 'core-views/approx-matching-records', percentage.toLocaleString(locale), engineStats.totalCount.toLocaleString(locale));
  }

  $('<span>').html(summaryText).appendTo(this._div.empty());
};
