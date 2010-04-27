package com.metaweb.gridworks.browsing.facets;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.filters.DualExpressionsNumberComparisonRowFilter;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;

public class ScatterplotFacet implements Facet {

    public static final int LIN = 0;
    public static final int LOG = 1;
    
    public static final int NO_ROTATION = 0;
    public static final int ROTATE_CW = 1;
    public static final int ROTATE_CCW = 2;
    
    /*
     * Configuration, from the client side
     */
    protected String name; // name of facet

    protected String expression_x; // expression to compute the x numeric value(s) per row
    protected String expression_y; // expression to compute the y numeric value(s) per row
    protected String columnName_x; // column to base the x expression on, if any
    protected String columnName_y; // column to base the y expression on, if any
    
    protected double from_x; // the numeric selection for the x axis
    protected double to_x;
    protected double from_y; // the numeric selection for the y axis
    protected double to_y;

    protected double min_x; 
    protected double max_x;
    protected double min_y;
    protected double max_y;
    
    protected int size;
    protected int dim_x;
    protected int dim_y;
    protected int rotation;

    protected double l;
    protected double dot;

    protected String image;
    
    protected String color_str;
    protected Color color;
    
    /*
     * Derived configuration data
     */
    protected int        columnIndex_x;
    protected int        columnIndex_y;
    protected Evaluable  eval_x;
    protected Evaluable  eval_y;
    protected String     errorMessage_x;
    protected String     errorMessage_y;

    protected boolean selected; // false if we're certain that all rows will match
                                // and there isn't any filtering to do
        
    public static final String NAME = "name";
    public static final String IMAGE = "image";
    public static final String COLOR = "color";
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
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        
        writer.object();
        
        writer.key(NAME); writer.value(name);
        writer.key(X_COLUMN_NAME); writer.value(columnName_x);
        writer.key(X_EXPRESSION); writer.value(expression_x);
        writer.key(Y_COLUMN_NAME); writer.value(columnName_y);
        writer.key(Y_EXPRESSION); writer.value(expression_y);
        writer.key(SIZE); writer.value(size);
        writer.key(DOT); writer.value(dot);
        writer.key(ROTATION); writer.value(rotation);
        writer.key(DIM_X); writer.value(dim_x);
        writer.key(DIM_Y); writer.value(dim_y);
        writer.key(COLOR); writer.value(color_str);

        if (IMAGE_URI) {
            writer.key(IMAGE); writer.value(image);
        }
        
        if (errorMessage_x != null) {
            writer.key(ERROR_X); writer.value(errorMessage_x);
        } else {
            if (!Double.isInfinite(min_x) && !Double.isInfinite(max_x)) {
                writer.key(MIN_X); writer.value(min_x);
                writer.key(MAX_X); writer.value(max_x);
                writer.key(FROM_X); writer.value(from_x);
                writer.key(TO_X); writer.value(to_x);
            }
        }
            
        if (errorMessage_y != null) {
            writer.key(ERROR_Y); writer.value(errorMessage_y);
        } else {
            if (!Double.isInfinite(min_y) && !Double.isInfinite(max_y)) {
                writer.key(MIN_Y); writer.value(min_y);
                writer.key(MAX_Y); writer.value(max_y);
                writer.key(FROM_Y); writer.value(from_y);
                writer.key(TO_Y); writer.value(to_y);
            }
        }
        
        writer.endObject();
    }

    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        name = o.getString(NAME);

        size = (o.has(SIZE)) ? o.getInt(SIZE) : 100;
        l = size;

        dot = (o.has(DOT)) ? o.getInt(DOT) : 0.5d;
        
        dim_x = (o.has(DIM_X)) ? getAxisDim(o.getString(DIM_X)) : LIN;
        dim_y = (o.has(DIM_Y)) ? getAxisDim(o.getString(DIM_Y)) : LIN;

        rotation = (o.has(ROTATION)) ? getRotation(o.getString(ROTATION)) : NO_ROTATION;
        
        color_str = (o.has(COLOR)) ? o.getString(COLOR) : "000000";
        color = new Color(Integer.parseInt(color_str,16));            
        
        columnName_x = o.getString(X_COLUMN_NAME);
        expression_x = o.getString(X_EXPRESSION);
        
        if (columnName_x.length() > 0) {
            Column x_column = project.columnModel.getColumnByName(columnName_x);
            if (x_column != null) {
                columnIndex_x = x_column.getCellIndex();
                
                NumericBinIndex index_x = ScatterplotFacet.getBinIndex(project, x_column, eval_x, expression_x);
                min_x = index_x.getMin();
                max_x = index_x.getMax();
            } else {
                errorMessage_x = "No column named " + columnName_x;
            }
        } else {
            columnIndex_x = -1;
        }
        
        try {
            eval_x = MetaParser.parse(expression_x);
        } catch (ParsingException e) {
            errorMessage_x = e.getMessage();
        }
        
        if (o.has(FROM_X) && o.has(TO_X)) {
            from_x = o.getDouble(FROM_X);
            to_x = o.getDouble(TO_X);
            selected = true;
        }
        
        columnName_y = o.getString(Y_COLUMN_NAME);
        expression_y = o.getString(Y_EXPRESSION);
        
        if (columnName_y.length() > 0) {
            Column y_column = project.columnModel.getColumnByName(columnName_y);
            if (y_column != null) {
                columnIndex_y = y_column.getCellIndex();
                
                NumericBinIndex index_y = ScatterplotFacet.getBinIndex(project, y_column, eval_y, expression_y);
                min_y = index_y.getMin();
                max_y = index_y.getMax();
            } else {
                errorMessage_y = "No column named " + columnName_y;
            }
        } else {
            columnIndex_y = -1;
        }
        
        try {
            eval_y = MetaParser.parse(expression_y);
        } catch (ParsingException e) {
            errorMessage_y = e.getMessage();
        }
        
        if (o.has(FROM_Y) && o.has(TO_Y)) {
            from_y = o.getDouble(FROM_Y);
            to_y = o.getDouble(TO_Y);
            selected = true;
        }
    }

    public RowFilter getRowFilter() {
        if (selected && 
            eval_x != null && errorMessage_x == null && 
            eval_y != null && errorMessage_y == null) 
        {
            return new DualExpressionsNumberComparisonRowFilter(eval_x, columnName_x, columnIndex_x, eval_y, columnName_y, columnIndex_y) {
                protected boolean checkValues(double x, double y) {
                    Point2D.Double p = new Point2D.Double(x,y);
                    p = translateCoordinates(p, dim_x, dim_y, rotation, l, min_x, max_x, min_y, max_y);
                    
                    boolean value = p.x >= from_x && p.x < to_x && p.y >= from_y && p.y < to_y;
                    
                    return value;
                };
            };
        } else {
            return null;
        }
    }

    public void computeChoices(Project project, FilteredRows filteredRows) {
        if (eval_x != null && eval_y != null && errorMessage_x == null && errorMessage_y == null) {
            Column column_x = project.columnModel.getColumnByCellIndex(columnIndex_x);
            NumericBinIndex index_x = getBinIndex(project, column_x, eval_x, expression_x);
            
            min_x = index_x.getMin();
            max_x = index_x.getMax();
            
            if (selected) {
                from_x = Math.max(from_x, min_x);
                to_x = Math.min(to_x, max_x);
            } else {
                from_x = min_x;
                to_x = max_x;
            }
                        
            Column column_y = project.columnModel.getColumnByCellIndex(columnIndex_y);
            NumericBinIndex index_y = getBinIndex(project, column_y, eval_y, expression_y);

            min_y = index_y.getMin();
            max_y = index_y.getMax();
            
            if (selected) {
                from_y = Math.max(from_y, min_y);
                to_y = Math.min(to_y, max_y);
            } else {
                from_y = min_y;
                to_y = max_y;
            }
            
            if (IMAGE_URI) {
                if (index_x.isNumeric() && index_y.isNumeric()) {
                    ScatterplotDrawingRowVisitor drawer = new ScatterplotDrawingRowVisitor(
                      columnIndex_x, columnIndex_y, min_x, max_x, min_y, max_y, 
                      size, dim_x, dim_y, rotation, dot, color
                    );
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

    public static String serializeImage(RenderedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        ImageIO.write(image, "png", output);
        output.close();
        String encoded = Base64.encodeBase64String(output.toByteArray());
        String url =  "data:image/png;base64," + encoded;
        return url;
    }
    
    public static int getAxisDim(String type) {
        return ("log".equals(type.toLowerCase())) ? LOG : LIN;
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
    
    public static NumericBinIndex getBinIndex(Project project, Column column, Evaluable eval, String expression) {
        String key = "numeric-bin:" + expression;
        if (eval == null) {
            try {
                eval = MetaParser.parse(expression);
            } catch (ParsingException e) {
                logger.warn("Error parsing expression",e);
            }
        }
        NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
        if (index == null) {
            index = new NumericBinIndex(project, column.getName(), column.getCellIndex(), eval);
            column.setPrecompute(key, index);
        }
        return index;
    }
    
    public static Point2D.Double translateCoordinates(Point2D.Double p, int dim_x, int dim_y, int rotation, double l, double min_x, double max_x, double min_y, double max_y) {
        double x = p.x;
        double y = p.y;
        
        if (dim_x == ScatterplotFacet.LOG) {
            x = Math.log10(p.x - min_x) * l / Math.log10(max_x - min_x);
        } else {
            x = (p.x - min_x) * l / (max_x - min_x);
        }

        if (dim_y == ScatterplotFacet.LOG) {
            y = Math.log10(p.y - min_y) * l / Math.log10(max_y - min_y);
        } else {
            y = (p.y - min_y) * l / (max_y - min_y);
        }
        
        if (rotation == ScatterplotFacet.ROTATE_CW) {
            double x1 = (x + y) / 2;
            double y1 = (l - x + y) / 2;
            x = x1;
            y = y1;
        } else if (rotation == ScatterplotFacet.ROTATE_CCW) {
            double x1 = (l - y + x) / 2;
            double y1 = (y + x) / 2;
            x = x1;
            y = y1;
        }
     
        p.x = x;
        p.y = y;
        
        return p;
    }
    
}
