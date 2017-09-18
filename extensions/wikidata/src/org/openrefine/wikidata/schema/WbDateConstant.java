package org.openrefine.wikidata.schema;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.google.common.collect.ImmutableMap;


public class WbDateConstant extends WbDateExpr {
    public static final String jsonType = "wbdateconstant";
    
    public static Map<SimpleDateFormat,Integer> acceptedFormats = ImmutableMap.<SimpleDateFormat,Integer>builder()
        .put(new SimpleDateFormat("yyyy"), 9)
        .put(new SimpleDateFormat("yyyy-MM"), 10)
        .put(new SimpleDateFormat("yyyy-MM-dd"), 11)
        .put(new SimpleDateFormat("yyyy-MM-dd'T'HH"), 12)
        .put(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"), 13)
        .put(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"), 14)
        .build();
    
    private TimeValue _parsed;
    private String _origDatestamp;
    
    public WbDateConstant(String origDatestamp) {
        _origDatestamp = origDatestamp;
        try {
           _parsed = parse(origDatestamp);
        } catch(ParseException e) {
            _parsed = null;
        }
    }
    
    @Override
    public TimeValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        if (_parsed == null) {
            throw new SkipStatementException();
        }
        return _parsed;
    }
    
    public static TimeValue parse(String datestamp) throws ParseException {
        Date date = null;
        int precision = 9; // default precision (will be overridden)
        for(Entry<SimpleDateFormat,Integer> entry : acceptedFormats.entrySet()) {
            try {
                date = entry.getKey().parse(datestamp);
                precision = entry.getValue();
            } catch (ParseException e) {
                continue;
            }
        }
        if (date == null) {
            throw new ParseException("Invalid date.", 0);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar = Calendar.getInstance();
            calendar.setTime(date);
            return Datamodel.makeTimeValue(
                    calendar.get(Calendar.YEAR),
                    (byte) (calendar.get(Calendar.MONTH)+1), // java starts at 0
                    (byte) calendar.get(Calendar.DAY_OF_MONTH),
                    (byte) calendar.get(Calendar.HOUR_OF_DAY),
                    (byte) calendar.get(Calendar.MINUTE),
                    (byte) calendar.get(Calendar.SECOND),
                    (byte) precision,
                    1,
                    1,
                    calendar.getTimeZone().getRawOffset()/3600000,
                    TimeValue.CM_GREGORIAN_PRO);
        }     
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("value");
        writer.value(_origDatestamp);
    }
    
    public static WbDateConstant fromJSON(JSONObject obj) throws JSONException {
        return new WbDateConstant(obj.getString("value"));
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }

}
