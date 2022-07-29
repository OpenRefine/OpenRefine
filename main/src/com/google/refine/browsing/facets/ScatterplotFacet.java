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

package com.google.refine.browsing.facets;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.DualExpressionsNumberComparisonRowFilter;
import com.google.refine.browsing.util.ExpressionBasedRowEvaluable;
import com.google.refine.browsing.util.NumericBinIndex;
import com.google.refine.browsing.util.NumericBinRecordIndex;
import com.google.refine.browsing.util.NumericBinRowIndex;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public class ScatterplotFacet implements Facet {

    public static final int LIN = 0;
    public static final int LOG = 1;

    public static final int NO_ROTATION = 0;
    public static final int ROTATE_CW = 1;
    public static final int ROTATE_CCW = 2;

    /*
     * Configuration, from the client side
     */
    public static class ScatterplotFacetConfig implements FacetConfig {

        @JsonProperty("name")
        protected String name; // name of facet

        @JsonProperty(X_EXPRESSION)
        protected String expression_x; // expression to compute the x numeric value(s) per row
        @JsonProperty(Y_EXPRESSION)
        protected String expression_y; // expression to compute the y numeric value(s) per row
        @JsonProperty(X_COLUMN_NAME)
        protected String columnName_x; // column to base the x expression on, if any
        @JsonProperty(Y_COLUMN_NAME)
        protected String columnName_y; // column to base the y expression on, if any

        @JsonProperty(SIZE)
        protected int size;
        @JsonIgnore
        protected int dim_x;
        @JsonIgnore
        protected int dim_y;
        @JsonIgnore
        protected String rotation_str;
        @JsonIgnore
        protected int rotation;

        @JsonIgnore
        protected double l = 1.;
        @JsonProperty(DOT)
        protected double dot;

        @JsonIgnore
        protected String color_str = "000000";

        @JsonIgnore
        protected Color getColor() {
            return new Color(Integer.parseInt(color_str, 16));
        }

        @JsonProperty(FROM_X)
        protected double from_x; // the numeric selection for the x axis, from 0 to 1
        @JsonProperty(TO_X)
        protected double to_x;
        @JsonProperty(FROM_Y)
        protected double from_y; // the numeric selection for the y axis, from 0 to 1
        @JsonProperty(TO_Y)
        protected double to_y;

        // false if we're certain that all rows will match
        // and there isn't any filtering to do
        protected boolean isSelected() {
            return from_x > 0 || to_x < 1 || from_y > 0 || to_y < 1;
        }

        @JsonProperty(DIM_X)
        public String getDimX() {
            return dim_x == LIN ? "lin" : "log";
        }

        @JsonProperty(DIM_Y)
        public String getDimY() {
            return dim_y == LIN ? "lin" : "log";
        }

        @Override
        public ScatterplotFacet apply(Project project) {
            ScatterplotFacet facet = new ScatterplotFacet();
            facet.initializeFromConfig(this, project);
            return facet;
        }

        public static int getRotation(String rotation) {
            rotation = rotation.toLowerCase();
            if ("cw".equals(rotation) || "right".equals(rotation)) {
                return ScatterplotFacet.ROTATE_CW;
            } else if ("ccw".equals(rotation) || "left".equals(rotation)) {
                return ScatterplotFacet.ROTATE_CCW;
            } else {
                return NO_ROTATION;
            }
        }

        @Override
        public String getJsonType() {
            return "scatterplot";
        }
    }

    ScatterplotFacetConfig config;

    /*
     * Derived configuration data
     */
    protected int columnIndex_x;
    protected int columnIndex_y;
    protected Evaluable eval_x;
    protected Evaluable eval_y;
    protected String errorMessage_x;
    protected String errorMessage_y;

    protected double min_x;
    protected double max_x;
    protected double min_y;
    protected double max_y;
    protected AffineTransform t;

    protected String image;

    public static final String NAME = "name";
    public static final String IMAGE = "image";
    public static final String COLOR = "color";
    public static final String BASE_COLOR = "base_color";
    public static final String SIZE = "l";
    public static final String ROTATION = "r";
    public static final String DOT = "dot";
    public static final String DIM_X = "dim_x";
    public static final String DIM_Y = "dim_y";

    public static final String X_COLUMN_NAME = "cx";
    public static final String X_EXPRESSION = "ex";
    public static final String MIN_X = "min_x";
    public static final String MAX_X = "max_x";
    public static final String TO_X = "to_x";
    public static final String FROM_X = "from_x";
    public static final String ERROR_X = "error_x";

    public static final String Y_COLUMN_NAME = "cy";
    public static final String Y_EXPRESSION = "ey";
    public static final String MIN_Y = "min_y";
    public static final String MAX_Y = "max_y";
    public static final String TO_Y = "to_y";
    public static final String FROM_Y = "from_y";
    public static final String ERROR_Y = "error_y";

    private static final boolean IMAGE_URI = false;

    public static String EMPTY_IMAGE;

    final static Logger logger = LoggerFactory.getLogger("scatterplot_facet");

    static {
        try {
            EMPTY_IMAGE = serializeImage(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR));
        } catch (IOException e) {
            EMPTY_IMAGE = "";
        }
    }

    @JsonProperty(NAME)
    public String getName() {
        return config.name;
    }

    @JsonProperty(X_COLUMN_NAME)
    public String getXColumnName() {
        return config.columnName_x;
    }

    @JsonProperty(X_EXPRESSION)
    public String getXExpression() {
        return config.expression_x;
    }

    @JsonProperty(Y_COLUMN_NAME)
    public String getYColumnName() {
        return config.columnName_y;
    }

    @JsonProperty(Y_EXPRESSION)
    public String getYExpression() {
        return config.expression_y;
    }

    @JsonProperty(SIZE)
    public int getSize() {
        return config.size;
    }

    @JsonProperty(DIM_X)
    public int getDimX() {
        return config.dim_x;
    }

    @JsonProperty(DIM_Y)
    public int getDimY() {
        return config.dim_y;
    }

    @JsonProperty(DOT)
    public double getDot() {
        return config.dot;
    }

    @JsonProperty(ROTATION)
    public double getRotation() {
        return config.rotation;
    }

    @JsonProperty(COLOR)
    public String getColorString() {
        return config.color_str;
    }

    @JsonProperty(IMAGE)
    @JsonInclude(Include.NON_NULL)
    public String getImage() {
        if (IMAGE_URI) {
            return image;
        }
        return null;
    }

    @JsonProperty(ERROR_X)
    @JsonInclude(Include.NON_NULL)
    public String getErrorX() {
        return errorMessage_x;
    }

    @JsonProperty(FROM_X)
    @JsonInclude(Include.NON_NULL)
    public Double getFromX() {
        if (errorMessage_x == null && !Double.isInfinite(min_x) && !Double.isInfinite(max_x)) {
            return config.from_x;
        }
        return null;
    }

    @JsonProperty(TO_X)
    @JsonInclude(Include.NON_NULL)
    public Double getToX() {
        if (errorMessage_x == null && !Double.isInfinite(min_x) && !Double.isInfinite(max_x)) {
            return config.to_x;
        }
        return null;
    }

    @JsonProperty(ERROR_Y)
    @JsonInclude(Include.NON_NULL)
    public String getErrorY() {
        return errorMessage_y;
    }

    @JsonProperty(FROM_Y)
    @JsonInclude(Include.NON_NULL)
    public Double getFromY() {
        if (errorMessage_y == null && !Double.isInfinite(min_y) && !Double.isInfinite(max_y)) {
            return config.from_y;
        }
        return null;
    }

    @JsonProperty(TO_Y)
    @JsonInclude(Include.NON_NULL)
    public Double getToY() {
        if (errorMessage_y == null && !Double.isInfinite(min_y) && !Double.isInfinite(max_y)) {
            return config.to_y;
        }
        return null;
    }

    public void initializeFromConfig(ScatterplotFacetConfig configuration, Project project) {
        config = configuration;

        t = createRotationMatrix(config.rotation, config.l);

        if (config.columnName_x.length() > 0) {
            Column x_column = project.columnModel.getColumnByName(config.columnName_x);
            if (x_column != null) {
                columnIndex_x = x_column.getCellIndex();

                NumericBinIndex index_x = ScatterplotFacet.getBinIndex(project, x_column, eval_x, config.expression_x);
                min_x = index_x.getMin();
                max_x = index_x.getMax();
            } else {
                errorMessage_x = "No column named " + config.columnName_x;
            }
        } else {
            columnIndex_x = -1;
        }

        try {
            eval_x = MetaParser.parse(config.expression_x);
        } catch (ParsingException e) {
            errorMessage_x = e.getMessage();
        }

        if (config.columnName_y.length() > 0) {
            Column y_column = project.columnModel.getColumnByName(config.columnName_y);
            if (y_column != null) {
                columnIndex_y = y_column.getCellIndex();

                NumericBinIndex index_y = ScatterplotFacet.getBinIndex(project, y_column, eval_y, config.expression_y);
                min_y = index_y.getMin();
                max_y = index_y.getMax();
            } else {
                errorMessage_y = "No column named " + config.columnName_y;
            }
        } else {
            columnIndex_y = -1;
        }

        try {
            eval_y = MetaParser.parse(config.expression_y);
        } catch (ParsingException e) {
            errorMessage_y = e.getMessage();
        }

    }

    @Override
    public RowFilter getRowFilter(Project project) {
        if (config.isSelected() &&
                eval_x != null && errorMessage_x == null &&
                eval_y != null && errorMessage_y == null) {
            return new DualExpressionsNumberComparisonRowFilter(
                    eval_x, config.columnName_x, columnIndex_x, eval_y, config.columnName_y, columnIndex_y) {

                double from_x_pixels = config.from_x * config.l;
                double to_x_pixels = config.to_x * config.l;
                double from_y_pixels = config.from_y * config.l;
                double to_y_pixels = config.to_y * config.l;

                @Override
                protected boolean checkValues(double x, double y) {
                    Point2D.Double p = new Point2D.Double(x, y);
                    p = translateCoordinates(p, min_x, max_x, min_y, max_y, config.dim_x, config.dim_y, config.l, t);
                    return p.x >= from_x_pixels && p.x <= to_x_pixels && p.y >= from_y_pixels && p.y <= to_y_pixels;
                };
            };
        } else {
            return null;
        }
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
        RowFilter rowFilter = getRowFilter(project);
        return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        if (eval_x != null && eval_y != null && errorMessage_x == null && errorMessage_y == null) {
            Column column_x = project.columnModel.getColumnByCellIndex(columnIndex_x);
            NumericBinIndex index_x = getBinIndex(project, column_x, eval_x, config.expression_x, "row-based");

            Column column_y = project.columnModel.getColumnByCellIndex(columnIndex_y);
            NumericBinIndex index_y = getBinIndex(project, column_y, eval_y, config.expression_y, "row-based");

            retrieveDataFromBinIndices(index_x, index_y);

            if (IMAGE_URI) {
                if (index_x.isNumeric() && index_y.isNumeric()) {
                    ScatterplotDrawingRowVisitor drawer = new ScatterplotDrawingRowVisitor(
                            columnIndex_x, columnIndex_y, min_x, max_x, min_y, max_y,
                            config.size, config.dim_x, config.dim_y, config.rotation, config.dot, config.getColor());
                    filteredRows.accept(project, drawer);

                    try {
                        image = serializeImage(drawer.getImage());
                    } catch (IOException e) {
                        logger.warn("Exception caught while generating the image", e);
                    }
                } else {
                    image = EMPTY_IMAGE;
                }
            }
        }
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        if (eval_x != null && eval_y != null && errorMessage_x == null && errorMessage_y == null) {
            Column column_x = project.columnModel.getColumnByCellIndex(columnIndex_x);
            NumericBinIndex index_x = getBinIndex(project, column_x, eval_x, config.expression_x, "record-based");

            Column column_y = project.columnModel.getColumnByCellIndex(columnIndex_y);
            NumericBinIndex index_y = getBinIndex(project, column_y, eval_y, config.expression_y, "record-based");

            retrieveDataFromBinIndices(index_x, index_y);

            if (IMAGE_URI) {
                if (index_x.isNumeric() && index_y.isNumeric()) {
                    ScatterplotDrawingRowVisitor drawer = new ScatterplotDrawingRowVisitor(
                            columnIndex_x, columnIndex_y, min_x, max_x, min_y, max_y,
                            config.size, config.dim_x, config.dim_y, config.rotation, config.dot, config.getColor());
                    filteredRecords.accept(project, drawer);

                    try {
                        image = serializeImage(drawer.getImage());
                    } catch (IOException e) {
                        logger.warn("Exception caught while generating the image", e);
                    }
                } else {
                    image = EMPTY_IMAGE;
                }
            }
        }
    }

    protected void retrieveDataFromBinIndices(NumericBinIndex index_x, NumericBinIndex index_y) {
        min_x = index_x.getMin();
        max_x = index_x.getMax();

        min_y = index_y.getMin();
        max_y = index_y.getMax();
    }

    public static String serializeImage(RenderedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        ImageIO.write(image, "png", output);
        output.close();
        String encoded = Base64.encodeBase64String(output.toByteArray());
        String url = "data:image/png;base64," + encoded;
        return url;
    }

    public static int getAxisDim(String type) {
        return ("log".equals(type.toLowerCase())) ? LOG : LIN;
    }

    public static NumericBinIndex getBinIndex(Project project, Column column, Evaluable eval, String expression) {
        return getBinIndex(project, column, eval, expression, "row-based");
    }

    public static NumericBinIndex getBinIndex(Project project, Column column, Evaluable eval, String expression, String mode) {
        String key = "numeric-bin:" + mode + ":" + expression;
        if (eval == null) {
            try {
                eval = MetaParser.parse(expression);
            } catch (ParsingException e) {
                logger.warn("Error parsing expression", e);
            }
        }
        NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
        if (index == null) {
            index = "row-based".equals(mode)
                    ? new NumericBinRowIndex(project, new ExpressionBasedRowEvaluable(column.getName(), column.getCellIndex(), eval))
                    : new NumericBinRecordIndex(project, new ExpressionBasedRowEvaluable(column.getName(), column.getCellIndex(), eval));

            column.setPrecompute(key, index);
        }
        return index;
    }

    private static double s_rotateScale = 1 / Math.sqrt(2.0);

    public static AffineTransform createRotationMatrix(int rotation, double l) {
        if (rotation == ScatterplotFacet.ROTATE_CW) {
            AffineTransform t = AffineTransform.getTranslateInstance(0, l / 2);
            t.scale(s_rotateScale, s_rotateScale);
            t.rotate(-Math.PI / 4);
            return t;
        } else if (rotation == ScatterplotFacet.ROTATE_CCW) {
            AffineTransform t = AffineTransform.getTranslateInstance(l / 2, 0);
            t.scale(s_rotateScale, s_rotateScale);
            t.rotate(Math.PI / 4);
            return t;
        } else {
            return null;
        }
    }

    public static Point2D.Double translateCoordinates(
            Point2D.Double p,
            double min_x, double max_x, double min_y, double max_y,
            int dim_x, int dim_y, double l, AffineTransform t) {

        double x = p.x;
        double y = p.y;

        double relative_x = x - min_x;
        double range_x = max_x - min_x;
        if (dim_x == ScatterplotFacet.LOG) {
            x = Math.log10(relative_x + 1) * l / Math.log10(range_x + 1);
        } else {
            x = relative_x * l / range_x;
        }

        double relative_y = y - min_y;
        double range_y = max_y - min_y;
        if (dim_y == ScatterplotFacet.LOG) {
            y = Math.log10(relative_y + 1) * l / Math.log10(range_y + 1);
        } else {
            y = relative_y * l / range_y;
        }

        p.x = x;
        p.y = y;
        if (t != null) {
            t.transform(p, p);
        }

        return p;
    }

}
