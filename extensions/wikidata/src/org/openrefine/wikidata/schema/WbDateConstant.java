package org.openrefine.wikidata.schema;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.common.collect.ImmutableMap;


public class WbDateConstant extends WbDateExpr {

    public static Map<SimpleDateFormat,Integer> acceptedFormats = ImmutableMap.<SimpleDateFormat,Integer>builder()
        .put(new SimpleDateFormat("yyyy"), 9)
        .put(new SimpleDateFormat("yyyy-MM"), 10)
        .put(new SimpleDateFormat("yyyy-MM-dd"), 11)
        .put(new SimpleDateFormat("yyyy-MM-dd'T'HH"), 12)
        .put(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"), 13)
        .put(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"), 14)
        .build();
    
    private TimeValue parsed;
    private String origDatestamp;
    
    @JsonCreator
    public WbDateConstant(
            @JsonProperty("value") String origDatestamp) {
        this.setOrigDatestamp(origDatestamp);
    }
    
    @Override
    public TimeValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        if (parsed == null) {
            throw new SkipStatementException();
        }
        return parsed;
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
    
    @JsonProperty("value")
    public String getOrigDatestamp() {
        return origDatestamp;
    }

    private void setOrigDatestamp(String origDatestamp) {
        this.origDatestamp = origDatestamp;
        try {
           this.parsed = parse(origDatestamp);
        } catch(ParseException e) {
            this.parsed = null;
        }
    }
    

}
