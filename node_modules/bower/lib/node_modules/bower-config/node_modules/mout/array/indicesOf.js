

    /**
     * Array indicesOf
     */
    function indicesOf(arr, item, fromIndex) {
        var results = [];
        if (arr == null) {
            return results;
        }

        fromIndex = typeof fromIndex === 'number' ? fromIndex : 0;

        var length = arr.length;
        var cursor = fromIndex >= 0 ? fromIndex : length + fromIndex;

        while (cursor < length) {
            if (arr[cursor] === item) {
                results.push(cursor);
            }
            cursor++;
        }

        return results;
    }

    module.exports = indicesOf;

