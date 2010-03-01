package com.metaweb.gridworks.expr.functions;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.util.CalendarParser;
import com.metaweb.gridworks.expr.util.CalendarParserException;
import com.metaweb.gridworks.gel.Function;

public class ToDate implements Function {

	public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 || args.length == 2) {
            Object o1 = args[0];
            if (o1 != null && o1 instanceof String) {
                boolean month_first = true;
                if (args.length == 2) {
                    Object o2 = args[1];
                    if (o2 != null && o2 instanceof Boolean) {
                        month_first = ((Boolean) o2).booleanValue();
                    }
                }
                try {
                    return CalendarParser.parse((String) o1, (month_first) ? CalendarParser.MM_DD_YY : CalendarParser.DD_MM_YY);
                } catch (CalendarParserException e) {
                    // do something about 
                }
            }
		}
		return null;
	}

	
	public void write(JSONWriter writer, Properties options)
		throws JSONException {
	
		writer.object();
		writer.key("description"); writer.value("Returns o converted to a date object");
		writer.key("params"); writer.value("o, boolean month_first (optional)");
		writer.key("returns"); writer.value("date");
		writer.endObject();
	}
}
