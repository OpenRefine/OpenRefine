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

package com.google.refine.importers;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.recon.ReconJob;
import com.google.refine.model.recon.StandardReconConfig;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class WikitextImporterTests extends ImporterTest {

    private WikitextImporter importer = null;
    private Map<String, Recon> mockedRecons = null;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        importer = new WikitextImporter();
        mockedRecons = new HashMap<>();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        importer = null;
        super.tearDown();
    }

    @Test
    public void readSimpleData() {
        String input = "\n"
                + "{|\n"
                + "|-\n"
                + "| a || b<br/>2 || c \n"
                + "|-\n"
                + "| d || e || f<br>\n"
                + "|-\n"
                + "|}\n";
        try {
            prepareOptions(0, 0, true, true, null, null);
            parse(input);
        } catch (Exception e) {
            Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "a");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "b\n2");
        Assert.assertEquals(project.rows.get(1).cells.get(2).value, "f");
    }

    /**
     * Issue #1448 https://github.com/OpenRefine/OpenRefine/issues/1448
     */
    @Test
    public void readTableWithMisplacedHeaders() {
        String input = "\n"
                + "{|\n"
                + "|-\n"
                + "| a || b<br/>2 || c \n"
                + "|-\n"
                + "| d\n"
                + "! e\n"
                + "| f<br>\n"
                + "|-\n"
                + "|}\n";
        try {
            prepareOptions(0, 0, true, true, null, null);
            parse(input);
        } catch (Exception e) {
            Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "e");
        Assert.assertEquals(project.rows.get(1).cells.get(2).value, "f");
    }

    public void readTableWithLinks() throws Exception {

        // Data credits: Wikipedia contributors,
        // https://de.wikipedia.org/w/index.php?title=Agenturen_der_Europäischen_Union&action=edit
        String input = "\n"
                + "{|\n"
                + "|-\n"
                + "| [[Europäisches Zentrum für die Förderung der Berufsbildung|Cedefop]] || Cedefop || http://www.cedefop.europa.eu/\n"
                + "|-\n"
                + "| [[Europäische Stiftung zur Verbesserung der Lebens- und Arbeitsbedingungen]] || EUROFOUND || [http://www.eurofound.europa.eu/]\n"
                + "|-\n"
                + "| [[Europäische Beobachtungsstelle für Drogen und Drogensucht]] || EMCDDA || [http://www.emcdda.europa.eu/ europa.eu]\n"
                + "|-\n"
                + "|}\n";

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String jsonResponse = "{\"q0\":[{\"id\":\"Q1377256\",\"name\":\"Europäische Beobachtungsstelle für Drogen und Drogensucht\"}],"
                    + "\"q1\":[{\"id\":\"Q1377549\",\"name\":\"European Foundation for the Improvement of Living and Working Conditions\"}],"
                    + "\"q2\":[{\"id\":\"Q1377256\",\"name\":\"European Monitoring Centre for Drugs and Drug Addiction\"}]}";
            server.enqueue(new MockResponse().setBody(jsonResponse));

            try {
                prepareOptions(0, 0, true, true, "https://de.wikipedia.org/wiki/", server.url("endpoint").url().toString());
                parse(input);
            } catch (Exception e) {
                Assert.fail("Parsing failed", e);
            }
            Assert.assertEquals(project.columnModel.columns.size(), 3);
            Assert.assertEquals(project.rows.size(), 3);
            Assert.assertEquals(project.rows.get(0).cells.size(), 3);

            // Reconciled cells
            Assert.assertEquals(project.rows.get(0).cells.get(1).value, "Cedefop");
            Assert.assertEquals(project.rows.get(0).cells.get(1).recon, null);
            Assert.assertEquals(project.rows.get(2).cells.get(0).value, "Europäische Beobachtungsstelle für Drogen und Drogensucht");
            Assert.assertEquals(project.rows.get(2).cells.get(0).recon.getBestCandidate().id, "Q1377256");

            // various ways to input external links
            Assert.assertEquals(project.rows.get(1).cells.get(2).value, "http://www.eurofound.europa.eu/");
            Assert.assertEquals(project.rows.get(2).cells.get(2).value, "http://www.emcdda.europa.eu/");
            // Assert.assertEquals(project.rows.get(0).cells.get(2).value, "http://www.cedefop.europa.eu/");
            // unfortunately the above does not seem to be supported by the parser (parsed as blank instead)
        }
    }

    @Test
    public void readStyledTableWithHeader() {
        // Data credits: Wikipedia contributors,
        // https://de.wikipedia.org/w/index.php?title=Agenturen_der_Europäischen_Union&action=edit
        String input = "\n"
                + "==Agenturen==\n"
                + "{| class=\"wikitable sortable\"\n"
                + "! style=\"text-align:left; width: 60em\" | Offizieller Name\n"
                + "! style=\"text-align:left; width: 9em\" | Abkürzung\n"
                + "! style=\"text-align:left; width: 6em\" | Website\n"
                + "! style=\"text-align:left; width: 15em\" | Standort\n"
                + "! style=\"text-align:left; width: 18em\" | Staat\n"
                + "! style=\"text-align:left; width: 6em\" | Gründung\n"
                + "! style=\"text-align:left; width: 50em\" | Anmerkungen\n"
                + "|-\n"
                + "| [[Europäisches Zentrum für die Förderung der Berufsbildung]] || '''Cedefop''' || [http://www.cedefop.europa.eu/] || [[Thessaloniki]] || {{Griechenland}} || 1975 ||\n"
                + "|-\n"
                + "| [[Europäische Stiftung zur Verbesserung der Lebens- und Arbeitsbedingungen]] || ''EUROFOUND'' || [http://www.eurofound.europa.eu/] || [[Dublin]] || {{Irland}} || 1975 ||\n"
                + "|-\n"
                + "| [[Europäische Beobachtungsstelle für Drogen und Drogensucht]] || EMCDDA || [http://www.emcdda.europa.eu/] || [[Lissabon]] || {{Portugal}} || 1993 ||\n"
                + "|-\n"
                + "|}\n";

        try {
            prepareOptions(-1, 1, true, true, null, null);
            parse(input);
        } catch (Exception e) {
            Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 7);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "Europäisches Zentrum für die Förderung der Berufsbildung");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "Cedefop");
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "EUROFOUND");
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "Offizieller Name");
        Assert.assertEquals(project.columnModel.columns.get(6).getName(), "Anmerkungen");
        Assert.assertEquals(project.rows.get(0).cells.size(), 7);
    }

    @Test
    public void readTableWithSpanningCells() {
        // inspired from https://www.mediawiki.org/wiki/Help:Tables
        String input = "{| class=\"wikitable\"\n"
                + "!colspan=\"6\"|Shopping List\n"
                + "|-\n"
                + "|Bread & Butter\n"
                + "|Pie\n"
                + "|Buns\n"
                + "|rowspan=\"2\"|Danish\n"
                + "|colspan=\"2\"|Croissant\n"
                + "|-\n"
                + "|Cheese\n"
                + "|colspan=\"2\"|Ice cream\n"
                + "|Butter\n"
                + "|Yogurt\n"
                + "|}\n";

        try {
            prepareOptions(-1, 1, true, true, null, null);
            parse(input);
        } catch (Exception e) {
            Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 6);
        Assert.assertNull(project.rows.get(1).cells.get(2));
        Assert.assertNull(project.rows.get(1).cells.get(3));
        Assert.assertEquals(project.rows.get(1).cells.get(4).value, "Butter");
    }

    @Test
    public void readTableWithReferences() {
        // inspired from https://www.mediawiki.org/wiki/Help:Tables
        String input = "{|\n"
                + "! price\n"
                + "! fruit\n"
                + "! merchant\n"
                + "|-\n"
                + "| a || b <ref name=\"myref\"> See [http://gnu.org here]</ref>  || c <ref name=\"ms\"> or http://microsoft.com/ </ref>\n"
                + "|-\n"
                + "| d || e <ref name=\"ms\"/>|| f <ref name=\"myref\" />\n"
                + "|-\n"
                + "|}\n";

        try {
            prepareOptions(-1, 1, true, true, null, null);
            parse(input);
        } catch (Exception e) {
            Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 5);
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "b");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "http://gnu.org");
        Assert.assertEquals(project.rows.get(0).cells.get(4).value, "http://microsoft.com/");
        Assert.assertEquals(project.rows.get(1).cells.get(4).value, "http://gnu.org");
        Assert.assertEquals(project.rows.get(1).cells.get(2).value, "http://microsoft.com/");
    }

    @Test
    public void readTableWithReferencesTemplates() {
        // inspired from https://www.mediawiki.org/wiki/Help:Tables
        String input = "{|\n"
                + "! price\n"
                + "! fruit\n"
                + "! merchant\n"
                + "|-\n"
                + "| a || b <ref name=\"myref\">{{cite web|url=http://gnu.org|accessdate=2017-08-30}}</ref>  || c <ref name=\"ms\"> or {{cite journal|url=http://microsoft.com/|title=BLah}} </ref>\n"
                + "|-\n"
                + "| d || e <ref name=\"ms\"/>|| f <ref name=\"myref\" />\n"
                + "|-\n"
                + "|}\n";

        try {
            prepareOptions(-1, 1, true, true, null, null);
            parse(input);
        } catch (Exception e) {
            Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 5);
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "b");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "http://gnu.org");
        Assert.assertEquals(project.rows.get(0).cells.get(4).value, "http://microsoft.com/");
        Assert.assertEquals(project.rows.get(1).cells.get(4).value, "http://gnu.org");
        Assert.assertEquals(project.rows.get(1).cells.get(2).value, "http://microsoft.com/");
    }

    /**
     * Include templates and image filenames
     */
    @Test
    public void readTableWithTemplates() {
        String input = "\n"
                + "{|\n"
                + "|-\n"
                + "| {{free to read}} || b || c \n"
                + "|-\n"
                + "| d\n"
                + "| [[File:My logo.svg|70px]]\n"
                + "| f<br>\n"
                + "|-\n"
                + "|}\n";
        try {
            prepareOptions(0, 0, true, true, null, null);
            parse(input);
        } catch (Exception e) {
            Assert.fail("Parsing failed", e);
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "{{free to read}}");
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "[[File:My logo.svg]]");
    }

    // --helpers--

    private void parse(String wikitext) {
        parseOneFile(importer, new StringReader(wikitext));
    }

    private void prepareOptions(
            int limit, int headerLines, boolean blankSpanningCells,
            boolean guessValueType, String wikiUrl, String reconEndpoint) {

        whenGetIntegerOption("limit", options, limit);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
        whenGetBooleanOption("blankSpanningCells", options, blankSpanningCells);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
        whenGetBooleanOption("parseReferences", options, true);
        whenGetBooleanOption("includeRawTemplates", options, true);
        whenGetStringOption("wikiUrl", options, wikiUrl);
        whenGetStringOption("reconService", options, reconEndpoint);
    }
}
