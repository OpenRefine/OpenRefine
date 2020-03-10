package com.google.refine.extension.gdata;

import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

public class SpreadsheetRETest {

	/**
	 * We cannot call {@link GoogleAPIExtension#extractSpreadSheetId(String url) } here,
	 * otherwise, we'll get NullPointerException during its class initialization.
	 *
	 * So we just copy the regex and test it here.
	 */
	@Test
	public void reTest() {
		String spreadSheetId = "16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0";

		String url1 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0/edit#gid=0";
		assertEquals(extractSpreadSheetId(url1), spreadSheetId);

		String url2 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0/";
		assertEquals(extractSpreadSheetId(url2), spreadSheetId);

		String url3 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0?foo=bar";
		assertEquals(extractSpreadSheetId(url3), spreadSheetId);

		String url4 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0/?foo=bar";
		assertEquals(extractSpreadSheetId(url4), spreadSheetId);

		String url5 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0#foo";
		assertEquals(extractSpreadSheetId(url5), spreadSheetId);

		String url6 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0";
		assertEquals(extractSpreadSheetId(url6), spreadSheetId);
	}

	private String extractSpreadSheetId(String url) {
		Matcher matcher = Pattern.compile("(?<=/d/).*?(?=[/?#]|$)").matcher(url);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}
}
