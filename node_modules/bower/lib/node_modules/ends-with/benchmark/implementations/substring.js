'use strict'

module.exports = function(a, b) {
  a = String(a);
  b = String(b);
  return a.substring(a.length - b.length) === b;
};
