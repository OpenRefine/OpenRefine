String.prototype.trim = function() {
    return this.replace(/^\s+/, '').replace(/\s+$/, '');
};

String.prototype.startsWith = function(s) {
    return this.length >= s.length && this.substring(0, s.length) == s;
};

String.prototype.endsWith = function(s) {
    return this.length >= s.length && this.substring(this.length - s.length) == s;
};

String.prototype.contains = function(s) {
    return this.indexOf(s) >= 0;
};