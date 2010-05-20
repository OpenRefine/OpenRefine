package com.metaweb.gridworks.browsing.facets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;

public class ScatterplotDrawingRowVisitor implements RowVisitor, RecordVisitor {

    int col_x;
    int col_y;
    int dim_x;
    int dim_y;
    int rotation;

    double l;
    double dot;

    double min_x;
    double max_x;
    double min_y;
    double max_y;
    
    BufferedImage image;
    Graphics2D g2;
    
    AffineTransform r;
    
    public ScatterplotDrawingRowVisitor(
            int col_x, int col_y, double min_x, double max_x, double min_y, double max_y,
            int size, int dim_x, int dim_y, int rotation, double dot, Color color)  
    {
        this.col_x = col_x;
        this.col_y = col_y;
        this.min_x = min_x;
        this.min_y = min_y;
        this.max_x = max_x;
        this.max_y = max_y;
        this.dot = dot;
        this.dim_x = dim_x;
        this.dim_y = dim_y;
        this.rotation = rotation;
        
        l = (double) size;
        r = ScatterplotFacet.createRotationMatrix(rotation, l);

        image = new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR);
        g2 = (Graphics2D) image.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1.0f));
        
        AffineTransform t = AffineTransform.getTranslateInstance(0, l);
        t.scale(1, -1);
        
        g2.setTransform(t);
        g2.setColor(color);
        g2.setPaint(color);
        
        if (r != null) {
            /*
             *  Fill in the negative quadrants to give a hint of how the plot has been rotated.
             */
            Graphics2D g2r = (Graphics2D) g2.create();
            g2r.transform(r);
            
            g2r.setPaint(Color.lightGray);
            g2r.fillRect(-size, 0, size, size);
            g2r.fillRect(0, -size, size, size);
            g2r.dispose();
        }
    }
    
    public void setColor(Color color) {
        g2.setColor(color);
        g2.setPaint(color);
    }
    
    @Override
    public boolean visit(Project project, int rowIndex, Row row) {
        Cell cellx = row.getCell(col_x);
        Cell celly = row.getCell(col_y);
        if ((cellx != null && cellx.value != null && cellx.value instanceof Number) &&
            (celly != null && celly.value != null && celly.value instanceof Number)) 
        {
            double xv = ((Number) cellx.value).doubleValue();
            double yv = ((Number) celly.value).doubleValue();

            Point2D.Double p = new Point2D.Double(xv,yv);
            
            p = ScatterplotFacet.translateCoordinates(
                    p, min_x, max_x, min_y, max_y, dim_x, dim_y, l, r);
            
            g2.fill(new Rectangle2D.Double(p.x - dot / 2, p.y - dot / 2, dot, dot));
        }
        
        return false;
    }
    
    @Override
    public boolean visit(Project project, Record record) {
    	for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
    		visit(project, r, project.rows.get(r));
    	}
    	return false;
    }
    
    public RenderedImage getImage() {
        return image;
    }
}

