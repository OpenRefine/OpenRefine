/*

Copyright 2010,2012 Google Inc.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.util.JSONUtilities;

public class RdfTripleImporterTests extends ImporterTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System Under Test
    RdfTripleImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new RdfTripleImporter();
        JSONUtilities.safePut(options, "base-url", "http://rdf.mybase.com");
    }

    @Test(enabled = false)
    public void canParseSingleLineTriple() {
        String sampleRdf = "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/music.artist.album> <http://rdf.mybase.com/ns/en.blood_on_the_tracks>.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            parseOneFile(SUT, reader);
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.mybase.com/ns/music.artist.album");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.mybase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.mybase.com/ns/en.blood_on_the_tracks");
    }

    @Test
    public void canParseMultiLineTriple() throws UnsupportedEncodingException {
        String sampleRdf = "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/music.artist.album> <http://rdf.mybase.com/ns/en.blood_on_the_tracks>.\n"
                +
                "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/music.artist.album> <http://rdf.mybase.com/ns/en.under_the_red_sky>.\n"
                +
                "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/music.artist.album> <http://rdf.mybase.com/ns/en.bringing_it_all_back_home>.";
        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));
        parseOneFile(SUT, input);

        // columns
        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.mybase.com/ns/music.artist.album");

        // rows
        Assert.assertEquals(project.rows.size(), 3);

        // row0
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.mybase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.mybase.com/ns/en.bringing_it_all_back_home");

        // row1
        Assert.assertEquals(project.rows.get(1).cells.size(), 2);
        Assert.assertNull(project.rows.get(1).cells.get(0));
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "http://rdf.mybase.com/ns/en.under_the_red_sky");
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].rowIndex, 0);
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].cellIndex, 0);

        // row2
        Assert.assertEquals(project.rows.get(2).cells.size(), 2);
        Assert.assertNull(project.rows.get(2).cells.get(0));
        Assert.assertEquals(project.rows.get(2).cells.get(1).value, "http://rdf.mybase.com/ns/en.blood_on_the_tracks");
        Assert.assertEquals(project.recordModel.getRowDependency(2).cellDependencies[1].rowIndex, 0);
        Assert.assertEquals(project.recordModel.getRowDependency(2).cellDependencies[1].cellIndex, 0);
    }

    @Test
    public void canParseMultiLineMultiPredicatesTriple() throws UnsupportedEncodingException {
        String sampleRdf = "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/music.artist.album> <http://rdf.mybase.com/ns/en.blood_on_the_tracks>.\n"
                +
                "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/music.artist.genre> <http://rdf.mybase.com/ns/en.folk_rock>.\n"
                +
                "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/music.artist.album> <http://rdf.mybase.com/ns/en.bringing_it_all_back_home>.";
        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));
        parseOneFile(SUT, input);

        // columns
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.mybase.com/ns/music.artist.album");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://rdf.mybase.com/ns/music.artist.genre");

        // rows
        Assert.assertEquals(project.rows.size(), 2);

        // row0
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.mybase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.mybase.com/ns/en.bringing_it_all_back_home");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "http://rdf.mybase.com/ns/en.folk_rock");

        // row1
        Assert.assertEquals(project.rows.get(1).cells.size(), 2);
        Assert.assertNull(project.rows.get(1).cells.get(0));
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "http://rdf.mybase.com/ns/en.blood_on_the_tracks");
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].rowIndex, 0);
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].cellIndex, 0);
    }

    @Test
    public void canParseTripleWithValue() throws UnsupportedEncodingException {
        String sampleRdf = "<http://rdf.mybase.com/ns/en.bob_dylan> <http://rdf.mybase.com/ns/common.topic.alias>\"Robert Zimmerman\"@en.";
        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));

        SUT = new RdfTripleImporter(RdfTripleImporter.Mode.N3);
        parseOneFile(SUT, input);

        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.mybase.com/ns/common.topic.alias");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.mybase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "Robert Zimmerman@en");
    }

    @Test
    public void canParseRdfXml() throws UnsupportedEncodingException {
        // From W3C spec http://www.w3.org/TR/REC-rdf-syntax/#example8
        String sampleRdf = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
                + "         xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
                + "  <rdf:Description rdf:about=\"http://www.w3.org/TR/rdf-syntax-grammar\">\n"
                + "    <dc:title>RDF/XML Syntax Specification (Revised)</dc:title>\n"
                + "    <dc:title xml:lang=\"en\">RDF/XML Syntax Specification (Revised)</dc:title>\n"
                + "    <dc:title xml:lang=\"en-US\">RDF/XML Syntax Specification (Revised)</dc:title>\n"
                + "  </rdf:Description>\n"
                + "\n"
                + "  <rdf:Description rdf:about=\"http://example.org/buecher/baum\" xml:lang=\"de\">\n"
                + "    <dc:title>Der Baum</dc:title>\n"
                + "    <dc:description>Das Buch ist außergewöhnlich</dc:description>\n"
                + "    <dc:title xml:lang=\"en\">The Tree</dc:title>\n"
                + "  </rdf:Description>\n"
                + "</rdf:RDF>\n";

        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));
        SUT = new RdfTripleImporter(RdfTripleImporter.Mode.RDFXML);
        parseOneFile(SUT, input);

        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://purl.org/dc/elements/1.1/title");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://purl.org/dc/elements/1.1/description");
        Assert.assertEquals(project.rows.size(), 5);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://www.w3.org/TR/rdf-syntax-grammar");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "RDF/XML Syntax Specification (Revised)@en-US");
        Assert.assertEquals(project.rows.get(3).cells.size(), 3);
        Assert.assertEquals(project.rows.get(3).cells.get(0).value, "http://example.org/buecher/baum");
        Assert.assertEquals(project.rows.get(3).cells.get(1).value, "The Tree@en");
        Assert.assertEquals(project.rows.get(3).cells.get(2).value, "Das Buch ist außergewöhnlich@de");
    }

    @Test
    public void canParseN3() throws UnsupportedEncodingException {
        String sampleRdf = "@prefix p:  <http://www.example.org/personal_details#> .\n" +
                "@prefix m:  <http://www.example.org/meeting_organization#> .\n\n" +
                "<http://www.example.org/people#fred>\n" +
                "p:GivenName     \"Fred\";\n" +
                "p:hasEmail              <mailto:fred@example.com>;\n" +
                "m:attending     <http://meetings.example.com/cal#m1> .\n";

        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));

        SUT = new RdfTripleImporter(RdfTripleImporter.Mode.N3);
        parseOneFile(SUT, input);

        Assert.assertEquals(project.columnModel.columns.size(), 4);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://www.example.org/meeting_organization#attending");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://www.example.org/people#fred");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://meetings.example.com/cal#m1");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "mailto:fred@example.com");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "Fred");
    }

    @Test
    public void canParseTtl() throws UnsupportedEncodingException {
        String sampleRdf = "@prefix p:  <http://www.example.org/personal_details#> .\n" +
                "@prefix m:  <http://www.example.org/meeting_organization#> .\n\n" +
                "<http://www.example.org/people#fred>\n" +
                "p:GivenName     \"Fred\";\n" +
                "p:hasEmail              <mailto:fred@example.com>;\n" +
                "m:attending     <http://meetings.example.com/cal#m1> .\n";

        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));

        SUT = new RdfTripleImporter(RdfTripleImporter.Mode.TTL);
        parseOneFile(SUT, input);

        Assert.assertEquals(project.columnModel.columns.size(), 4);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://www.example.org/meeting_organization#attending");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://www.example.org/people#fred");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://meetings.example.com/cal#m1");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "mailto:fred@example.com");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "Fred");
    }

    @Test
    public void canParseNTriples() throws UnsupportedEncodingException {
        String sampleRdf = "<http://www.example.org/people#fred> <http://www.example.org/meeting_organization#attending> <http://meetings.example.com/cal#m1> . \n"
                +
                "<http://www.example.org/people#fred> <http://www.example.org/personal_details#hasEmail> <mailto:fred@example.com> . \n" +
                "<http://www.example.org/people#fred> <http://www.example.org/personal_details#GivenName> \"Fred\" . ";

        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));

        SUT = new RdfTripleImporter(RdfTripleImporter.Mode.NT);
        parseOneFile(SUT, input);

        Assert.assertEquals(project.columnModel.columns.size(), 4);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "http://www.example.org/meeting_organization#attending");

        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://www.example.org/people#fred");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "Fred");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "mailto:fred@example.com");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "http://meetings.example.com/cal#m1");
    }

    @Test
    public void canParseTurtleBlankNode() throws UnsupportedEncodingException {
        String sampleRdf = "@prefix ex: <http://example.org/data#> .\n" +
                "<http://example.org/web-data> ex:title \"Web Data\" ;\n" +
                "                               ex:professor [ ex:fullName \"Alice Carol\" ;\n" +
                "                                              ex:homePage <http://example.net/alice-carol> ] .";

        InputStream input = new ByteArrayInputStream(sampleRdf.getBytes("UTF-8"));

        SUT = new RdfTripleImporter(RdfTripleImporter.Mode.TTL);
        parseOneFile(SUT, input);

        String[] columns = { "subject",
                "http://example.org/data#professor",
                "http://example.org/data#title",
                "http://example.org/data#homePage",
                "http://example.org/data#fullName",
        };

        Assert.assertEquals(project.columnModel.columns.size(), columns.length);
        for (int i = 0; i < columns.length; i++) {
            Assert.assertEquals(project.columnModel.columns.get(i).getName(), columns[i]);
        }

        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(1).cells.size(), 5);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://example.org/web-data");
        // Generated blank node ID is random, but should match
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, project.rows.get(1).cells.get(0).value);
    }

    @Test
    public void canParseJsonld() throws UnsupportedEncodingException {
        String sampleJsonld = "{\n " +
                "  \"@context\": {\n " +
                "    \"m\": \"http://www.example.org/meeting_organization#\",\n " +
                "    \"p\": \"http://www.example.org/personal_details#\",\n " +
                "    \"rdf\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\n " +
                "    \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n " +
                "    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\"\n " +
                "  },\n " +
                "  \"@id\": \"http://www.example.org/people#fred\",\n " +
                "  \"m:attending\": {\n " +
                "    \"@id\": \"http://meetings.example.com/cal#m1\"\n " +
                "  },\n " +
                "  \"p:GivenName\": \"Fred\",\n " +
                "  \"p:hasEmail\": {\n " +
                "    \"@id\": \"mailto:fred@example.com\"\n " +
                "  }\n " +
                "}";

        InputStream input = new ByteArrayInputStream(sampleJsonld.getBytes("UTF-8"));

        SUT = new RdfTripleImporter(RdfTripleImporter.Mode.JSONLD);
        parseOneFile(SUT, input);

        Assert.assertEquals(project.columnModel.columns.size(), 4);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "http://www.example.org/meeting_organization#attending");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://www.example.org/people#fred");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "mailto:fred@example.com");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "Fred");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "http://meetings.example.com/cal#m1");
    }
}
