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