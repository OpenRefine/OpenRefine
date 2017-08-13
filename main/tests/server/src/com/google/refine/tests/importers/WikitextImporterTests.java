/*

Copyright 2010,2011 Google Inc.
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

package com.google.refine.tests.importers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.StringReader;

import org.json.JSONException;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.importers.WikitextImporter;

public class WikitextImporterTests extends ImporterTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";

    //System Under Test
    WikitextImporter importer = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        importer = new WikitextImporter();
    }

    @Override
    @AfterMethod
    public void tearDown(){
        importer = null;
        super.tearDown();
    }
    
    @Test
    public void readSimpleData() {
    	String input = "\n"
		+ "{|\n"
		+ "|-\n"
		+ "| a || b || c \n"
		+ "|-\n"
		+ "| d || e || f\n"
		+ "|-\n"
		+ "|}\n";
    	try {
    	        prepareOptions(0, 0, 0, 0, true);
    		parse(input);
    	} catch (Exception e) {
    		Assert.fail("Parsing failed", e);
    	}
    	Assert.assertEquals(project.columnModel.columns.size(), 3);
    	Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "a");
        Assert.assertEquals(project.rows.get(1).cells.get(2).value, "f");
    }
    
    @Test
    public void readTableWithLinks() {
        // Data credits: Wikipedia contributors, https://de.wikipedia.org/w/index.php?title=Agenturen_der_Europäischen_Union&action=edit
        String input = "\n"
            +"{|\n"
            +"|-\n"
            +"| [[Europäisches Zentrum für die Förderung der Berufsbildung|Cedefop]] || Cedefop || [http://www.cedefop.europa.eu/]\n"
            +"|-\n"
            +"| [[Europäische Stiftung zur Verbesserung der Lebens- und Arbeitsbedingungen]] || EUROFOUND || [http://www.eurofound.europa.eu/]\n"
            +"|-\n"
            +"| [[Europäische Beobachtungsstelle für Drogen und Drogensucht]] || EMCDDA || [http://www.emcdda.europa.eu/]\n"
            +"|-\n"
            +"|}\n";

        try {
                prepareOptions(0, 0, 0, 0, true);
                parse(input);
        } catch (Exception e) {
                Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "Cedefop");
        Assert.assertEquals(project.rows.get(1).cells.get(2).value, "http://www.eurofound.europa.eu/");
    }
/*
    @Test
    public void readStyledTableWithHeader() {
        // Data credits: Wikipedia contributors, https://de.wikipedia.org/w/index.php?title=Agenturen_der_Europäischen_Union&action=edit
        String input = "\n"
            +"{| class=\"wikitable sortable\"\n"
            +"! style=\"text-align:left; width: 60em\" | Offizieller Name\n"
            +"! style=\"text-align:left; width: 9em\" | Abkürzung\n"
            +"! style=\"text-align:left; width: 6em\" | Website\n"
            +"! style=\"text-align:left; width: 15em\" | Standort\n"
            +"! style=\"text-align:left; width: 18em\" | Staat\n"
            +"! style=\"text-align:left; width: 6em\" | Gründung\n"
            +"! style=\"text-align:left; width: 50em\" | Anmerkungen\n"
            +"|-\n"
            +"| [[Europäisches Zentrum für die Förderung der Berufsbildung]] || Cedefop || [http://www.cedefop.europa.eu/] || [[Thessaloniki]] || {{Griechenland}} || 1975 ||\n"
            +"|-\n"
            +"| [[Europäische Stiftung zur Verbesserung der Lebens- und Arbeitsbedingungen]] || EUROFOUND || [http://www.eurofound.europa.eu/] || [[Dublin]] || {{Irland}} || 1975 ||\n"
            +"|-\n"
            +"| [[Europäische Beobachtungsstelle für Drogen und Drogensucht]] || EMCDDA || [http://www.emcdda.europa.eu/] || [[Lissabon]] || {{Portugal}} || 1993 ||\n"
            +"|-\n"
            +"|}\n";

        try {
                prepareOptions(0, 0, 0, 0, true);
                parse(input);
        } catch (Exception e) {
                Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 7);
        Assert.assertEquals(project.rows.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.size(), 7);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "Europäisches Zentrum für die Förderung der Berufsbildung");
        Assert.assertEquals(project.rows.get(1).cells.get(2).value, "http://www.eurofound.europa.eu/");
    }*/

    //--helpers--
    
    private void parse(String wikitext) {
    	parseOneFile(importer, new StringReader(wikitext));
    }

    private void prepareOptions(
        int limit, int skip, int ignoreLines,
        int headerLines, boolean guessValueType) {
        
        whenGetIntegerOption("limit", options, limit);
        whenGetIntegerOption("skipDataLines", options, skip);
        whenGetIntegerOption("ignoreLines", options, ignoreLines);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
    }

    private void verifyOptions() {
        try {
            verify(options, times(1)).getString("separator");
            verify(options, times(1)).getInt("limit");
            verify(options, times(1)).getInt("skipDataLines");
            verify(options, times(1)).getInt("ignoreLines");
            verify(options, times(1)).getInt("headerLines");
            verify(options, times(1)).getBoolean("guessCellValueTypes");
            verify(options, times(1)).getBoolean("processQuotes");
            verify(options, times(1)).getBoolean("storeBlankCellsAsNulls");
        } catch (JSONException e) {
            Assert.fail("JSON exception",e);
        }

    }
}
