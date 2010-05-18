package com.metaweb.gridworks.tests.importers;

import java.io.StringReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.metaweb.gridworks.importers.RdfTripleImporter;
import com.metaweb.gridworks.model.Project;


public class RdfTripleImporterTests {
    // logging
    final static protected Logger logger = LoggerFactory.getLogger("RdfImporterTests");

    //System Under Test
    RdfTripleImporter SUT = null;
    Project project = null;
    Properties options = null;

    @BeforeMethod
    public void SetUp(){
        SUT = new RdfTripleImporter();
        project = new Project();
        options = new Properties();
        options.put("base-url", "http://rdf.freebase.com");
    }

    @Test
    public void CanParseSingleLineTriple(){
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            SUT.read(reader, project, options);
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
    }

    @Test
    public void CanParseMultiLineTriple(){
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.under_the_red_sky>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.bringing_it_all_back_home>.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            SUT.read(reader, project, options);
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");
        Assert.assertEquals(project.rows.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "http://rdf.freebase.com/ns/en.bringing_it_all_back_home"); //NB triples aren't created in order they were input
        Assert.assertEquals(project.rows.get(2).cells.get(1).value, "http://rdf.freebase.com/ns/en.under_the_red_sky"); //NB triples aren't created in order they were input
    }

    @Test
    public void CanParseMultiLineMultiPredicatesTriple(){
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.genre> <http://rdf.freebase.com/ns/en.folk_rock>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.bringing_it_all_back_home>.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            SUT.read(reader, project, options);
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://rdf.freebase.com/ns/music.artist.genre");
        Assert.assertEquals(project.rows.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);//FIXME should the number of cells == 3?  should be updated if a column is added after the row is created
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
        Assert.assertEquals(project.rows.get(1).cells.size(), 2);//FIXME should the number of cells == 3?  should be updated if a column is added after the row is created
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "http://rdf.freebase.com/ns/en.bringing_it_all_back_home"); //NB triples aren't created in order they were input
        Assert.assertEquals(project.rows.get(2).cells.size(), 3);
        Assert.assertEquals(project.rows.get(2).cells.get(2).value, "http://rdf.freebase.com/ns/en.folk_rock"); //NB triples aren't created in order they were input
    }
}
