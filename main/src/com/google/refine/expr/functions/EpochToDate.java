
package com.google.refine.expr.functions;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Properties;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class EpochToDate implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1 && args[0] instanceof Number) {
            long epoch = (long) args[0];
            ZoneId zoneId = ZoneId.of("UTC");
            Instant instant = null;
            OffsetDateTime date = null;
            if (args.length == 1) {
                instant = Instant.ofEpochSecond(epoch);
                date = OffsetDateTime.ofInstant(instant, zoneId);
                return date;
            } else if (args.length == 2 && args[1] instanceof String) {
                Object o2 = args[1];
                String type = ((String) o2).toLowerCase();
                if (type.equals("second")) {
                    instant = Instant.ofEpochSecond(epoch);
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                } else if (type.equals("millisecond")) {
                    instant = Instant.ofEpochSecond(epoch / 1000);
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                } else if (type.equals("microsecond")) {
                    instant = Instant.ofEpochSecond(epoch / 1000000);
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expect one argument, " +
                "which is a number of epoch time\n" +
                "expect two argument, the first is a epoch time(second, millisecond, microsecond), the second " +
                "is the type");
    }

    @Override
    public String getDescription() {
        return "Returns a number converted to a date. Can parse one parameter or two parameters. When parsing one parameter, the number is the epoch second."
                +
                "When parsing two parameters, the first is the number, the second is the numbers type, such as second, millisecond, microsecond.";
    }

    @Override
    public String getParams() {
        return "A number of epoch second, millisecond, microsecond. The second parameter is not necessary, is the input number's type";
    }

    @Override
    public String getReturns() {
        return "date(OffsetDateTime)";
    }

}
