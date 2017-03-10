define(['../array/indexOf', '../array/slice', '../array/take'], function (indexOf, slice, take) {

    var _ = {};

    /**
     * Creates a partially applied function.
     */
    function partial(f) {
        var as = slice(arguments, 1);
        var has_ = indexOf(as, _) !== -1;

        return function() {
            var rest = slice(arguments);

            // Don't waste time checking for placeholders if there aren't any.
            var args = has_ ? take(as.length, function(i) {
                var a = as[i];
                return a === _ ? rest.shift() : a;
            }) : as;

            return f.apply(this, rest.length ? args.concat(rest) : args);
        };
    }

    partial._ = _;

    return partial;

});
