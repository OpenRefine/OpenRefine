'use strict';

module.exports = function(a, b) {
  a = String(a);
  b = String(b);
  return a.slice(a.length - b.length) === b;
};
