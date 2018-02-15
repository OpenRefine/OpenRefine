/*
 * 
 * Copyright 2010, Google Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package com.google.refine.beam.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableRow;

import java.util.Arrays;
import java.util.List;

import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.ValidatesRunner;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AddColumnOperationTest {

  private static final String SEPARATOR_CHAR = ",";
  private static final String COL1_NAME = "col1";
  private static final String COL2_NAME = "col2";

  private static final String ADD_COLUMN_OP = "{\n" //
      + "    \"op\": \"core/column-addition\",\n"
      + "    \"description\": \"Create column test at index 2 based on column col2 using expression grel:value + \\\" test\\\"\",\n"
      + "    \"engineConfig\": {\n" + "      \"mode\": \"row-based\",\n" + "      \"facets\": []\n"
      + "    },\n" + "    \"newColumnName\": \"new\",\n" + "    \"columnInsertIndex\": 1,\n"
      + "    \"baseColumnName\": \"col2\",\n"
      + "    \"expression\": \"grel:value + \\\" test\\\"\",\n"
      + "    \"onError\": \"set-to-blank\"\n" //
      + "  }\n";

  private static final String[] IN_CSV_LINES = new String[] {"Matteo,Salvini", "Matteo,Renzi"};
  private static final String[] OUT_CSV_LINES =
      new String[] {"Matteo,Salvini test,Salvini", "Matteo,Renzi test,Renzi"};
  private static final ObjectMapper OBJ_MAPPER = new ObjectMapper();

  @Rule
  public TestPipeline p = TestPipeline.create();

  /**
   * Example test that tests the AddColumnFn transformation by using an in-memory input and
   * inspecting the output.
   */
  @Test
  @Category(ValidatesRunner.class)
  public void testAddColumnOp() throws Exception {

    final String[] inputColumns = new String[] {COL1_NAME, COL2_NAME};
    final List<String> strRows = Arrays.asList(IN_CSV_LINES);
    final JsonNode operationJson = OBJ_MAPPER.readTree(ADD_COLUMN_OP);
    final String srcColumnName = operationJson.get("baseColumnName").asText();
    final String newColumnName = operationJson.get("newColumnName").asText();
    final JsonNode facets = operationJson.get("engineConfig").get("facets");
    int newColumnPos = operationJson.get("columnInsertIndex").asInt();
    final String exp = operationJson.get("expression").asText();
    final String[] outpuColumns = getOutpuColumns(inputColumns, newColumnPos, newColumnName);

    PCollection<TableRow> input = p.apply(Create.of(strRows)).setCoder(StringUtf8Coder.of())
        .apply(ParDo.of(new CsvLineToTableRowFn()));

    PCollection<TableRow> transformedRows = input//
        .apply("Apply facets (filter)", ParDo.of(new ApplyFacetFilter(facets))) //
        .apply("Add Column", MapElements.via(new AddColumnFn(srcColumnName, newColumnName, exp)));

    PCollection<String> output = transformedRows//
        .apply("Row => CSV", MapElements.via(new RowToCsvLine(outpuColumns)));

    PAssert.that(output).containsInAnyOrder(OUT_CSV_LINES);

    p.run().waitUntilFinish();
  }


  private String[] getOutpuColumns(String[] inputColumns, int newColumnPos, String newColumnName) {
    String[] outputColumns = new String[inputColumns.length + 1];
    boolean found = false;
    for (int i = 0; i < inputColumns.length; i++) {
      if (i == newColumnPos) {
        outputColumns[i++] = newColumnName;
        found = true;
      }
      outputColumns[i] = inputColumns[found ? i - 1 : i];

    }
    return outputColumns;
  }

  
  @SuppressWarnings("serial")
  static class ApplyFacetFilter extends DoFn<TableRow, TableRow> {
    String facets;

    public ApplyFacetFilter(JsonNode facets) {
      this.facets = facets.asText();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
      TableRow row = c.element();
      if (facets == null || facets.isEmpty()) {
        c.output(row);
      } else {
        //TODO implement
      }
    }
  }

  @SuppressWarnings("serial")
  private static class RowToCsvLine extends SimpleFunction<TableRow, String> {

    private String[] outputColumns;

    RowToCsvLine(String[] outputColumns) {
      this.outputColumns = outputColumns;
    }

    @Override
    public String apply(TableRow row) {
      StringBuilder ret = new StringBuilder();
      for (int i = 0; i < outputColumns.length; i++) {
        ret.append(row.get(outputColumns[i]));
        if (i < outputColumns.length - 1) {
          ret.append(SEPARATOR_CHAR);
        }
      }
      return ret.toString();
    }
  }

  @SuppressWarnings("serial")
  private static class CsvLineToTableRowFn extends DoFn<String, TableRow> {

    @ProcessElement
    public void processElement(ProcessContext c) {
      final String[] split = c.element().split(SEPARATOR_CHAR);
      TableRow row = new TableRow()//
          .set(COL1_NAME, split[0])//
          .set(COL2_NAME, split[1]);
      c.output(row);
    }
  }



}
