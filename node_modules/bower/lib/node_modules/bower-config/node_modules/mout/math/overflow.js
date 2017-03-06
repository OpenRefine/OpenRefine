

    /**
     * Wraps number within bounds both positive and negative
     */
    function overflow(number, min, max){
        if ( max === undefined ) {
            max = min;
            min = 0;
        }

        var difference = max - min;

        if ( number < min ) {
            number += difference * ( ~~( ( min - number ) / difference ) + 1 );
        }

        return min + ( number - min ) % difference;
    }

    module.exports = overflow;


