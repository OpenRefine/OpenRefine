/*!
 * ends-with <https://github.com/jonschlinkert/ends-with>
 *
 * Copyright (c) 2014 Jon Schlinkert, contributors.
 * Licensed under the MIT license.
 */

'use strict';

module.exports = function (a, b) {
  if (Array.isArray(a)) {
    return a[a.length - 1] === b;
  }

  a = String(a);
  b = String(b);

  var i = b.length;
  var len = a.length - i;

  while (i--) {
    if (b.charAt(i) !== a.charAt(len + i)) {
      return false;
    }
  }
  return true;
};