
package com.google.refine.expr.util;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
// TODO: The static import below should really use the native parameter ordering, but we're too lazy to switch all the calls
import static org.testng.AssertJUnit.assertEquals; // JUnit compatible parameter order

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CalenderParserTest {

    @DataProvider(name = "parseDate")
    private static Object[][] parseDate() {
        return new Object[][] {
                { "21012024", CalendarParser.DD_MM_YY },
                { "21-01-2024", CalendarParser.DD_MM_YY },
                { "21/01/2024", CalendarParser.DD_MM_YY },
                { "01212024", CalendarParser.MM_DD_YY },
                { "01-21-2024", CalendarParser.MM_DD_YY },
                { "01/21/2024", CalendarParser.MM_DD_YY },
                { "01202421", CalendarParser.MM_YY_DD },
                { "01-2024-21", CalendarParser.MM_YY_DD },
                { "01/2024/21", CalendarParser.MM_YY_DD },
                { "21202401", CalendarParser.DD_YY_MM },
                { "21-2024-01", CalendarParser.DD_YY_MM },
                { "21/2024/01", CalendarParser.DD_YY_MM },
                { "20242101", CalendarParser.YY_DD_MM },
                { "2024-21-01", CalendarParser.YY_DD_MM },
                { "2024/21/01", CalendarParser.YY_DD_MM },
                { "20240121", CalendarParser.YY_MM_DD },
                { "2024-01-21", CalendarParser.YY_MM_DD },
                { "2024/01/21", CalendarParser.YY_MM_DD },
        };
    }

    @Test(dataProvider = "parseDate")
    public void shouldParseDateWhenDifferentFormat_parseTest(String inputDateStr, int inputOrder) throws CalendarParserException {
        Calendar calendar = CalendarParser.parse(inputDateStr, inputOrder);

        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH));
        assertEquals(2024, calendar.get(Calendar.YEAR));
        assertEquals(21, calendar.get(Calendar.DATE));
    }

    @DataProvider(name = "dateWithMonthsAsString")
    private static Object[][] dateWithMonthsAsString() {
        return new Object[][] {
                { "11 April 12", CalendarParser.DD_MM_YY },
                { "April 11 12", CalendarParser.MM_DD_YY },
                { "12 April 11", CalendarParser.YY_MM_DD },
                { "12 11 April", CalendarParser.YY_DD_MM },
                { "April 12 11", CalendarParser.MM_YY_DD },
                { "11 12 April", CalendarParser.DD_YY_MM }
        };
    }

    @Test(dataProvider = "dateWithMonthsAsString")
    public void shouldParseDateWhenMonthAsString_parseTest(String dateInput, int orderInput) throws CalendarParserException {
        Calendar calendar = CalendarParser.parse(dateInput, orderInput);

        assertEquals(11, calendar.get(Calendar.DATE));
        assertEquals(Calendar.APRIL, calendar.get(Calendar.MONTH));
        assertEquals(2012, calendar.get(Calendar.YEAR));
    }

    @DataProvider(name = "dateWithDayAsOrdinalNumber")
    private static Object[][] dateWithDayAsOrdinalNumber() {
        return new Object[][] {
                { "11st 4 12", CalendarParser.DD_MM_YY },
                { "4 11st 12", CalendarParser.MM_DD_YY },
                { "12 4 11st", CalendarParser.YY_MM_DD },
                { "12 11st 4", CalendarParser.YY_DD_MM },
                { "4 12 11st", CalendarParser.MM_YY_DD },
                { "11st 12 4", CalendarParser.DD_YY_MM }
        };
    }

    @Test(dataProvider = "dateWithDayAsOrdinalNumber")
    public void shouldParseDateWhenDayAsOrdinalNumber_parseTest(String dateInput, int orderInput) throws CalendarParserException {
        Calendar calendar = CalendarParser.parse(dateInput, orderInput);

        assertEquals(11, calendar.get(Calendar.DATE));
        assertEquals(Calendar.APRIL, calendar.get(Calendar.MONTH));
        assertEquals(2012, calendar.get(Calendar.YEAR));
    }

    @DataProvider(name = "peculiarDates")
    private static Object[][] peculiarDates() {
        return new Object[][] {
                { "oct 4 12", CalendarParser.DD_MM_YY },
                { "4 oct 12", CalendarParser.MM_DD_YY },
                { "4 oct 12", CalendarParser.MM_YY_DD },
        };
    }

    @Test(dataProvider = "peculiarDates")
    public void shouldParseDateWhenInputDateMismatchWithOrder_parseDate(String dateStr, int order) throws CalendarParserException {
        Calendar calendar = CalendarParser.parse(dateStr, order);

        assertNotNull(calendar);
    }

    @Test
    public void shouldParseDateWhenInputHasTime_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 8:30:54:003am +05:30";
        Calendar calendar = CalendarParser.parse(dateStr);

        assertEquals(8, calendar.get(Calendar.HOUR));
        assertEquals(Calendar.AM, calendar.get(Calendar.AM_PM));
        assertEquals(30, calendar.get(Calendar.MINUTE));
        assertEquals(54, calendar.get(Calendar.SECOND));
        assertEquals(3, calendar.get(Calendar.MILLISECOND));
        assertEquals("GMT+05:30", calendar.getTimeZone().toZoneId().getId());
    }

    @DataProvider(name = "months")
    public static Object[][] months() {
        return new Object[][] {
                { "jana", 1 },
                { "MarCh", 3 },
                { "Dec", 12 },
                { "october", 10 },
                { "JULY", 7 },
                { "xyy", ParserState.UNSET }
        };
    }

    @Test(dataProvider = "months")
    public void monthToNumberTest(String input, int expected) {
        assertEquals(expected, CalendarParser.monthNameToNumber(input));
    }

    @DataProvider(name = "calendersToString")
    public static Object[][] calendersToString() {
        List<Calendar> calendars = getCalenderList();
        return new Object[][] {
                { calendars.get(0), "20 Apr 2024  1:52:22.500 -08:00" },
                { calendars.get(1), "7 Nov 2022 11:06:08.050 +05:30" } };
    }

    @Test(dataProvider = "calendersToString")
    public void calenderToStringTest(Calendar input, String expected) {
        String str = CalendarParser.toString(input);

        assertEquals(expected, str);

        String[] dateTime = str.split("( )+");
        assertEquals(5, dateTime.length);

        String[] time = dateTime[3].split(":");
        assertEquals(3, time.length);

    }

    @Test
    public void whenInputNull_calendarToStringTest() {
        assertNull(CalendarParser.toString(null));
    }

    @DataProvider(name = "orderString")
    private static Object[][] orderString() {
        return new Object[][] {
                { 0, "DD_MM_YY" },
                { 1, "MM_DD_YY" },
                { 3, "MM_YY_DD" },
                { 4, "DD_YY_MM" },
                { 6, "YY_DD_MM" },
                { 7, "YY_MM_DD" },
                { 10, "??10??" }
        };
    }

    @Test(dataProvider = "orderString")
    public void toOrderStringTest(int input, String expected) {
        assertEquals(expected, CalendarParser.getOrderString(input));
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Found negative number in date \"-22/02/2024\"")
    public void shouldThrowExceptionWhenNegativeNumberInDateStr_parseTest() throws CalendarParserException {
        String dateStr = "-22/02/2024";
        CalendarParser.parse(dateStr, CalendarParser.DD_MM_YY);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Year missing from \"20/01/\"")
    public void shouldThrowExceptionWhenYearMissingDD_MM_YY_parseTest() throws CalendarParserException {
        String dateStr = "20/01/";
        CalendarParser.parse(dateStr, CalendarParser.DD_MM_YY);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Month missing from \"20//2024\"")
    public void shouldThrowExceptionWhenMonthMissingDD_MM_YY_parseTest() throws CalendarParserException {
        String dateStr = "20//2024";
        CalendarParser.parse(dateStr, CalendarParser.DD_MM_YY);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "^Day missing from .*")
    public void shouldThrowExceptionWhenDateMissingMM_DD_YY_parseTest() throws CalendarParserException {
        String dateStr = "01//2024";
        CalendarParser.parse(dateStr, CalendarParser.MM_DD_YY);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "^Year and month missing from .*")
    public void shouldThrowExceptionWhenMonthAndYearAreMissingDD_MM_YY_parseTest() throws CalendarParserException {
        String dateStr = "20/";
        CalendarParser.parse(dateStr, CalendarParser.DD_MM_YY);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "^Day and month missing from .*")
    public void shouldThrowExceptionWhenMonthAndDayAreMissingYY_DD_MM_parseTest() throws CalendarParserException {
        String dateStr = "2024-";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad hour 28 in .*")
    public void shouldThrowExceptionWhenHourValueIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 28:30:54:003 pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad hour string \"1hour\" in .*")
    public void shouldThrowExceptionWhenHourIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 1hour:30:54:003 pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad minute 90 in .*")
    public void shouldThrowExceptionWhenMinuteValueIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 8:90:54:003 pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad minute string \"30Minute\" in .*")
    public void shouldThrowExceptionWhenMinuteIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 2:30Minute:54:003 pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad second 94 in .*")
    public void shouldThrowExceptionWhenSecondValueIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 8:30:94:003 pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad second string \"54Sec\" in .*")
    public void shouldThrowExceptionWhenSecondIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 2:30:54Sec:003 pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad millisecond 1111 in .*")
    public void shouldThrowExceptionWhenMilliSecondValueIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 8:30:44:1111 pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Bad millisecond string \"003milli\" in .*")
    public void shouldThrowExceptionWhenMilliSecondIsBad_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 2:30:54:003milli pm +05:30";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "Unrecognized time \"hour:minute:second\" in date .*")
    public void shouldThrowExceptionWhenBadTime_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 hour:minute:second";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    @Test(expectedExceptions = CalendarParserException.class, expectedExceptionsMessageRegExp = "^Bad time zone minute offset \"30min\" in .*")
    public void shouldThrowExceptionWhenBadTimeZone_parseTest() throws CalendarParserException {
        String dateStr = "20/01/2024 8:30:54:003 pm +05:30min";
        CalendarParser.parse(dateStr, CalendarParser.YY_DD_MM);
    }

    private static Calendar getCalendar(int year, int month, int date, int hour, int minutes, int seconds, int milliSeconds,
            TimeZone timeZone) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, date, hour, minutes, seconds);
        cal.set(Calendar.MILLISECOND, milliSeconds);
        cal.setTimeZone(timeZone);
        return cal;
    }

    private static List<Calendar> getCalenderList() {
        List<Calendar> calendars = new ArrayList<>();
        calendars.add(getCalendar(2024, 3, 20, 1, 52, 22, 500, TimeZone.getTimeZone("America/Los_Angeles")));
        calendars.add(getCalendar(2022, 10, 7, 11, 6, 8, 50, TimeZone.getTimeZone("IST")));
        return calendars;
    }

}
