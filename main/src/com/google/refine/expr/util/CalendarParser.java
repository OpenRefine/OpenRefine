/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.expr.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.refine.util.ParsingUtilities;

// Taken from http://icecube.wisc.edu/~dglo/software/calparse/index.html
// Copyright Dave Glowacki. Released under the BSD license.

/**
 * Date parser state.
 */
class ParserState {

    /** bit indicating that the year comes before the month. */
    static final int YEAR_BEFORE_MONTH = 0x4;
    /** bit indicating that the year comes before the day. */
    static final int YEAR_BEFORE_DAY = 0x2;
    /** bit indicating that the month comes before the day. */
    static final int MONTH_BEFORE_DAY = 0x1;

    /** bit indicating that the year comes after the month. */
    static final int YEAR_AFTER_MONTH = 0x0;
    /** bit indicating that the year comes after the day. */
    static final int YEAR_AFTER_DAY = 0x0;
    /** bit indicating that the month comes after the day. */
    static final int MONTH_AFTER_DAY = 0x0;

    /** value indicating an unset variable. */
    static final int UNSET = Integer.MIN_VALUE;

    /** <code>true</code> if year should appear before month. */
    private boolean yearBeforeMonth;
    /** <code>true</code> if year should appear before day. */
    private boolean yearBeforeDay;
    /** <code>true</code> if month should appear before day. */
    private boolean monthBeforeDay;

    /** year. */
    private int year;
    /** month (0-11). */
    private int month;
    /** day of month. */
    private int day;
    /** hour (0-23). */
    private int hour;
    /** minute (0-59). */
    private int minute;
    /** second (0-59). */
    private int second;
    /** millisecond (0-999). */
    private int milli;

    /** <code>true</code> if time is after noon. */
    private boolean timePostMeridian;

    /** time zone (use default time zone if this is <code>null</code>). */
    private TimeZone timeZone;

    /**
     * Create parser state for the specified order.
     * 
     * @param order
     *            <code>YY_MM_DD</code>, <code>MM_DD_YY</code>, etc.
     */
    ParserState(int order) {
        yearBeforeMonth = (order & YEAR_BEFORE_MONTH) == YEAR_BEFORE_MONTH;
        yearBeforeDay = (order & YEAR_BEFORE_DAY) == YEAR_BEFORE_DAY;
        monthBeforeDay = (order & MONTH_BEFORE_DAY) == MONTH_BEFORE_DAY;

        year = UNSET;
        month = UNSET;
        day = UNSET;
        hour = UNSET;
        minute = UNSET;
        second = UNSET;
        timePostMeridian = false;
    }

    /**
     * Get day of month.
     * 
     * @return day of month
     */
    int getDate() {
        return day;
    }

    /**
     * Get hour.
     * 
     * @return hour
     */
    int getHour() {
        return hour;
    }

    /**
     * Get millisecond.
     * 
     * @return millisecond
     */
    int getMillisecond() {
        return milli;
    }

    /**
     * Get minute.
     * 
     * @return minute
     */
    int getMinute() {
        return minute;
    }

    /**
     * Get month.
     * 
     * @return month
     */
    int getMonth() {
        return month;
    }

    /**
     * Get second.
     * 
     * @return second
     */
    int getSecond() {
        return second;
    }

    /**
     * Get time zone.
     * 
     * @return time zone (<code>null</code> if none was specified)
     */
    TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Get year.
     * 
     * @return year
     */
    int getYear() {
        return year;
    }

    /**
     * Is day of month value set?
     * 
     * @return <code>true</code> if a value has been assigned
     */
    boolean isDateSet() {
        return (day != UNSET);
    }

    /**
     * Is hour value set?
     * 
     * @return <code>true</code> if a value has been assigned
     */
    boolean isHourSet() {
        return (hour != UNSET);
    }

    /**
     * Is millisecond value set?
     * 
     * @return <code>true</code> if a value has been assigned
     */
    boolean isMillisecondSet() {
        return (milli != UNSET);
    }

    /**
     * Is minute value set?
     * 
     * @return <code>true</code> if a value has been assigned
     */
    boolean isMinuteSet() {
        return (minute != UNSET);
    }

    /**
     * Is a numeric month placed before a numeric day of month?
     * 
     * @return <code>true</code> if month is before day of month
     */
    boolean isMonthBeforeDay() {
        return monthBeforeDay;
    }

    /**
     * Is month value set?
     * 
     * @return <code>true</code> if a value has been assigned
     */
    boolean isMonthSet() {
        return (month != UNSET);
    }

    /**
     * Is second value set?
     * 
     * @return <code>true</code> if a value has been assigned
     */
    boolean isSecondSet() {
        return (second != UNSET);
    }

    /**
     * Is the time post-meridian (i.e. afternoon)?
     * 
     * @return <code>true</code> if time is P.M.
     */
    boolean isTimePostMeridian() {
        return (timePostMeridian || hour > 12);
    }

    /**
     * Is a numeric year placed before a numeric day of month?
     * 
     * @return <code>true</code> if year is before day of month
     */
    boolean isYearBeforeDay() {
        return yearBeforeDay;
    }

    /**
     * Is a numeric year placed before a numeric month?
     * 
     * @return <code>true</code> if year is before month
     */
    boolean isYearBeforeMonth() {
        return yearBeforeMonth;
    }

    /**
     * Is year value set?
     * 
     * @return <code>true</code> if a value has been assigned
     */
    boolean isYearSet() {
        return (year != UNSET);
    }

    /**
     * Fill the calendar with the parsed date.
     * 
     * @param cal
     *            calendar to fill
     * @param ignoreChanges
     *            if <code>true</code>, throw an exception when a date like <code>Sept 31</code> is changed to
     *            <code>Oct 1</code>
     * 
     * @throws CalendarParserException
     *             if the date cannot be set for some reason
     */
    void setCalendar(GregorianCalendar cal, boolean ignoreChanges)
            throws CalendarParserException {
        cal.clear();
        if (year != UNSET && month != UNSET && day != UNSET) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DATE, day);

            if (!ignoreChanges) {
                final int calYear = cal.get(Calendar.YEAR);
                final int calMonth = cal.get(Calendar.MONTH);
                final int calDay = cal.get(Calendar.DATE);

                if (calYear != year || (calMonth + 1) != month || calDay != day) {
                    throw new CalendarParserException("Date was set to "
                            + calYear + "/" + (calMonth + 1) + "/" + calDay
                            + " not requested " + year + "/" + month + "/"
                            + day);
                }
            }
        }

        cal.clear(Calendar.HOUR);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        if (hour != UNSET && minute != UNSET) {
            cal.set(Calendar.HOUR, hour);
            cal.set(Calendar.MINUTE, minute);
            if (second != UNSET) {
                cal.set(Calendar.SECOND, second);
                if (milli != UNSET) {
                    cal.set(Calendar.MILLISECOND, milli);
                }
            }

            if (timeZone != null) {
                cal.setTimeZone(timeZone);
            }
        }
    }

    /**
     * Set the day of month value.
     * 
     * @param val
     *            day of month value
     * 
     * @throws CalendarParserException
     *             if the value is not a valid day of month
     */
    void setDate(int val) throws CalendarParserException {
        if (val < 1 || val > 31) {
            throw new CalendarParserException("Bad day " + val);
        }

        day = val;
    }

    /**
     * Set the hour value.
     * 
     * @param val
     *            hour value
     * 
     * @throws CalendarParserException
     *             if the value is not a valid hour
     */
    void setHour(int val) throws CalendarParserException {
        final int tmpHour;
        if (timePostMeridian) {
            tmpHour = val + 12;
            timePostMeridian = false;
        } else {
            tmpHour = val;
        }

        if (tmpHour < 0 || tmpHour > 23) {
            throw new CalendarParserException("Bad hour " + val);
        }

        hour = tmpHour;
    }

    /**
     * Set the millisecond value.
     * 
     * @param val
     *            millisecond value
     * 
     * @throws CalendarParserException
     *             if the value is not a valid millisecond
     */
    void setMillisecond(int val) throws CalendarParserException {
        if (val < 0 || val > 999) {
            throw new CalendarParserException("Bad millisecond " + val);
        }

        milli = val;
    }

    /**
     * Set the minute value.
     * 
     * @param val
     *            minute value
     * 
     * @throws CalendarParserException
     *             if the value is not a valid minute
     */
    void setMinute(int val) throws CalendarParserException {
        if (val < 0 || val > 59) {
            throw new CalendarParserException("Bad minute " + val);
        }

        minute = val;
    }

    /**
     * Set the month value.
     * 
     * @param val
     *            month value
     * 
     * @throws CalendarParserException
     *             if the value is not a valid month
     */
    void setMonth(int val) throws CalendarParserException {
        if (val < 1 || val > 12) {
            throw new CalendarParserException("Bad month " + val);
        }

        month = val;
    }

    /**
     * Set the second value.
     * 
     * @param val
     *            second value
     * 
     * @throws CalendarParserException
     *             if the value is not a valid second
     */
    void setSecond(int val) throws CalendarParserException {
        if (val < 0 || val > 59) {
            throw new CalendarParserException("Bad second " + val);
        }

        second = val;
    }

    /**
     * Set the AM/PM indicator value.
     * 
     * @param val
     *            <code>true</code> if time represented is after noon
     */
    void setTimePostMeridian(boolean val) {
        timePostMeridian = val;
    }

    /**
     * Set the time zone.
     * 
     * @param tz
     *            time zone
     */
    void setTimeZone(TimeZone tz) {
        timeZone = tz;
    }

    /**
     * Set the year value.
     * 
     * @param val
     *            year value
     * 
     * @throws CalendarParserException
     *             if the value is not a valid year
     */
    void setYear(int val) throws CalendarParserException {
        if (val < 0) {
            throw new CalendarParserException("Bad year " + val);
        }

        year = val;
    }
}

/**
 * A parser for arbitrary date/time strings.
 */
public class CalendarParser {

    /** bit indicating that the year comes before the month. */
    public static final int YEAR_BEFORE_MONTH = ParserState.YEAR_BEFORE_MONTH;
    /** bit indicating that the year comes before the day. */
    public static final int YEAR_BEFORE_DAY = ParserState.YEAR_BEFORE_DAY;
    /** bit indicating that the month comes before the day. */
    public static final int MONTH_BEFORE_DAY = ParserState.MONTH_BEFORE_DAY;

    /** bit indicating that the year comes after the month. */
    public static final int YEAR_AFTER_MONTH = ParserState.YEAR_AFTER_MONTH;
    /** bit indicating that the year comes after the day. */
    public static final int YEAR_AFTER_DAY = ParserState.YEAR_AFTER_DAY;
    /** bit indicating that the month comes after the day. */
    public static final int MONTH_AFTER_DAY = ParserState.MONTH_AFTER_DAY;

    /** day/month/year order. */
    public static final int DD_MM_YY = YEAR_AFTER_MONTH | YEAR_AFTER_DAY
            | MONTH_AFTER_DAY;
    /** month/day/year order. */
    public static final int MM_DD_YY = YEAR_AFTER_MONTH | YEAR_AFTER_DAY
            | MONTH_BEFORE_DAY;
    /** month/year/day order. */
    public static final int MM_YY_DD = YEAR_AFTER_MONTH | YEAR_BEFORE_DAY
            | MONTH_BEFORE_DAY;
    /** day/year/month order. */
    public static final int DD_YY_MM = YEAR_BEFORE_MONTH | YEAR_AFTER_DAY
            | MONTH_AFTER_DAY;
    /** year/day/month order. */
    public static final int YY_DD_MM = YEAR_BEFORE_MONTH | YEAR_BEFORE_DAY
            | MONTH_AFTER_DAY;
    /** year/month/day order. */
    public static final int YY_MM_DD = YEAR_BEFORE_MONTH | YEAR_BEFORE_DAY
            | MONTH_BEFORE_DAY;

    /** list of time zone names. */
    private static final String[] zoneNames = loadTimeZoneNames();

    /** Unknown place in time parsing. */
    private static final int PLACE_UNKNOWN = 0;
    /** Parsing hour value from time string. */
    private static final int PLACE_HOUR = 1;
    /** Parsing minute value from time string. */
    private static final int PLACE_MINUTE = 2;
    /** Parsing second value from time string. */
    private static final int PLACE_SECOND = 3;
    /** Parsing millisecond value from time string. */
    private static final int PLACE_MILLI = 4;

    /** Adjustment for two-digit years will break in 2050. */
    private static final int CENTURY_OFFSET = 2000;

    /** value indicating an unset variable. */
    private static final int UNSET = ParserState.UNSET;

    /** set to <code>true</code> to enable debugging. */
    private static final boolean DEBUG = false;

    /** list of weekday names. */
    private static final String[] WEEKDAY_NAMES = { "sunday", "monday",
            "tuesday", "wednesday", "thursday", "friday", "saturday", };

    /** list of month abbreviations and names. */
    private static final String[][] MONTHS = { { "jan", "January" },
            { "feb", "February" }, { "mar", "March" }, { "apr", "April" },
            { "may", "May" }, { "jun", "June" }, { "jul", "July" },
            { "aug", "August" }, { "sep", "September" }, { "oct", "October" },
            { "nov", "November" }, { "dec", "December" }, };

    /**
     * Append formatted time string to the string buffer.
     * 
     * @param buf
     *            string buffer
     * @param cal
     *            object containing time
     * @param needSpace
     *            <code>true</code> if a space character should be inserted before any data
     */
    private static final void appendTimeString(StringBuffer buf, Calendar cal, boolean needSpace) {
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        final int minute = cal.get(Calendar.MINUTE);
        final int second = cal.get(Calendar.SECOND);
        final int milli = cal.get(Calendar.MILLISECOND);

        if (hour != 0 || minute != 0 || second != 0 || milli != 0) {
            if (needSpace) {
                buf.append(' ');
            }
            if (hour < 10) {
                buf.append(' ');
            }
            buf.append(hour);

            if (minute < 10) {
                buf.append(":0");
            } else {
                buf.append(':');
            }
            buf.append(minute);

            if (second != 0 || milli != 0) {
                if (second < 10) {
                    buf.append(":0");
                } else {
                    buf.append(':');
                }
                buf.append(second);

                if (milli != 0) {
                    if (milli < 10) {
                        buf.append(".00");
                    } else if (milli < 100) {
                        buf.append(".0");
                    } else {
                        buf.append('.');
                    }
                    buf.append(milli);
                }
            }
        }

        TimeZone tz = cal.getTimeZone();
        if (tz.getRawOffset() == 0) {
            buf.append(" GMT");
        } else {
            buf.append(' ');

            int offset = tz.getRawOffset() / (60 * 1000);
            if (offset < 0) {
                buf.append('-');
                offset = -offset;
            } else {
                buf.append('+');
            }

            int hrOff = offset / 60;
            if (hrOff < 10) {
                buf.append('0');
            }
            buf.append(hrOff);
            buf.append(':');

            int minOff = offset % 60;
            if (minOff < 10) {
                buf.append('0');
            }
            buf.append(minOff);
        }
    }

    /**
     * Return a string representation of the order value.
     * 
     * @param order
     *            order
     * 
     * @return order string
     */
    public static final String getOrderString(int order) {
        switch (order) {
            case DD_MM_YY:
                return "DD_MM_YY";
            case MM_DD_YY:
                return "MM_DD_YY";
            case MM_YY_DD:
                return "MM_YY_DD";
            case DD_YY_MM:
                return "DD_YY_MM";
            case YY_DD_MM:
                return "YY_DD_MM";
            case YY_MM_DD:
                return "YY_MM_DD";
            default:
                break;
        }

        return "??" + order + "??";
    }

    /**
     * Translate a string representation of an ordinal number to the appropriate numeric value.<br>
     * For example, <code>"1st"</code> would return <code>1</code>, <code>"23rd"</code> would return <code>23</code>,
     * etc.
     * 
     * @param str
     *            ordinal string
     * 
     * @return the numeric value of the ordinal number, or <code>CalendarParser.UNSET</code> if the supplied string is
     *         not a valid ordinal number.
     */
    private static final int getOrdinalNumber(String str) {
        final int len = (str == null ? 0 : str.length());
        if (len >= 3) {

            String suffix = str.substring(len - 2);
            if (suffix.equalsIgnoreCase("st") || suffix.equalsIgnoreCase("nd")
                    || suffix.equalsIgnoreCase("rd")
                    || suffix.equalsIgnoreCase("th")) {
                try {
                    return Integer.parseInt(str.substring(0, len - 2));
                } catch (NumberFormatException nfe) {
                    // fall through if number was not parsed
                }
            }
        }

        return UNSET;
    }

    /**
     * Get name of current place in time.
     * 
     * @param place
     *            place ID
     * 
     * @return place name (<code>"hour"</code>, <code>"minute"</code>, etc.
     */
    private static final String getTimePlaceString(int place) {
        switch (place) {
            case PLACE_HOUR:
                return "hour";
            case PLACE_MINUTE:
                return "minute";
            case PLACE_SECOND:
                return "second";
            case PLACE_MILLI:
                return "millisecond";
            default:
                break;
        }

        return "unknown";
    }

    /**
     * Determine is the supplied string is a value weekday name.
     * 
     * @param str
     *            weekday name to check
     * 
     * @return <code>true</code> if the supplied string is a weekday name.
     */
    private static final boolean isWeekdayName(String str) {
        if (str == null || str.length() < 3) {
            return false;
        }

        String lstr = str.toLowerCase();
        for (String element : WEEKDAY_NAMES) {
            if (lstr.startsWith(element)
                    || element.toLowerCase().startsWith(lstr)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Load list of time zones if sun.util.calendar.ZoneInfo exists.
     * 
     * @return <code>null</code> if time zone list cannot be loaded.
     */
    private static final String[] loadTimeZoneNames() {
        Class<?> zoneInfo;
        try {
            zoneInfo = Class.forName("sun.util.calendar.ZoneInfo");
        } catch (ClassNotFoundException cnfe) {
            return null;
        }

        Method method;
        try {
            method = zoneInfo.getDeclaredMethod("getAvailableIDs", new Class[0]);
        } catch (NoSuchMethodException nsme) {
            return null;
        }

        Object result;
        try {
            result = method.invoke((Object) null);
        } catch (IllegalAccessException iae) {
            return null;
        } catch (InvocationTargetException ite) {
            return null;
        }

        String[] tmpList = (String[]) result;

        int numSaved = 0;
        String[] finalList = null;

        for (int i = 0; i < 2; i++) {
            if (i > 0) {
                if (numSaved == 0) {
                    return null;
                }

                finalList = new String[numSaved];
                numSaved = 0;
            }

            for (int j = 0; j < tmpList.length; j++) {
                final int len = tmpList[j].length();
                if ((len > 2 && Character.isUpperCase(tmpList[j].charAt(1)))
                        && (len != 7 || !Character.isDigit(tmpList[j].charAt(3)))) {
                    if (finalList == null) {
                        numSaved++;
                    } else {
                        finalList[numSaved++] = tmpList[j];
                    }

                    if (len == 3 && tmpList[j].charAt(1) == 'S'
                            && tmpList[j].charAt(2) == 'T') {
                        if (finalList == null) {
                            numSaved++;
                        } else {
                            StringBuffer dst = new StringBuffer();
                            dst.append(tmpList[j].charAt(0));
                            dst.append("DT");
                            finalList[numSaved++] = dst.toString();
                        }
                    }
                }
            }
        }

        return finalList;
    }

    /**
     * Convert the supplied month name to its numeric representation. <br>
     * For example, <code>"January"</code> (or any substring) would return <code>1</code> and <code>"December"</code>
     * would return <code>12</code>.
     * 
     * @param str
     *            month name
     * 
     * @return the numeric month, or <code>CalendarParser.UNSET</code> if the supplied string is not a valid month name.
     */
    public static int monthNameToNumber(String str) {
        if (str != null && str.length() >= 3) {
            String lstr = str.toLowerCase();
            for (int i = 0; i < MONTHS.length; i++) {
                if (lstr.startsWith(MONTHS[i][0])
                        || MONTHS[i][1].toLowerCase().startsWith(lstr)) {
                    return i + 1;
                }
            }
        }

        return UNSET;
    }

    /**
     * Extract a date from a string, defaulting to YY-MM-DD order for all-numeric strings.
     * 
     * @param dateStr
     *            date string
     * 
     * @return parsed date
     * 
     * @throws CalendarParserException
     *             if there was a problem parsing the string.
     */
    public static final Calendar parse(String dateStr)
            throws CalendarParserException {
        return parse(dateStr, YY_MM_DD);
    }

    public static final OffsetDateTime parseAsOffsetDateTime(String dateStr) throws CalendarParserException {
        return ParsingUtilities.calendarToOffsetDateTime(parse(dateStr));
    }

    /**
     * Extract a date from a string.
     * 
     * @param dateStr
     *            date string
     * @param order
     *            order in which pieces of numeric strings are assigned (should be one of <code>YY_MM_DD</code>,
     *            <code>MM_DD_YY</code>, etc.)
     * 
     * @return parsed date
     * 
     * @throws CalendarParserException
     *             if there was a problem parsing the string.
     */
    public static final Calendar parse(String dateStr, int order)
            throws CalendarParserException {
        return parse(dateStr, order, true);
    }

    public static final OffsetDateTime parseAsOffsetDateTime(String dateStr, int order)
            throws CalendarParserException {
        return ParsingUtilities.calendarToOffsetDateTime(parse(dateStr, order));
    }

    /**
     * Extract a date from a string.
     * 
     * @param dateStr
     *            date string
     * @param order
     *            order in which pieces of numeric strings are assigned (should be one of <code>YY_MM_DD</code>,
     *            <code>MM_DD_YY</code>, etc.)
     * @param ignoreChanges
     *            if <code>true</code>, ignore date changes such as <code>Feb 31</code> being changed to
     *            <code>Mar 3</code>.
     * 
     * @return parsed date
     * 
     * @throws CalendarParserException
     *             if there was a problem parsing the string.
     */
    public static final Calendar parse(String dateStr, int order,
            boolean ignoreChanges) throws CalendarParserException {
        if (dateStr == null) {
            return null;
        }

        return parseString(dateStr, order, ignoreChanges);
    }

    public static final OffsetDateTime parseAsOffsetDateTime(String dateStr, int order,
            boolean ignoreChanges) throws CalendarParserException {
        return ParsingUtilities.calendarToOffsetDateTime(parse(dateStr, order, ignoreChanges));
    }

    /**
     * Parse a non-numeric token from the date string.
     * 
     * @param dateStr
     *            full date string
     * @param state
     *            parser state
     * @param token
     *            string being parsed
     * 
     * @throws CalendarParserException
     *             if there was a problem parsing the token
     */
    private static final void parseNonNumericToken(String dateStr,
            ParserState state, String token) throws CalendarParserException {
        // if it's a weekday name, ignore it
        if (isWeekdayName(token)) {
            if (DEBUG) {
                System.err.println("IGNORE \"" + token + "\" (weekday)");
            }
            return;
        }

        // if it looks like a time, deal with it
        if (token.indexOf(':') > 0) {
            final char firstChar = token.charAt(0);
            if (Character.isDigit(firstChar)) {
                parseTime(dateStr, state, token);
                return;
            } else if (firstChar == '+' || firstChar == '-') {
                parseTimeZoneOffset(dateStr, state, token);
                return;
            } else {
                throw new CalendarParserException("Unrecognized time \""
                        + token + "\" in date \"" + dateStr + "\"");
            }
        }

        // try to parse month name
        int tmpMon = monthNameToNumber(token);

        // if token isn't a month name ... PUKE
        if (tmpMon != UNSET) {

            // if month number is unset, set it and move on
            if (!state.isMonthSet()) {
                state.setMonth(tmpMon);
                if (DEBUG) {
                    System.err.println("MONTH="
                            + MONTHS[state.getMonth() - 1][0] + " (" + token
                            + ") name");
                }
                return;
            }

            // try to move the current month value to the year or day
            if (!state.isYearSet()) {
                if (state.isDateSet() || state.isYearBeforeDay()) {
                    state.setYear(state.getMonth());
                    state.setMonth(tmpMon);
                    if (DEBUG) {
                        System.err.println("MONTH="
                                + MONTHS[state.getMonth() - 1][0] + ", YEAR="
                                + state.getYear() + " (" + token
                                + ") name swap");
                    }
                } else {
                    state.setDate(state.getMonth());
                    state.setMonth(tmpMon);
                    if (DEBUG) {
                        System.err.println("MONTH="
                                + MONTHS[state.getMonth() - 1][0] + ", DAY="
                                + state.getDate() + " (" + token
                                + ") name swap");
                    }
                }

                return;
            }

            // year was already set, so try to move month value to day
            if (!state.isDateSet()) {
                state.setDate(state.getMonth());
                state.setMonth(tmpMon);
                if (DEBUG) {
                    System.err.println("MONTH="
                            + MONTHS[state.getMonth() - 1][0] + ", DAY="
                            + state.getDate() + " (" + token + ") name swap 2");
                }

                return;
            }

            // can't move month value to year or day ... PUKE
            if (DEBUG) {
                System.err.println("*** Too many numbers in \"" + dateStr
                        + "\"");
            }
            throw new CalendarParserException("Too many numbers in"
                    + " date \"" + dateStr + "\"");
        }

        // maybe it's an ordinal number list "1st", "23rd", etc.
        int val = getOrdinalNumber(token);
        if (val == UNSET) {
            final String lToken = token.toLowerCase();

            if (lToken.equals("am")) {
                // don't need to do anything
                if (DEBUG) {
                    System.err.println("TIME=AM (" + token + ")");
                }
                return;
            } else if (lToken.equals("pm")) {
                if (!state.isHourSet()) {
                    state.setTimePostMeridian(true);
                } else {
                    state.setHour(state.getHour() + 12);
                }

                if (DEBUG) {
                    System.err.println("TIME=PM (" + token + ")");
                }
                return;
            } else if (zoneNames != null) {
                // maybe it's a time zone name
                for (String zoneName : zoneNames) {
                    if (token.equalsIgnoreCase(zoneName)) {
                        TimeZone tz = TimeZone.getTimeZone(token);
                        if (tz.getRawOffset() != 0 || lToken.equals("gmt")) {
                            state.setTimeZone(tz);
                            return;
                        }
                    }
                }
            }

            if (DEBUG) {
                System.err.println("*** Unknown string \"" + token + "\"");
            }
            throw new CalendarParserException("Unknown string \"" + token
                    + "\" in date \"" + dateStr + "\"");
        }

        // if no day yet, we're done
        if (!state.isDateSet()) {
            state.setDate(val);
            if (DEBUG) {
                System.err.println("DAY=" + state.getDate() + " (" + token
                        + ") ord");
            }
            return;
        }

        // if either year or month is unset...
        if (!state.isYearSet() || !state.isMonthSet()) {

            // if day can't be a month, shift it into year
            if (state.getDate() > 12) {
                if (!state.isYearSet()) {
                    state.setYear(state.getDate());
                    state.setDate(val);
                    if (DEBUG) {
                        System.err.println("YEAR=" + state.getYear() + ", DAY="
                                + state.getDate() + " (" + token
                                + ") ord>12 swap");
                    }
                    return;
                }

                // year was already set, maybe we can move it to month
                if (state.getYear() <= 12) {
                    state.setMonth(state.getYear());
                    state.setYear(state.getDate());
                    state.setDate(val);

                    if (DEBUG) {
                        System.err.println("YEAR=" + state.getYear()
                                + ", MONTH=" + state.getMonth() + ", DAY="
                                + state.getDate() + " (" + token
                                + ") ord megaswap");
                    }

                    return;
                }

                // try to shift day value to either year or month
            } else if (!state.isYearSet()) {
                if (!state.isMonthSet() && !state.isYearBeforeMonth()) {
                    state.setMonth(state.getDate());
                    state.setDate(val);
                    if (DEBUG) {
                        System.err.println("MONTH=" + state.getMonth()
                                + ", DAY=" + state.getDate() + " (" + token
                                + ") ord swap");
                    }
                    return;
                }

                state.setYear(state.getDate());
                state.setDate(val);
                if (DEBUG) {
                    System.err.println("YEAR=" + state.getYear() + ", DAY="
                            + state.getDate() + " (" + token + ") ord swap");
                }
                return;

                // year was set, so we know month is unset
            } else {

                state.setMonth(state.getDate());
                state.setDate(val);
                if (DEBUG) {
                    System.err.println("MONTH=" + state.getMonth() + ", DAY="
                            + state.getDate() + " (" + token + ") ord swap#2");
                }
                return;
            }
        }

        if (DEBUG) {
            System.err.println("*** Extra number \"" + token + "\"");
        }
        throw new CalendarParserException("Cannot assign ordinal in \""
                + dateStr + "\"");
    }

    /**
     * Split a large numeric value into a year/month/date values.
     * 
     * @param dateStr
     *            full date string
     * @param state
     *            parser state
     * @param val
     *            numeric value to use
     * 
     * @throws CalendarParserException
     *             if there was a problem splitting the value
     */
    private static final void parseNumericBlob(String dateStr,
            ParserState state, int val) throws CalendarParserException {
        if (state.isYearSet() || state.isMonthSet() || state.isDateSet()) {
            throw new CalendarParserException("Unknown value " + val
                    + " in date \"" + dateStr + "\"");
        }

        int tmpVal = val;
        if (state.isYearBeforeMonth()) {
            if (state.isYearBeforeDay()) {
                final int last = tmpVal % 100;
                tmpVal /= 100;

                final int middle = tmpVal % 100;
                tmpVal /= 100;

                state.setYear(tmpVal);
                if (state.isMonthBeforeDay()) {
                    // YYYYMMDD
                    state.setMonth(middle);
                    state.setDate(last);
                } else {
                    // YYYYDDMM
                    state.setDate(middle);
                    state.setMonth(last);
                }
            } else {
                // DDYYYYMM
                state.setMonth(tmpVal % 100);
                tmpVal /= 100;

                state.setYear(tmpVal % 10000);
                tmpVal /= 10000;

                state.setDate(tmpVal);
            }
        } else if (state.isYearBeforeDay()) {
            // MMYYYYDD
            state.setDate(tmpVal % 100);
            tmpVal /= 100;

            state.setYear(tmpVal % 10000);
            tmpVal /= 10000;

            state.setMonth(tmpVal);
        } else {
            state.setYear(tmpVal % 10000);
            tmpVal /= 10000;

            final int middle = tmpVal % 100;
            tmpVal /= 100;
            if (state.isMonthBeforeDay()) {
                // MMDDYYYY
                state.setDate(middle);
                state.setMonth(tmpVal);
            } else {
                // DDMMYYYY
                state.setDate(tmpVal);
                state.setMonth(middle);
            }
        }

        if (DEBUG) {
            System.err.println("YEAR=" + state.getYear() + " MONTH="
                    + state.getMonth() + " DAY=" + state.getDate() + " (" + val
                    + ") blob");
        }
    }

    /**
     * Use a numeric token from the date string.
     * 
     * @param dateStr
     *            full date string
     * @param state
     *            parser state
     * @param val
     *            numeric value to use
     * 
     * @throws CalendarParserException
     *             if there was a problem parsing the token
     */
    private static final void parseNumericToken(String dateStr,
            ParserState state, int val) throws CalendarParserException {
        // puke if we've already found 3 values
        if (state.isYearSet() && state.isMonthSet() && state.isDateSet()) {
            if (DEBUG) {
                System.err.println("*** Extra number " + val);
            }
            throw new CalendarParserException("Extra value \"" + val
                    + "\" in date \"" + dateStr + "\"");
        }

        // puke up on negative numbers
        if (val < 0) {
            if (DEBUG) {
                System.err.println("*** Negative number " + val);
            }
            throw new CalendarParserException("Found negative number in"
                    + " date \"" + dateStr + "\"");
        }

        if (val > 9999) {
            parseNumericBlob(dateStr, state, val);
            return;
        }

        // deal with obvious years first
        if (val > 31) {

            // if no year yet, assign it and move on
            if (!state.isYearSet()) {
                state.setYear(val);
                if (DEBUG) {
                    System.err.println("YEAR=" + state.getYear() + " (" + val
                            + ") >31");
                }
                return;
            }

            // puke if the year value can't possibly be a day or month
            if (state.getYear() > 31) {
                if (DEBUG) {
                    System.err.println("*** Ambiguous year " + state.getYear()
                            + " vs. " + val);
                }
                String errMsg = "Couldn't decide on year number in date \""
                        + dateStr + "\"";
                throw new CalendarParserException(errMsg);
            }

            // if the year value can't be a month...
            if (state.getYear() > 12) {

                // if day isn't set, use old val as day and new val as year
                if (!state.isDateSet()) {
                    state.setDate(state.getYear());
                    state.setYear(val);

                    if (DEBUG) {
                        System.err.println("YEAR=" + state.getYear() + ", DAY="
                                + state.getDate() + " (" + val + ") >31 swap");
                    }

                    return;
                }

                // NOTE: both day and year are set

                // try using day value as month so we can move year
                // value to day and use new value as year
                if (state.getDate() <= 12) {
                    state.setMonth(state.getDate());
                    state.setDate(state.getYear());
                    state.setYear(val);

                    if (DEBUG) {
                        System.err.println("YEAR=" + state.getYear()
                                + ", MONTH=" + state.getMonth() + ", DAY="
                                + state.getDate() + " (" + val
                                + ") >31 megaswap");
                    }

                    return;
                }

                if (DEBUG) {
                    System.err.println("*** Unassignable year-like"
                            + " number " + val);
                }
                throw new CalendarParserException("Bad number " + val
                        + " found in date \"" + dateStr + "\"");
            }

            // NOTE: year <= 12

            if (!state.isDateSet() && !state.isMonthSet()) {
                if (state.isMonthBeforeDay()) {
                    state.setMonth(state.getYear());
                    state.setYear(val);
                    if (DEBUG) {
                        System.err.println("YEAR=" + state.getYear()
                                + ", MONTH=" + state.getMonth() + " (" + val
                                + ") >31 swap");
                    }
                } else {
                    state.setDate(state.getYear());
                    state.setYear(val);
                    if (DEBUG) {
                        System.err
                                .println("YEAR=" + state.getYear() + ", DAY="
                                        + state.getDate() + " (" + val
                                        + ") >31 swap#2");
                    }
                }

                return;
            }

            if (!state.isDateSet()) {
                state.setDate(state.getYear());
                state.setYear(val);
                if (DEBUG) {
                    System.err.println("YEAR=" + state.getYear() + ", DAY="
                            + state.getDate() + " (" + val + ") >31 day swap");
                }
                return;
            }

            // assume this was a mishandled month
            state.setMonth(state.getYear());
            state.setYear(val);

            if (DEBUG) {
                System.err.println("YEAR=" + state.getYear() + ", MONTH="
                        + state.getMonth() + " (" + val + ") >31 mon swap");
            }

            return;
        }

        // now deal with non-month values
        if (val > 12) {

            // if no year value yet...
            if (!state.isYearSet()) {

                // if the day is set, or if we assign year before day...
                if (state.isDateSet() || state.isYearBeforeDay()) {
                    state.setYear(val);
                    if (DEBUG) {
                        System.err.println("YEAR=" + state.getYear() + " ("
                                + val + ") >12");
                    }
                } else {
                    state.setDate(val);
                    if (DEBUG) {
                        System.err.println("DAY=" + state.getDate() + " ("
                                + val + ") >12");
                    }
                }

                return;
            }

            // NOTE: year is set

            // if no day value yet, assign it and move on
            if (!state.isDateSet()) {
                state.setDate(val);

                if (DEBUG) {
                    System.err.println("DAY=" + state.getDate() + " (" + val
                            + ") >12 !yr");
                }

                return;
            }

            // NOTE: both year and day are set

            // XXX see if we can shift things around

            if (DEBUG) {
                System.err.println("*** Unassignable year/day number " + val);
            }
            throw new CalendarParserException("Bad number " + val
                    + " found in date \"" + dateStr + "\"");
        }

        // NOTE: ambiguous value

        // if year is set, this must be either the month or day
        if (state.isYearSet()) {
            if (state.isMonthSet()
                    || (!state.isDateSet() && !state.isMonthBeforeDay())) {
                state.setDate(val);
                if (DEBUG) {
                    System.err.println("DAY=" + state.getDate() + " (" + val
                            + ") ambig!yr");
                }
            } else {
                state.setMonth(val);
                if (DEBUG) {
                    System.err.println("MONTH=" + state.getMonth() + " (" + val
                            + ") ambig!yr");
                }
            }

            return;
        }

        // NOTE: year not set

        // if month is set, this must be either the year or day
        if (state.isMonthSet()) {
            if (state.isDateSet() || state.isYearBeforeDay()) {
                state.setYear(val);
                if (DEBUG) {
                    System.err.println("YEAR=" + state.getYear() + " (" + val
                            + ") ambig!mo");
                }
            } else {
                state.setDate(val);
                if (DEBUG) {
                    System.err.println("DAY=" + state.getDate() + " (" + val
                            + ") ambig!mo");
                }
            }

            return;
        }

        // NOTE: neither year nor month is set

        // if day is set, this must be either the year or month
        if (state.isDateSet()) {
            if (state.isYearBeforeMonth()) {
                state.setYear(val);
                if (DEBUG) {
                    System.err.println("YEAR=" + state.getYear() + " (" + val
                            + ") ambig!day");
                }
            } else {
                state.setMonth(val);
                if (DEBUG) {
                    System.err.println("MONTH=" + state.getMonth() + " (" + val
                            + ") ambig!day");
                }
            }

            return;
        }

        // NOTE: no value set yet
        if (state.isYearBeforeMonth()) {
            if (state.isYearBeforeDay()) {
                state.setYear(val);
                if (DEBUG) {
                    System.err.println("YEAR=" + state.getYear() + " (" + val
                            + ") YM|YD");
                }
            } else {
                state.setDate(val);
                if (DEBUG) {
                    System.err.println("DAY=" + state.getDate() + " (" + val
                            + ") YM!YD");
                }
            }
        } else if (state.isMonthBeforeDay()) {
            state.setMonth(val);
            if (DEBUG) {
                System.err.println("MONTH=" + state.getMonth() + " (" + val
                        + ") !YM|MD");
            }
        } else {
            state.setDate(val);
            if (DEBUG) {
                System.err.println("DAY=" + state.getDate() + " (" + val
                        + ") !YM!MD");
            }
        }
    }

    /**
     * Extract a date from the supplied string.
     * 
     * @param dateStr
     *            string to parse
     * @param order
     *            year/month/day order (YY_MM_DD, MM_DD_YY, etc.)
     * @param ignoreChanges
     *            if <code>true</code>, ignore date changes such as <code>Feb 31</code> being changed to
     *            <code>Mar 3</code>.
     * 
     * @return parsed date
     * 
     * @throws CalendarParserException
     *             if no valid date was found.
     */
    private static final Calendar parseString(String dateStr, int order,
            boolean ignoreChanges) throws CalendarParserException {
        ParserState state = new ParserState(order);

        Pattern pat = Pattern.compile("([\\s/,]+|(\\S)\\-)");
        Matcher matcher = pat.matcher(dateStr);

        int prevEnd = 0;
        while (prevEnd < dateStr.length()) {
            String token;
            if (!matcher.find()) {
                token = dateStr.substring(prevEnd);
                prevEnd = dateStr.length();
            } else {
                final boolean isMinus = (matcher.groupCount() == 2 && matcher
                        .group(2) != null);

                if (!isMinus) {
                    token = dateStr.substring(prevEnd, matcher.start());
                } else {
                    token = dateStr.substring(prevEnd, matcher.start())
                            + matcher.group(2);
                }

                prevEnd = matcher.end();
            }

            if (DEBUG) {
                System.err.println("YEAR "
                        + (state.isYearSet() ? Integer
                                .toString(state.getYear()) : "UNSET")
                        + ", MONTH "
                        + (state.isMonthSet() ? Integer.toString(state
                                .getMonth()) : "UNSET")
                        + ", DAY "
                        + (state.isDateSet() ? Integer
                                .toString(state.getDate()) : "UNSET")
                        + ", TOKEN=\"" + token + "\"");
            }

            // try to decipher next token as a number
            try {
                final int val = Integer.parseInt(token);
                parseNumericToken(dateStr, state, val);
            } catch (NumberFormatException e) {
                parseNonNumericToken(dateStr, state, token);
            }
        }

        // before checking for errors, check for missing year
        if (!state.isDateSet() && state.getYear() <= 31) {
            int tmp = state.getDate();
            state.setDate(state.getYear());
            state.setYear(tmp);
        }

        if (!state.isDateSet()) {
            if (!state.isMonthSet()) {
                if (!state.isYearSet()) {
                    throw new CalendarParserException("No date found in \""
                            + dateStr + "\"");
                } else {
                    throw new CalendarParserException("Day and month missing"
                            + " from \"" + dateStr + "\"");
                }
            } else {
                throw new CalendarParserException("Day missing from \""
                        + dateStr + "\"");
            }
        } else if (!state.isMonthSet()) {
            if (!state.isYearSet()) {
                throw new CalendarParserException("Year and month missing"
                        + " from \"" + dateStr + "\"");
            } else {
                throw new CalendarParserException("Month missing from \""
                        + dateStr + "\"");
            }
        } else if (!state.isYearSet()) {
            throw new CalendarParserException("Year missing from \"" + dateStr
                    + "\"");
        }

        final int tmpYear = state.getYear();
        if (tmpYear < 50) {
            state.setYear(tmpYear + CENTURY_OFFSET);
        } else if (tmpYear < 100) {
            state.setYear(tmpYear + (CENTURY_OFFSET - 100));
        }

        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("Z"));

        state.setCalendar(cal, ignoreChanges);

        if (DEBUG) {
            System.err.println("Y" + state.getYear() + " M" + state.getMonth()
                    + " D" + state.getDate() + " H" + state.getHour() + " M"
                    + state.getMinute() + " S" + state.getSecond() + " L"
                    + state.getMillisecond() + " => " + toString(cal));
        }

//        return cal.toInstant().atOffset(ZoneOffset.of("Z"));
        return cal;
    }

    /**
     * Parse a time string.
     * 
     * @param dateStr
     *            full date string
     * @param state
     *            parser state
     * @param timeStr
     *            string containing colon-separated time
     * 
     * @throws CalendarParserException
     *             if there is a problem with the time
     */
    private static final void parseTime(String dateStr, ParserState state,
            String timeStr) throws CalendarParserException {
        int place = PLACE_HOUR;

        String tmpTime;

        final char lastChar = timeStr.charAt(timeStr.length() - 1);
        if (lastChar != 'm' && lastChar != 'M') {
            if (DEBUG) {
                System.err.println("No AM/PM in \"" + timeStr + "\" (time)");
            }
            tmpTime = timeStr;
        } else {
            final char preLast = timeStr.charAt(timeStr.length() - 2);
            if (preLast == 'a' || preLast == 'A') {
                state.setTimePostMeridian(false);
            } else if (preLast == 'p' || preLast == 'P') {
                state.setTimePostMeridian(true);
            } else {
                throw new CalendarParserException("Bad time \"" + timeStr
                        + "\" in date \"" + dateStr + "\"");
            }

            tmpTime = timeStr.substring(0, timeStr.length() - 2);
            if (DEBUG) {
                System.err.println("Found "
                        + (state.isTimePostMeridian() ? "PM" : "AM")
                        + ". now \"" + tmpTime + "\" (time)");
            }
        }

        String[] tList = tmpTime.split("[:\\.]");
        for (String token : tList) {
            if (DEBUG) {
                System.err.println("HOUR "
                        + (state.isHourSet() ? Integer
                                .toString(state.getHour()) : "UNSET")
                        + ", MINUTE "
                        + (state.isMinuteSet() ? Integer.toString(state
                                .getMinute()) : "UNSET")
                        + ", SECOND "
                        + (state.isSecondSet() ? Integer.toString(state
                                .getSecond()) : "UNSET")
                        + ", MILLISECOND "
                        + (state.isMillisecondSet() ? Integer.toString(state
                                .getMillisecond()) : "UNSET")
                        + ", TOKEN=\""
                        + token + "\"");
            }

            final int val;
            try {
                val = Integer.parseInt(token);
            } catch (NumberFormatException nfe) {
                throw new CalendarParserException("Bad "
                        + getTimePlaceString(place) + " string \"" + token
                        + "\" in \"" + dateStr + "\"");
            }

            switch (place) {
                case PLACE_HOUR:
                    try {
                        state.setHour(val);
                    } catch (CalendarParserException dfe) {
                        throw new CalendarParserException(dfe.getMessage()
                                + " in \"" + dateStr + "\"");
                    }
                    if (DEBUG) {
                        System.err.println("Set hour to " + val);
                    }
                    place = PLACE_MINUTE;
                    break;
                case PLACE_MINUTE:
                    try {
                        state.setMinute(val);
                    } catch (CalendarParserException dfe) {
                        throw new CalendarParserException(dfe.getMessage()
                                + " in \"" + dateStr + "\"");
                    }
                    if (DEBUG) {
                        System.err.println("Set minute to " + val);
                    }
                    place = PLACE_SECOND;
                    break;
                case PLACE_SECOND:
                    try {
                        state.setSecond(val);
                    } catch (CalendarParserException dfe) {
                        throw new CalendarParserException(dfe.getMessage()
                                + " in \"" + dateStr + "\"");
                    }
                    if (DEBUG) {
                        System.err.println("Set second to " + val);
                    }
                    place = PLACE_MILLI;
                    break;
                case PLACE_MILLI:
                    try {
                        state.setMillisecond(val);
                    } catch (CalendarParserException dfe) {
                        throw new CalendarParserException(dfe.getMessage()
                                + " in \"" + dateStr + "\"");
                    }
                    if (DEBUG) {
                        System.err.println("Set millisecond to " + val);
                    }
                    place = PLACE_UNKNOWN;
                    break;
                default:
                    throw new CalendarParserException("Unexpected place value "
                            + place);
            }
        }
    }

    /**
     * Parse a time zone offset string.
     * 
     * @param dateStr
     *            full date string
     * @param state
     *            parser state
     * @param zoneStr
     *            string containing colon-separated time zone offset
     * 
     * @throws CalendarParserException
     *             if there is a problem with the time
     */
    private static final void parseTimeZoneOffset(String dateStr,
            ParserState state, String zoneStr) throws CalendarParserException {
        int place = PLACE_HOUR;

        final boolean isNegative = (zoneStr.charAt(0) == '-');
        if (!isNegative && zoneStr.charAt(0) != '+') {
            throw new CalendarParserException("Bad time zone offset \""
                    + zoneStr + "\" in date \"" + dateStr + "\"");
        }

        int hour = UNSET;
        int minute = UNSET;

        String[] tList = zoneStr.substring(1).split(":");
        for (String token : tList) {
            if (DEBUG) {
                System.err
                        .println("TZ_HOUR "
                                + (hour != UNSET ? Integer.toString(hour)
                                        : "UNSET")
                                + ", TZ_MINUTE "
                                + (minute != UNSET ? Integer.toString(minute)
                                        : "UNSET")
                                + ", TOKEN=\"" + token
                                + "\"");
            }

            final int val;
            try {
                val = Integer.parseInt(token);
            } catch (NumberFormatException nfe) {
                throw new CalendarParserException("Bad time zone "
                        + getTimePlaceString(place) + " offset \"" + token
                        + "\" in \"" + dateStr + "\"");
            }

            switch (place) {
                case PLACE_HOUR:
                    hour = val;
                    if (DEBUG) {
                        System.err.println("Set time zone offset hour to " + val);
                    }
                    place = PLACE_MINUTE;
                    break;
                case PLACE_MINUTE:
                    minute = val;
                    if (DEBUG) {
                        System.err.println("Set time zone offset minute to " + val);
                    }
                    place = PLACE_UNKNOWN;
                    break;
                default:
                    throw new CalendarParserException("Unexpected place value "
                            + place);
            }
        }

        String customID = "GMT" + (isNegative ? "-" : "+") + hour + ":"
                + (minute < 10 ? "0" : "") + minute;

        state.setTimeZone(TimeZone.getTimeZone(customID));
    }

    /**
     * Return a printable representation of the date.
     * 
     * @param cal
     *            calendar to convert to a string
     * 
     * @return a printable string.
     */
    public static final String prettyString(Calendar cal) {
        if (cal == null) {
            return null;
        }

        final int calYear = cal.get(Calendar.YEAR);
        final int calMonth = cal.get(Calendar.MONTH);
        final int calDay = cal.get(Calendar.DATE);

        boolean needSpace = false;
        StringBuffer buf = new StringBuffer();

        if (calMonth >= 0 && calMonth < MONTHS.length) {
            if (needSpace) {
                buf.append(' ');
            }
            buf.append(MONTHS[calMonth][1]);
            needSpace = true;
        }
        if (calDay > 0) {
            if (needSpace) {
                buf.append(' ');
            }
            buf.append(calDay);
            if (calYear > UNSET) {
                buf.append(',');
            }
            needSpace = true;
        }
        if (calYear > UNSET) {
            if (needSpace) {
                buf.append(' ');
            }
            buf.append(calYear);
        }

        appendTimeString(buf, cal, needSpace);

        return buf.toString();
    }

    /**
     * Return a basic representation of the string.
     * 
     * @param cal
     *            calendar to convert to a string
     * 
     * @return the basic string.
     */
    public static final String toString(Calendar cal) {
        if (cal == null) {
            return null;
        }

        final int calYear = cal.get(Calendar.YEAR);
        final int calMonth = cal.get(Calendar.MONTH);
        final int calDay = cal.get(Calendar.DATE);

        boolean needSpace = false;
        StringBuffer buf = new StringBuffer();

        if (calDay > 0) {
            if (needSpace) {
                buf.append(' ');
            }
            buf.append(calDay);
            needSpace = true;
        }
        if (calMonth >= 0 && calMonth < MONTHS.length) {
            if (needSpace) {
                buf.append(' ');
            }
            buf.append(MONTHS[calMonth][1].substring(0, 3));
            needSpace = true;
        }
        if (calYear > UNSET) {
            if (needSpace) {
                buf.append(' ');
            }
            buf.append(calYear);
        }

        appendTimeString(buf, cal, needSpace);

        return buf.toString();
    }

    /**
     * Return a string representation of the date suitable for use in an SQL statement.
     * 
     * @param cal
     *            calendar to convert to a string
     * 
     * @return the SQL-friendly string.
     */
    public static final String toSQLString(Calendar cal) {
        if (cal == null) {
            return null;
        }

        final int calYear = cal.get(Calendar.YEAR);
        final int calMonth = cal.get(Calendar.MONTH);
        final int calDay = cal.get(Calendar.DATE);

        StringBuffer buf = new StringBuffer();

        buf.append(calYear);
        buf.append('-');
        if ((calMonth + 1) < 10) {
            buf.append('0');
        }
        buf.append(calMonth + 1);
        buf.append('-');
        if (calDay < 10) {
            buf.append('0');
        }
        buf.append(calDay);

        appendTimeString(buf, cal, true);

        return buf.toString();
    }
}
