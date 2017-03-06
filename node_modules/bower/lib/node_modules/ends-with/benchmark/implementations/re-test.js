'use strict';

module.exports = function(a, b) {
  a = String(a);
  b = String(b);
  var re = new RegExp(b + '$');
  return re.test(a);
};
