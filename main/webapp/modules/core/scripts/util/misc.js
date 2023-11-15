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

const rtf = new Intl.RelativeTimeFormat(navigator.languages, { numeric: 'auto' });

function formatRelativeDate(d) {
  const date = new Date(d);
  const now = new Date();
  const diffTime = now - date;

  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

  if (Math.abs(diffDays) < 1) {
    const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
    return rtf.format(-diffHours, 'hour');
  } else if (Math.abs(diffDays) < 7) {
    return rtf.format(-diffDays, 'day');
  } else if (Math.abs(diffDays) < 30) {
    const diffWeeks = Math.floor(diffDays / 7);
    return rtf.format(-diffWeeks, 'week');
  } else if (Math.abs(diffDays) < 365) {
    const diffMonths = Math.floor(diffDays / 30); // Approximation
    return rtf.format(-diffMonths, 'month');
  } else {
    const diffYears = diffDays / 365; // Approximation
    return rtf.format(-Math.floor(diffYears), 'year');
  }
}

function setInitialHeightTextArea(textarea) {
  const textareaStyle = getComputedStyle(textarea);
  const fontSizePx = textareaStyle.fontSize;
  const fontSize = Number(fontSizePx.replace(/px$/, ''));
  let initialHeight = Math.max(textarea.scrollHeight,Math.round(3*1.1*fontSize));
  initialHeight = Math.min(initialHeight,Math.round(15*1.1*fontSize));
  textarea.style.height = initialHeight+'px';
}
