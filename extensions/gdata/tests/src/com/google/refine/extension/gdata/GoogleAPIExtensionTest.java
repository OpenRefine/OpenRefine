
package com.google.refine.extension.gdata;

import com.google.refine.ProjectManager;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

public class GoogleAPIExtensionTest {

    @Test
    public void extractSpreadSheetIdTest() {
        // GoogleAPIExtension will call ProjectManager.singleton.getPreferenceStore() during class initialization,
        // which will cause NullPointerException if this line is omitted.
        ProjectManager.singleton = mock(ProjectManager.class);

        String spreadSheetId = "16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0";

        String url1 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0/edit#gid=0";
        assertEquals(GoogleAPIExtension.extractSpreadSheetId(url1), spreadSheetId);

        String url2 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0/";
        assertEquals(GoogleAPIExtension.extractSpreadSheetId(url2), spreadSheetId);

        String url3 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0?foo=bar";
        assertEquals(GoogleAPIExtension.extractSpreadSheetId(url3), spreadSheetId);

        String url4 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0/?foo=bar";
        assertEquals(GoogleAPIExtension.extractSpreadSheetId(url4), spreadSheetId);

        String url5 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0#foo";
        assertEquals(GoogleAPIExtension.extractSpreadSheetId(url5), spreadSheetId);

        String url6 = "https://docs.google.com/spreadsheets/d/16L0JfpBWPfBJTqKtm-YU5-UBWLpkwXII-IRLMLnoKw0";
        assertEquals(GoogleAPIExtension.extractSpreadSheetId(url6), spreadSheetId);
    }
}
