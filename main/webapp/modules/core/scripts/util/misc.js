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

function cloneDeep(o) {
  if (o === undefined || o === null) {
    return o;
  } else if (o instanceof Function) {
    return o;
  } else if (o instanceof Array) {
    var a = [];
    for (var i = 0; i < o.length; i++) {
      a.push(cloneDeep(o[i]));
    }
    return a;
  } else if (o instanceof Object) {
    var a = {};
    for (var n in o) {
      if (o.hasOwnProperty(n)) {
        a[n] = cloneDeep(o[n]);
      }
    }
    return a;
  } else {
    return o;
  }
}

function formatRelativeDate(d) {
  var d = new Date(d);
  var almost_last_year = Date.today().add({ months: -11 });
  var last_month = Date.today().add({ months: -1 });
  var last_week = Date.today().add({ days: -7 });
  var today = Date.today();
  var tomorrow = Date.today().add({ days: 1 });

  if (d.between(today, tomorrow)) {
    return $.i18n('core-util-enc/today', d.toString("h:mm tt"));
  } else if (d.between(last_week, today)) {
    var diff = Math.floor(daysIntoYear(today) - daysIntoYear(d));
    return (diff <= 1) ? ($.i18n('core-util-enc/yesterday', d.toString("h:mm tt"))) : $.i18n('core-util-enc/days-ago', diff);
  } else if (d.between(last_month, today)) {
    var diff = Math.floor((daysIntoYear(today) - daysIntoYear(d)) / 7);
    if (diff < 1) {diff += 52};
    return $.i18n('core-util-enc/weeks-ago', diff) ;
  } else if (d.between(almost_last_year, today)) {
    var diff = today.getMonth() - d.getMonth();
    if (diff < 1) {
      diff += 12;
    }
    return $.i18n('core-util-enc/months-ago', diff);
  } else {
    var diff = Math.floor(today.getYear() - d.getYear());
    return $.i18n('core-util-enc/years-ago', diff);
  }
}

function daysIntoYear(date){
  return (Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) - Date.UTC(date.getFullYear(), 0, 0)) / 24 / 60 / 60 / 1000;
}

function setInitialHeightTextArea(textarea) {
  const textareaStyle = getComputedStyle(textarea);
  const fontSizePx = textareaStyle.fontSize;
  const fontSize = Number(fontSizePx.replace(/px$/, ''));
  let initialHeight = Math.max(textarea.scrollHeight,Math.round(3*1.1*fontSize));
  initialHeight = Math.min(initialHeight,Math.round(15*1.1*fontSize));
  textarea.style.height = initialHeight+'px';
}
