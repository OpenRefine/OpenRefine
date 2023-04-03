package com.google.refine.importers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.util.ParsingUtilities;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

public class SeparatorBasedImporterTests extends ImporterTest {

  @Override
  @BeforeTest
  public void init() {
    logger = LoggerFactory.getLogger(this.getClass());
  }

  // System Under Test
  SeparatorBasedImporter SUT = null;

  @Override
  @BeforeMethod
  public void setUp() {
    super.setUp();
    SUT = new SeparatorBasedImporter();
  }

  @Override
  @AfterMethod
  public void tearDown() {
    SUT = null;
    super.tearDown();
  }

  @Test
  public void testThatDefaultGuessIsATabSeparator() {
    ObjectNode options = SUT.createParserUIInitializationData(
            job, new LinkedList<>(), "text/json");
    assertEquals("\\t", options.get("separator").textValue());
  }

  @Test
  public void testThatSeparatorIsGuessedCorrectlyForCSV() throws IOException {
    List<ObjectNode> fileRecords = prepareFileRecords("food.small.csv");
    ObjectNode options = SUT.createParserUIInitializationData(
            job, fileRecords, "text/csv");
    assertEquals(",", options.get("separator").textValue());
  }

  @Test
  public void testThatSeparatorIsGuessedCorrectlyForTSV() throws IOException {
    List<ObjectNode> fileRecords = prepareFileRecords("movies-condensed.tsv");
    ObjectNode options = SUT.createParserUIInitializationData(
            job, fileRecords, "text/tsv");
    assertEquals("\\t", options.get("separator").textValue());
  }

  @Test
  public void testThatSeparatorIsGuessedCorrectlyForJSON() throws IOException {
    List<ObjectNode> fileRecords = prepareFileRecords("grid_small.json");
    ObjectNode options = SUT.createParserUIInitializationData(
            job, fileRecords, "text/json");
    assertEquals("\\t", options.get("separator").textValue());
  }

  // ------------helper methods---------------

  private List<ObjectNode> prepareFileRecords(final String FILE) throws IOException {
    String filename = ClassLoader.getSystemResource(FILE).getPath();
    // File is assumed to be in job.getRawDataDir(), so copy it there
    FileUtils.copyFile(new File(filename), new File(job.getRawDataDir(), FILE));
    List<ObjectNode> fileRecords = new ArrayList<>();
    fileRecords.add(ParsingUtilities.evaluateJsonStringToObjectNode(
            String.format("{\"location\": \"%s\"}", FILE)));
    return fileRecords;
  }
}
