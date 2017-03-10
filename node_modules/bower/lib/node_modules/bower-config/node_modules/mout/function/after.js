

    /**
     * Calls closure only after callback is called x times
     */
    function after(closure, times){
        return function () {
            if (--times <= 0) closure();
        };
    }

    module.exports = after;


