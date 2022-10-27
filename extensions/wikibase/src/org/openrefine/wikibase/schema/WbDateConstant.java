/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.schema;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

/**
 * A constant for a time value, accepting a number of formats which determine the precision of the parsed value.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbDateConstant implements WbExpression<TimeValue> {

    /**
     * Map of formats accepted by the parser. Each format is associated to the time precision it induces (an integer
     * according to Wikibase's data model).
     */
    public static Map<SimpleDateFormat, Integer> acceptedFormats = ImmutableMap.<SimpleDateFormat, Integer> builder()
            .put(new SimpleDateFormat("yyyy'M'"), 6)
            .put(new SimpleDateFormat("yyyy'C'"), 7)
            .put(new SimpleDateFormat("yyyy'D'"), 8)
            .put(new SimpleDateFormat("yyyy"), 9)
            .put(new SimpleDateFormat("yyyy-MM"), 10)
            .put(new SimpleDateFormat("yyyy-MM-dd"), 11)
            .build();

    public static Pattern calendarSuffixPattern = Pattern.compile("_(Q[1-9][0-9]*)$");

    private TimeValue parsed;
    private final String origDatestamp;

    /**
     * Constructor. Used for deserialization from JSON. The object will be constructed even if the time cannot be parsed
     * (it will evaluate to null) in {@link evaluate}.
     * 
     * @param origDatestamp
     *            the date value as a string
     */
    @JsonCreator
    public WbDateConstant(@JsonProperty("value") String origDatestamp) {
        this.origDatestamp = origDatestamp;
        parsed = null;
    }

    @Override
    public void validate(ValidationState validation) {
        if (origDatestamp == null) {
            validation.addError("Empty date field");
        } else {
            try {
                this.parsed = parse(origDatestamp);
            } catch (ParseException e) {
                validation.addError(String.format("Invalid date provided: '%s'", origDatestamp));
            }
        }
    }

    @Override
    public TimeValue evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        return parsed;
    }

    /**
     * Parses a timestamp into a Wikibase {@link TimeValue}. The precision is automatically inferred from the format.
     * 
     * @param datestamp
     *            the time to parse
     * @return
     * @throws ParseException
     *             if the time cannot be parsed
     */
    public static TimeValue parse(String datestamp)
            throws ParseException {
        Date bestDate = null;
        int precision = 0; // default precision (will be overridden if successfully parsed)
        int maxLength = 0; // the maximum length parsed
        boolean bceFlag = false; // judge whether this is a BCE year
        String calendarIri = TimeValue.CM_GREGORIAN_PRO; // Gregorian calendar is assumed by default

        String trimmedDatestamp = datestamp.trim();

        if ("TODAY".equals(trimmedDatestamp)) {
            Calendar calendar = Calendar.getInstance();
            TimeValue todaysDate = Datamodel.makeTimeValue(
                    calendar.get(Calendar.YEAR),
                    (byte) (calendar.get(Calendar.MONTH) + 1),
                    (byte) calendar.get(Calendar.DAY_OF_MONTH),
                    (byte) 0, (byte) 0, (byte) 0, (byte) 11, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO);
            return todaysDate;
        }

        if (trimmedDatestamp.startsWith("-")) {
            trimmedDatestamp = trimmedDatestamp.substring(1);
            bceFlag = true;
        }

        for (Entry<SimpleDateFormat, Integer> entry : acceptedFormats.entrySet()) {
            ParsePosition position = new ParsePosition(0);
            Date date = entry.getKey().parse(trimmedDatestamp, position);

            if (date == null) {
                continue;
            }

            // Potentially parse the calendar Qid after the date
            int consumedUntil = position.getIndex();
            if (consumedUntil < trimmedDatestamp.length()) {
                Matcher matcher = calendarSuffixPattern.matcher(
                        trimmedDatestamp.subSequence(position.getIndex(), trimmedDatestamp.length()));
                if (matcher.find()) {
                    String calendarQid = matcher.group(1);
                    calendarIri = Datamodel.SITE_WIKIDATA + calendarQid;
                    consumedUntil = trimmedDatestamp.length();
                }
            }

            // Ignore parses which failed or do not consume all the input
            if (date != null && position.getIndex() > maxLength
            // only allow to partially consume the input if the precision is day and followed by a T (as in ISO)
                    && (consumedUntil == trimmedDatestamp.length()
                            || (entry.getValue() == 11 && trimmedDatestamp.charAt(consumedUntil) == 'T'))) {
                precision = entry.getValue();
                bestDate = date;
                maxLength = position.getIndex();
            }
        }
        if (bestDate == null || precision == 0) {
            throw new ParseException("Invalid date.", 0);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar = Calendar.getInstance();
            calendar.setTime(bestDate);
            long year = calendar.get(Calendar.YEAR);
            int month = precision < 10 ? 0 : calendar.get(Calendar.MONTH) + 1;
            int day_of_month = precision < 11 ? 0 : calendar.get(Calendar.DAY_OF_MONTH);
            if (bceFlag)
                year = -1 * year;
            return Datamodel.makeTimeValue(year, (byte) month,
                    (byte) day_of_month, (byte) calendar.get(Calendar.HOUR_OF_DAY),
                    (byte) calendar.get(Calendar.MINUTE), (byte) calendar.get(Calendar.SECOND), (byte) precision, 0, 0,
                    0, calendarIri);
        }
    }

    /**
     * @return the original datestamp
     */
    @JsonProperty("value")
    public String getOrigDatestamp() {
        return origDatestamp;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbDateConstant.class.isInstance(other)) {
            return false;
        }
        WbDateConstant otherConstant = (WbDateConstant) other;
        return origDatestamp.equals(otherConstant.getOrigDatestamp());
    }

    @Override
    public int hashCode() {
        return origDatestamp.hashCode();
    }

}
