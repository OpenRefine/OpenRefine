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

package org.openrefine.importers;

import java.io.StringReader;

import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.util.JSONUtilities;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class RdfTripleImporterTests extends ImporterTest {
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    
    //System Under Test
    RdfTripleImporter SUT = null;

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
        SUT = new RdfTripleImporter(runner());
        JSONUtilities.safePut(options, "base-url", "http://rdf.freebase.com");
    }

    @Test(enabled=false)
    public void canParseSingleLineTriple() throws Exception{
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.";
        StringReader reader = new StringReader(sampleRdf);

        GridState grid = parseOneFile(SUT, reader);
        
        ColumnModel columnModel = grid.getColumnModel();
            
        Assert.assertEquals(columnModel.getColumns().size(), 2);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");
        Assert.assertEquals(grid.rowCount(), 1);
        Assert.assertEquals(grid.getRow(0).cells.size(), 2);
        Assert.assertEquals(grid.getRow(0).getCell(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(grid.getRow(0).getCell(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
    }

    @Test
    public void canParseMultiLineTriple() throws Exception {
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.under_the_red_sky>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.bringing_it_all_back_home>.";
        StringReader input = new StringReader(sampleRdf);
        GridState grid = parseOneFile(SUT, input);

        ColumnModel columnModel = grid.getColumnModel();
        //columns
        Assert.assertEquals(columnModel.getColumns().size(), 2);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");

        //rows
        Assert.assertEquals(grid.rowCount(), 3);
        
        //row0
        Assert.assertEquals(grid.getRow(0).cells.size(), 2);
        Assert.assertEquals(grid.getRow(0).getCellValue(0), "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(grid.getRow(0).getCellValue(1), "http://rdf.freebase.com/ns/en.bringing_it_all_back_home"); 

        //row1
        Assert.assertEquals(grid.getRow(1).cells.size(), 2);
        Assert.assertNull(grid.getRow(1).getCell(0));
        Assert.assertEquals(grid.getRow(1).getCellValue(1), "http://rdf.freebase.com/ns/en.under_the_red_sky");

        //row2
        Assert.assertEquals(grid.getRow(2).cells.size(), 2);
        Assert.assertNull(grid.getRow(2).getCell(0));
        Assert.assertEquals(grid.getRow(2).getCellValue(1), "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
    }

    @Test
    public void canParseMultiLineMultiPredicatesTriple() throws Exception {
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.genre> <http://rdf.freebase.com/ns/en.folk_rock>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.bringing_it_all_back_home>.";
        StringReader input = new StringReader(sampleRdf);
        GridState grid = parseOneFile(SUT, input);
 
        ColumnModel columnModel = grid.getColumnModel();
        //columns
        Assert.assertEquals(columnModel.getColumns().size(), 3);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "http://rdf.freebase.com/ns/music.artist.genre");
        
        //rows
        Assert.assertEquals(grid.rowCount(), 2);

        //row0
        Assert.assertEquals(grid.getRow(0).cells.size(), 3);
        Assert.assertEquals(grid.getRow(0).getCellValue(0), "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(grid.getRow(0).getCellValue(1), "http://rdf.freebase.com/ns/en.bringing_it_all_back_home");
        Assert.assertEquals(grid.getRow(0).getCellValue(2), "http://rdf.freebase.com/ns/en.folk_rock");

        //row1
        Assert.assertEquals(grid.getRow(1).cells.size(), 3);
        Assert.assertNull(grid.getRow(1).getCell(0));
        Assert.assertEquals(grid.getRow(1).getCell(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
    }
    
    @Test
    public void canParseTripleWithValue() throws Exception {
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/common.topic.alias>\"Robert Zimmerman\"@en.";
        StringReader input = new StringReader(sampleRdf);
        
        SUT = new RdfTripleImporter(runner(), RdfTripleImporter.Mode.N3);
        GridState grid = parseOneFile(SUT, input);

        ColumnModel columnModel = grid.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().size(), 2);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://rdf.freebase.com/ns/common.topic.alias");
        Assert.assertEquals(grid.rowCount(), 1);
        Assert.assertEquals(grid.getRow(0).cells.size(), 2);
        Assert.assertEquals(grid.getRow(0).getCellValue(0), "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(grid.getRow(0).getCellValue(1), "Robert Zimmerman@en");
    }    
    
    @Test
    public void canParseRdfXml() throws Exception {
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

        StringReader input = new StringReader(sampleRdf);
        SUT = new RdfTripleImporter(runner(), RdfTripleImporter.Mode.RDFXML);
        GridState grid = parseOneFile(SUT, input);

        ColumnModel columnModel = grid.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().size(), 3);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://purl.org/dc/elements/1.1/title");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "http://purl.org/dc/elements/1.1/description");
        Assert.assertEquals(grid.rowCount(), 5);
        Assert.assertEquals(grid.getRow(0).cells.size(), 3);
        Assert.assertEquals(grid.getRow(0).getCellValue(0), "http://www.w3.org/TR/rdf-syntax-grammar");
        Assert.assertEquals(grid.getRow(0).getCellValue(1), "RDF/XML Syntax Specification (Revised)@en-US");
        Assert.assertEquals(grid.getRow(3).cells.size(), 3);
        Assert.assertEquals(grid.getRow(3).getCellValue(0), "http://example.org/buecher/baum");
        Assert.assertEquals(grid.getRow(3).getCellValue(1), "The Tree@en");
        Assert.assertEquals(grid.getRow(3).getCellValue(2), "Das Buch ist außergewöhnlich@de");
    }
    
    @Test
    public void canParseN3() throws Exception {
        String sampleRdf = "@prefix p:  <http://www.example.org/personal_details#> .\n" + 
            "@prefix m:  <http://www.example.org/meeting_organization#> .\n\n" + 
            "<http://www.example.org/people#fred>\n" + 
                    "p:GivenName     \"Fred\";\n" + 
                    "p:hasEmail              <mailto:fred@example.com>;\n" + 
                    "m:attending     <http://meetings.example.com/cal#m1> .\n";
                        
        StringReader input = new StringReader(sampleRdf);
        
        SUT = new RdfTripleImporter(runner(), RdfTripleImporter.Mode.N3);
        GridState grid = parseOneFile(SUT, input);

        ColumnModel columnModel = grid.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().size(), 4);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://www.example.org/meeting_organization#attending");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(columnModel.getColumns().get(3).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(grid.rowCount(), 1);
        Assert.assertEquals(grid.getRow(0).cells.size(), 4);
        Assert.assertEquals(grid.getRow(0).getCellValue(0), "http://www.example.org/people#fred");
        Assert.assertEquals(grid.getRow(0).getCellValue(1), "http://meetings.example.com/cal#m1");
        Assert.assertEquals(grid.getRow(0).getCellValue(2), "mailto:fred@example.com");
        Assert.assertEquals(grid.getRow(0).getCellValue(3), "Fred");
    }
    
    @Test
    public void canParseTtl() throws Exception {
        String sampleRdf = "@prefix p:  <http://www.example.org/personal_details#> .\n" + 
            "@prefix m:  <http://www.example.org/meeting_organization#> .\n\n" + 
            "<http://www.example.org/people#fred>\n" + 
                    "p:GivenName     \"Fred\";\n" + 
                    "p:hasEmail              <mailto:fred@example.com>;\n" + 
                    "m:attending     <http://meetings.example.com/cal#m1> .\n";
                        
        StringReader input = new StringReader(sampleRdf);
        
        SUT = new RdfTripleImporter(runner(), RdfTripleImporter.Mode.TTL);
        GridState grid = parseOneFile(SUT, input);

        ColumnModel columnModel = grid.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().size(), 4);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://www.example.org/meeting_organization#attending");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(columnModel.getColumns().get(3).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(grid.rowCount(), 1);
        Assert.assertEquals(grid.getRow(0).cells.size(), 4);
        Assert.assertEquals(grid.getRow(0).getCellValue(0), "http://www.example.org/people#fred");
        Assert.assertEquals(grid.getRow(0).getCellValue(1), "http://meetings.example.com/cal#m1");
        Assert.assertEquals(grid.getRow(0).getCellValue(2), "mailto:fred@example.com");
        Assert.assertEquals(grid.getRow(0).getCellValue(3), "Fred");
    }
    
    @Test
    public void canParseNTriples() throws Exception {
        String sampleRdf = "<http://www.example.org/people#fred> <http://www.example.org/meeting_organization#attending> <http://meetings.example.com/cal#m1> . \n" +
                           "<http://www.example.org/people#fred> <http://www.example.org/personal_details#hasEmail> <mailto:fred@example.com> . \n" +
                           "<http://www.example.org/people#fred> <http://www.example.org/personal_details#GivenName> \"Fred\" . ";
                        
        StringReader input = new StringReader(sampleRdf);
        
        SUT = new RdfTripleImporter(runner(), RdfTripleImporter.Mode.NT);
        GridState grid = parseOneFile(SUT, input);

        ColumnModel columnModel = grid.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().size(), 4);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(columnModel.getColumns().get(3).getName(), "http://www.example.org/meeting_organization#attending");
        
        Assert.assertEquals(grid.rowCount(), 1);
        Assert.assertEquals(grid.getRow(0).cells.size(), 4);
        Assert.assertEquals(grid.getRow(0).getCell(0).value, "http://www.example.org/people#fred");
        Assert.assertEquals(grid.getRow(0).getCell(1).value, "Fred");
        Assert.assertEquals(grid.getRow(0).getCell(2).value, "mailto:fred@example.com");
        Assert.assertEquals(grid.getRow(0).getCell(3).value, "http://meetings.example.com/cal#m1");
    }
    
    @Test
    public void canParseJsonld() throws Exception {
        String sampleJsonld = "{\n "+
        "  \"@context\": {\n "+
        "    \"m\": \"http://www.example.org/meeting_organization#\",\n "+
        "    \"p\": \"http://www.example.org/personal_details#\",\n "+
        "    \"rdf\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\n "+
        "    \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n "+
        "    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\"\n "+
        "  },\n "+
        "  \"@id\": \"http://www.example.org/people#fred\",\n "+
        "  \"m:attending\": {\n "+
        "    \"@id\": \"http://meetings.example.com/cal#m1\"\n "+
        "  },\n "+
        "  \"p:GivenName\": \"Fred\",\n "+
        "  \"p:hasEmail\": {\n "+
        "    \"@id\": \"mailto:fred@example.com\"\n "+
        "  }\n "+
        "}";
                        
        StringReader input = new StringReader(sampleJsonld);
        
        SUT = new RdfTripleImporter(runner(), RdfTripleImporter.Mode.JSONLD);
        GridState grid = parseOneFile(SUT, input);

        ColumnModel columnModel = grid.getColumnModel();
        Assert.assertEquals(columnModel.getColumns().size(), 4);
        Assert.assertEquals(columnModel.getColumns().get(0).getName(), "subject");
        Assert.assertEquals(columnModel.getColumns().get(1).getName(), "http://www.example.org/personal_details#hasEmail");
        Assert.assertEquals(columnModel.getColumns().get(2).getName(), "http://www.example.org/personal_details#GivenName");
        Assert.assertEquals(columnModel.getColumns().get(3).getName(), "http://www.example.org/meeting_organization#attending");
        Assert.assertEquals(grid.rowCount(), 1);
        Assert.assertEquals(grid.getRow(0).cells.size(), 4);
        Assert.assertEquals(grid.getRow(0).getCellValue(0), "http://www.example.org/people#fred");
        Assert.assertEquals(grid.getRow(0).getCellValue(1), "mailto:fred@example.com");
        Assert.assertEquals(grid.getRow(0).getCellValue(2), "Fred");
        Assert.assertEquals(grid.getRow(0).getCellValue(3), "http://meetings.example.com/cal#m1");
    } 
}
