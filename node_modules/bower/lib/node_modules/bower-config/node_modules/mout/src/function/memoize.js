define(['../lang/isFunction', '../object/hasOwn'], function(isFunction, hasOwn){

    /**
     * Creates a function that memoizes the result of `fn`. If `resolver` is
     * provided it determines the cache key for storing the result based on the
     * arguments provided to the memoized function. By default, the first argument
     * provided to the memoized function is coerced to a string and used as the
     * cache key. The `fn` is invoked with the `this` binding of the memoized
     * function. Modified from lodash.
     *
     * @param {Function} fn Function to have its output memoized.
     * @param {Function} context Function to resolve the cache key.
     * @return {Function} Returns the new memoized function.
     */
    function memoize(fn, resolver) {
        if (!isFunction(fn) || (resolver && !isFunction(resolver))) {
            throw new TypeError('Expected a function');
        }

        var memoized = function() {
            var cache = memoized.cache,
                key = resolver ? resolver.apply(this, arguments) : arguments[0];

            if (hasOwn(cache, key)) {
                return cache[key];
            }
            var result = fn.apply(this, arguments);
            cache[key] = result;
            return result;
        };

        memoized.cache = {};

        return memoized;
    }

    return memoize;
});

