
package com.google.refine.expr.functions;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Properties;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class TimeSinceUnixEpochToDate implements Function {

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
                String unit = ((String) o2).toLowerCase();
                if (unit.equals("second")) {
                    instant = Instant.ofEpochSecond(epoch);
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                } else if (unit.equals("millisecond")) {
                    instant = Instant.ofEpochSecond(epoch / 1000);
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                } else if (unit.equals("microsecond")) {
                    instant = Instant.ofEpochSecond(epoch / 1000000);
                    date = OffsetDateTime.ofInstant(instant, zoneId);
                    return date;
                }
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this)
                + " accepts a number and an optional second argument containing a string specifying the units");
    }

    @Override
    public String getDescription() {
        return "Returns a number converted to a date based on Unix Epoch Time. The number can be Unix Epoch Time in one of the following supported units: second, millisecond, microsecond. Defaults to 'second'.";
    }

    @Override
    public String getParams() {
        return "number n, string unit (optional, defaults to 'seconds')";
    }

    @Override
    public String getReturns() {
        return "date(OffsetDateTime)";
    }

}
