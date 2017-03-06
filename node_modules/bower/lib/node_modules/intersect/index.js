module.exports = intersect;

function many (sets) {
  var o = {};
  var l = sets.length - 1;
  var first = sets[0];
  var last = sets[l];
  
  for(var i in first) o[first[i]] = 0;
  
  for(var i = 1; i <= l; i++) {
    var row = sets[i];
    for(var j in row) {
      var key = row[j];
      if(o[key] === i - 1) o[key] = i;
    }
  }
  
  var a = [];
  for(var i in last) {
    var key = last[i];
    if(o[key] === l) a.push(key);
  }
  
  return a;
}

function intersect (a, b) {
  if (!b) return many(a);

  var res = [];
  for (var i = 0; i < a.length; i++) {
    if (indexOf(b, a[i]) > -1) res.push(a[i]);
  }
  return res;
}

intersect.big = function(a, b) {
  if (!b) return many(a);
  
  var ret = [];
  var temp = {};
  
  for (var i = 0; i < b.length; i++) {
    temp[b[i]] = true;
  }
  for (var i = 0; i < a.length; i++) {
    if (temp[a[i]]) ret.push(a[i]);
  }
  
  return ret;
}

function indexOf(arr, el) {
  for (var i = 0; i < arr.length; i++) {
    if (arr[i] === el) return i;
  }
  return -1;
}
