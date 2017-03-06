var intersect = require('./');

var a = [];
var b = [];

for (var i = 0; i < 500; i++) {
  a[i] = i;
  b[i] = i + 200;
}

var start = Date.now();
for (var i = 0; i < 10000; i++) intersect(a, b);
console.log('%s ms', Date.now() - start);
