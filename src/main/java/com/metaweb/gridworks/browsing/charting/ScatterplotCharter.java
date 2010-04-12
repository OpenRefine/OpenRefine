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

    private static final Color COLOR = Color.black;

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

        private static final double px = 0.5f;

        boolean process = true;
        boolean smoothed = false;
        
        int width = 50;
        int height = 50;
        
        int col_x;
        int col_y;
        double w;
        double h;
        double min_x;
        double min_y;
        double max_x;
        double max_y;

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
                min_x = index_x.getMin() * 1.1d;
                max_x = index_x.getMax() * 1.1d;
            }

            String col_y_name = o.getString("cy");
            Column column_y = project.columnModel.getColumnByName(col_y_name);
            if (column_y != null) {
                col_y = column_y.getCellIndex();
                index_y = getBinIndex(project, column_y);
                min_y = index_y.getMin() * 1.1d;
                max_y = index_y.getMax() * 1.1d;
            }
            
            width = o.getInt("w");
            height = o.getInt("h");
            
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
                g2.setColor(COLOR);
                g2.setPaint(COLOR);
            } else {
                image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
                process = false;
            }
        }

        private NumericBinIndex getBinIndex(Project project, Column column) {
            String key = "numeric-bin:value";
            Evaluable eval = null;
            try {
                eval = MetaParser.parse("value");
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
                                        
                    double x = (xv - min_x) * w / max_x;
                    double y = (yv - min_y) * h / max_y;
                    g2.fill(new Rectangle2D.Double(x, y, px, px));
                }
            }
            
            return false;
        }
        
        public RenderedImage getImage() {
            return image;
        }
    }
}
