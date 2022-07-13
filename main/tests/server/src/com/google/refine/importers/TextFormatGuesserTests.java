/*

Copyright 2020 OpenRefine committers
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

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.io.PatternFilenameFilter;
import com.google.refine.importing.FormatGuesser;

public class TextFormatGuesserTests extends ImporterTest {

    FormatGuesser guesser;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        guesser = new TextFormatGuesser();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        guesser = null;
        super.tearDown();
    }

    @Test
    public void xlsTextGuessTest() throws FileNotFoundException, IOException {
        // Test an XLSX file without the correct file extension
        String dir = ClassLoader.getSystemResource("Colorado-Municipalities-small-xlsx.gz").getPath();
        InputStream is = new GZIPInputStream(new FileInputStream(new File(dir)));
        File tmp = File.createTempFile("openrefinetests-textguesser", "");
        FileUtils.copyInputStreamToFile(is, tmp);
        String format = guesser.guess(tmp, "UTF-8", "text");
        assertEquals(format, "binary");
    }

    @Test
    public void csvGuesserTest() {
        extensionGuesserTests("csv", "text/line-based");
    }

    @Test
    public void tsvGuesserTest() {
        extensionGuesserTests("tsv", "text/line-based");
    }

    @Test(enabled = false) // FIXME: Our JSON guesser doesn't work on small files
    public void jsonGuesserTest() {
        extensionGuesserTests("json", "text/json");
    }

    @Test
    public void xmlGuesserTest() {
        extensionGuesserTests("xml", "text/xml");
    }

    private void extensionGuesserTests(String extension, String expectedFormat) {
        String dir = ClassLoader.getSystemResource("food.csv").getPath();
        dir = dir.substring(0, dir.lastIndexOf('/'));
        File testDataDir = new File(dir);
        for (String testFile : testDataDir.list(new PatternFilenameFilter(".+\\." + extension))) {
            String format = guesser.guess(new File(dir, testFile), "UTF-8", "text");
            assertEquals(format, expectedFormat, "Format guess failed for " + testFile);
        }
    }

    @Test
    public void guessWikiTable() throws IOException {
        String input = "\n"
                + "{|\n"
                + "|-\n"
                + "| a || b<br/>2 || c \n"
                + "|-\n"
                + "| d || e || f<br>\n"
                + "|-\n"
                + "|}\n";
        testWikiTableString(input);
    }

    private void testWikiTableString(String input) throws IOException, FileNotFoundException {
        File tmp = File.createTempFile("openrefinetests-textguesser", "");
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(tmp),
                Charset.forName("UTF-8").newEncoder());
        writer.write(input);
        writer.close();
        String format = guesser.guess(tmp, "UTF-8", "text");
        assertEquals(format, "text/wiki");
    }

    @Test
    public void guessTableWithMisplacedHeaders() throws FileNotFoundException, IOException {
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
        testWikiTableString(input);
    }

    @Test
    public void guessTableWithLinks() throws FileNotFoundException, IOException {

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
        testWikiTableString(input);
    }

    @Test
    public void readStyledTableWithHeader() throws FileNotFoundException, IOException {
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
        testWikiTableString(input);
    }

    @Test
    public void guessTableWithSpanningCells() throws FileNotFoundException, IOException {
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
        testWikiTableString(input);
    }

    @Test
    public void guessTableWithReferences() throws FileNotFoundException, IOException {
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
        testWikiTableString(input);
    }

    @Test
    public void guessTableWithReferencesTemplates() throws FileNotFoundException, IOException {
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
        testWikiTableString(input);
    }

    @Test
    public void guessTableWithTemplates() throws FileNotFoundException, IOException {
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
        testWikiTableString(input);
    }

}
