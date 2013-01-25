// Bind a method to an object and cache it
// via: http://webreflection.blogspot.be/2012/11/my-name-is-bound-method-bound.html
Object.defineProperty(Object.prototype, "bound", {
  value: function (methodName) {
    var boundName = "__bound__" + methodName;
    return this[boundName] || (this[boundName] = this[methodName].bind(this));
  },
});
