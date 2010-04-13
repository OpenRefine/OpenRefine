package com.metaweb.gridworks.browsing.charting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.browsing.facets.NumericBinIndex;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ScatterplotCharter {

    private static final int LIN = 0;
    private static final int LOG = 1;
    private static final int RADIAL = 2;
    
    private static int getAxisDim(String type) {
        if ("log".equals(type)) {
            return LOG;
        } else if ("rad".equals(type) || "radial".equals(type)) {
            return RADIAL;
        } else {
            return LIN;
        }
    }
    
    public String getContentType() {
        return "image/png";
    }
    
    public void draw(OutputStream output, Project project, Engine engine, JSONObject options) throws IOException, JSONException {

        DrawingRowVisitor drawingVisitor = new DrawingRowVisitor(project, options);
        FilteredRows filteredRows = engine.getAllFilteredRows(false);
        filteredRows.accept(project, drawingVisitor);
     
        ImageIO.write(drawingVisitor.getImage(), "png", output);
    }
    
    class DrawingRowVisitor implements RowVisitor {

        boolean process = true;
        
        int width;
        int height;
        int col_x;
        int col_y;
        int dim;

        double w;
        double h;
        double min_x;
        double min_y;
        double max_x;
        double max_y;
        double delta_x;
        double delta_y;
        double log_delta_x;
        double log_delta_y;
        double rx;
        double ry;
        double dot;
        
        Color color; 
        
        NumericBinIndex index_x;
        NumericBinIndex index_y;
        
        BufferedImage image;
        Graphics2D g2;
               
        public DrawingRowVisitor(Project project, JSONObject o) throws JSONException {
            String col_x_name = o.getString("cx");
            Column column_x = project.columnModel.getColumnByName(col_x_name);
            if (column_x != null) {
                col_x = column_x.getCellIndex();
                index_x = getBinIndex(project, column_x);
                min_x = index_x.getMin();
                max_x = index_x.getMax();
            }

            String col_y_name = o.getString("cy");
            Column column_y = project.columnModel.getColumnByName(col_y_name);
            if (column_y != null) {
                col_y = column_y.getCellIndex();
                index_y = getBinIndex(project, column_y);
                min_y = index_y.getMin();
                max_y = index_y.getMax();
            }
            
            width = (o.has("w")) ? o.getInt("w") : 20;
            height = (o.has("h")) ? o.getInt("h") : 20;
            
            dot = (o.has("dot")) ? o.getDouble("dot") : 0.1d;
            
            dim = (o.has("dim")) ? getAxisDim(o.getString("dim")) : LIN;
            
            delta_x = max_x - min_x;
            delta_y = max_y - min_y;
            
            if (dim == RADIAL) {
                rx = (o.has("rx")) ? o.getDouble("rx") : 0.0d;
                ry = (o.has("ry")) ? o.getDouble("ry") : 0.0d;
            } else if (dim == LOG) {
                log_delta_x = Math.log10(delta_x);
                log_delta_y = Math.log10(delta_y);
            }
            
            String color_str = (o.has("color")) ? o.getString("color") : "000000";
            color = new Color(Integer.parseInt(color_str,16));            
            
            w = (double) width;
            h = (double) height;
            
            if (index_x.isNumeric() && index_y.isNumeric()) {
                image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                g2 = (Graphics2D) image.getGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(1.0f));
                AffineTransform t = AffineTransform.getTranslateInstance(0,h);
                t.concatenate(AffineTransform.getScaleInstance(1.0d, -1.0d));
                g2.setTransform(t);
                g2.setColor(color);
                g2.setPaint(color);
            } else {
                image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
                process = false;
            }
        }

        private NumericBinIndex getBinIndex(Project project, Column column) {
            String expression = "value";
            String key = "numeric-bin:" + expression;
            Evaluable eval = null;
            try {
                eval = MetaParser.parse(expression);
            } catch (ParsingException e) {
                // this should never happen
            }
            NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
            if (index == null) {
                index = new NumericBinIndex(project, column.getName(), column.getCellIndex(), eval);
                column.setPrecompute(key, index);
            }
            return index;
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
            if (process) {
                Cell cellx = row.getCell(col_x);
                Cell celly = row.getCell(col_y);
                if ((cellx != null && cellx.value != null && cellx.value instanceof Number) &&
                    (celly != null && celly.value != null && celly.value instanceof Number)) 
                {
                    double xv = ((Number) cellx.value).doubleValue();
                    double yv = ((Number) celly.value).doubleValue();
                                        
                    double x;
                    double y;
                    
                    if (dim == LOG) {
                        x = Math.log10(xv - min_x) * w / log_delta_x - dot / 2;
                        y = Math.log10(yv - min_y) * h / log_delta_y - dot / 2;
                    } else if (dim == RADIAL) {
                        x = (xv - min_x) * w / delta_x - dot / 2;
                        y = (yv - min_y) * h / delta_y - dot / 2;
                    } else {
                        x = (xv - min_x) * w / delta_x - dot / 2;
                        y = (yv - min_y) * h / delta_y - dot / 2;
                    }
                    g2.fill(new Rectangle2D.Double(x, y, dot, dot));
                }
            }
            
            return false;
        }
        
        public RenderedImage getImage() {
            return image;
        }
    }
}
