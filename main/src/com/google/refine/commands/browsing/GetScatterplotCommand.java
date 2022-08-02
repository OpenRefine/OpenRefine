/*

Copyright 2010, Google Inc.
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

package com.google.refine.commands.browsing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.facets.ScatterplotDrawingRowVisitor;
import com.google.refine.browsing.facets.ScatterplotFacet;
import com.google.refine.browsing.util.NumericBinIndex;
import com.google.refine.commands.Command;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class GetScatterplotCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("get-scatterplot_command");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            long start = System.currentTimeMillis();

            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            PlotterConfig conf = ParsingUtilities.mapper.readValue(
                    request.getParameter("plotter"),
                    PlotterConfig.class);

            response.setHeader("Content-Type", "image/png");

            ServletOutputStream sos = null;

            try {
                sos = response.getOutputStream();
                draw(sos, project, engine, conf);
            } finally {
                sos.close();
            }

            logger.trace("Drawn scatterplot in {} ms", Long.toString(System.currentTimeMillis() - start));
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }

    protected static class PlotterConfig {

        @JsonProperty(ScatterplotFacet.SIZE)
        public int size = 100;
        @JsonProperty(ScatterplotFacet.DOT)
        double dot = 100;
        @JsonIgnore
        public int dim_x = ScatterplotFacet.LIN;
        @JsonIgnore
        public int dim_y = ScatterplotFacet.LIN;
        @JsonProperty(ScatterplotFacet.ROTATION)
        public int rotation = ScatterplotFacet.NO_ROTATION;
        @JsonProperty(ScatterplotFacet.COLOR)
        public String color_str = "000000";
        @JsonProperty(ScatterplotFacet.BASE_COLOR)
        public String base_color_str = null;
        @JsonProperty(ScatterplotFacet.X_COLUMN_NAME)
        public String columnName_x = "";
        @JsonProperty(ScatterplotFacet.X_EXPRESSION)
        public String expression_x = "value";
        @JsonProperty(ScatterplotFacet.Y_COLUMN_NAME)
        public String columnName_y = "";
        @JsonProperty(ScatterplotFacet.Y_EXPRESSION)
        public String expression_y = "value";

        @JsonProperty(ScatterplotFacet.DIM_X)
        public String getDimX() {
            return dim_x == ScatterplotFacet.LIN ? "lin" : "log";
        }

        @JsonProperty(ScatterplotFacet.DIM_Y)
        public String getDimY() {
            return dim_y == ScatterplotFacet.LIN ? "lin" : "log";
        }

        @JsonProperty(ScatterplotFacet.DIM_X)
        public void setDimX(String dim) {
            dim_x = dim.equals("lin") ? ScatterplotFacet.LIN : ScatterplotFacet.LOG;
        }

        @JsonProperty(ScatterplotFacet.DIM_Y)
        public void setDimY(String dim) {
            dim_y = dim.equals("lin") ? ScatterplotFacet.LIN : ScatterplotFacet.LOG;
        }

        // rotation can be set to "none" (a JSON string) in which case it should be ignored
        @JsonProperty(ScatterplotFacet.ROTATION)
        public void setRotation(Object rotation) {
            this.rotation = ScatterplotFacet.ScatterplotFacetConfig.getRotation(rotation.toString());
        }
    }

    public void draw(OutputStream output, Project project, Engine engine, PlotterConfig o) throws IOException {

        double min_x = 0;
        double min_y = 0;
        double max_x = 0;
        double max_y = 0;

        int columnIndex_x = 0;
        int columnIndex_y = 0;

        Evaluable eval_x = null;
        Evaluable eval_y = null;

        Color color = new Color(Integer.parseInt(o.color_str, 16));

        Color base_color = o.base_color_str != null ? new Color(Integer.parseInt(o.base_color_str, 16)) : null;

        if (o.columnName_x.length() > 0) {
            Column x_column = project.columnModel.getColumnByName(o.columnName_x);
            if (x_column != null) {
                columnIndex_x = x_column.getCellIndex();
            }
        } else {
            columnIndex_x = -1;
        }

        try {
            eval_x = MetaParser.parse(o.expression_x);
        } catch (ParsingException e) {
            logger.warn("error parsing expression", e);
        }

        if (o.columnName_y.length() > 0) {
            Column y_column = project.columnModel.getColumnByName(o.columnName_y);
            if (y_column != null) {
                columnIndex_y = y_column.getCellIndex();
            }
        } else {
            columnIndex_y = -1;
        }

        try {
            eval_y = MetaParser.parse(o.expression_y);
        } catch (ParsingException e) {
            logger.warn("error parsing expression", e);
        }

        NumericBinIndex index_x = null;
        NumericBinIndex index_y = null;

        Column column_x = project.columnModel.getColumnByName(o.columnName_x);
        if (column_x != null) {
            columnIndex_x = column_x.getCellIndex();
            index_x = ScatterplotFacet.getBinIndex(project, column_x, eval_x, o.expression_x);
            min_x = index_x.getMin();
            max_x = index_x.getMax();
        }

        Column column_y = project.columnModel.getColumnByName(o.columnName_y);
        if (column_y != null) {
            columnIndex_y = column_y.getCellIndex();
            index_y = ScatterplotFacet.getBinIndex(project, column_y, eval_y, o.expression_y);
            min_y = index_y.getMin();
            max_y = index_y.getMax();
        }

        if (index_x != null && index_y != null && index_x.isNumeric() && index_y.isNumeric()) {
            ScatterplotDrawingRowVisitor drawer = new ScatterplotDrawingRowVisitor(
                    columnIndex_x, columnIndex_y, min_x, max_x, min_y, max_y,
                    o.size, o.dim_x, o.dim_y, o.rotation, o.dot, color);

            if (base_color != null) {
                drawer.setColor(base_color);

                FilteredRows filteredRows = engine.getAllRows();
                filteredRows.accept(project, drawer);

                drawer.setColor(color);
            }

            {
                FilteredRows filteredRows = engine.getAllFilteredRows();
                filteredRows.accept(project, drawer);
            }

            ImageIO.write(drawer.getImage(), "png", output);
        } else {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), "png", output);
        }

    }

}
