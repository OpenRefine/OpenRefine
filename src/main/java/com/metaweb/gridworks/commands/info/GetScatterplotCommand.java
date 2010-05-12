package com.metaweb.gridworks.commands.info;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.facets.NumericBinIndex;
import com.metaweb.gridworks.browsing.facets.ScatterplotDrawingRowVisitor;
import com.metaweb.gridworks.browsing.facets.ScatterplotFacet;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;

public class GetScatterplotCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("get-scatterplot_command");
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            long start = System.currentTimeMillis();
            
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            JSONObject conf = getJsonParameter(request,"plotter");
            
            response.setHeader("Content-Type", "image/png");
            
            ServletOutputStream sos = null;
            
            try {
                sos = response.getOutputStream();
                draw(sos, project, engine, conf);
            } finally {
                sos.close();
            }
            
            logger.info("Drawn scatterplot in {} ms", Long.toString(System.currentTimeMillis() - start));
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }
    
    public void draw(OutputStream output, Project project, Engine engine, JSONObject o) throws IOException, JSONException {

        double min_x = 0;
        double min_y = 0;
        double max_x = 0;
        double max_y = 0;
        
        int columnIndex_x = 0;
        int columnIndex_y = 0;
        
        Evaluable eval_x = null;
        Evaluable eval_y = null;
        
        int size = (o.has(ScatterplotFacet.SIZE)) ? o.getInt(ScatterplotFacet.SIZE) : 100;

        double dot = (o.has(ScatterplotFacet.DOT)) ? o.getDouble(ScatterplotFacet.DOT) : 100;
        
        int dim_x = (o.has(ScatterplotFacet.DIM_X)) ? ScatterplotFacet.getAxisDim(o.getString(ScatterplotFacet.DIM_X)) : ScatterplotFacet.LIN;
        int dim_y = (o.has(ScatterplotFacet.DIM_Y)) ? ScatterplotFacet.getAxisDim(o.getString(ScatterplotFacet.DIM_Y)) : ScatterplotFacet.LIN;

        int rotation = (o.has(ScatterplotFacet.ROTATION)) ? ScatterplotFacet.getRotation(o.getString(ScatterplotFacet.ROTATION)) : ScatterplotFacet.NO_ROTATION;
        
        String color_str = (o.has(ScatterplotFacet.COLOR)) ? o.getString(ScatterplotFacet.COLOR) : "000000";
        Color color = new Color(Integer.parseInt(color_str,16));
        
        String base_color_str = (o.has(ScatterplotFacet.BASE_COLOR)) ? o.getString(ScatterplotFacet.BASE_COLOR) : null;
        Color base_color = base_color_str != null ? new Color(Integer.parseInt(base_color_str,16)) : null;
        
        String columnName_x = o.getString(ScatterplotFacet.X_COLUMN_NAME);
        String expression_x = (o.has(ScatterplotFacet.X_EXPRESSION)) ? o.getString(ScatterplotFacet.X_EXPRESSION) : "value";
        
        if (columnName_x.length() > 0) {
            Column x_column = project.columnModel.getColumnByName(columnName_x);
            if (x_column != null) {
                columnIndex_x = x_column.getCellIndex();
            }
        } else {
            columnIndex_x = -1;
        }
        
        try {
            eval_x = MetaParser.parse(expression_x);
        } catch (ParsingException e) {
            logger.warn("error parsing expression", e);
        }
        
        String columnName_y = o.getString(ScatterplotFacet.Y_COLUMN_NAME);
        String expression_y = (o.has(ScatterplotFacet.Y_EXPRESSION)) ? o.getString(ScatterplotFacet.Y_EXPRESSION) : "value";
        
        if (columnName_y.length() > 0) {
            Column y_column = project.columnModel.getColumnByName(columnName_y);
            if (y_column != null) {
                columnIndex_y = y_column.getCellIndex();
            }
        } else {
            columnIndex_y = -1;
        }
        
        try {
            eval_y = MetaParser.parse(expression_y);
        } catch (ParsingException e) {
            logger.warn("error parsing expression", e);
        }
        
        NumericBinIndex index_x = null;
        NumericBinIndex index_y = null;
        
        String col_x_name = o.getString(ScatterplotFacet.X_COLUMN_NAME);
        Column column_x = project.columnModel.getColumnByName(col_x_name);
        if (column_x != null) {
            columnIndex_x = column_x.getCellIndex();
            index_x = ScatterplotFacet.getBinIndex(project, column_x, eval_x, expression_x);
            min_x = index_x.getMin();
            max_x = index_x.getMax();
        }

        String col_y_name = o.getString(ScatterplotFacet.Y_COLUMN_NAME);
        Column column_y = project.columnModel.getColumnByName(col_y_name);
        if (column_y != null) {
            columnIndex_y = column_y.getCellIndex();
            index_y = ScatterplotFacet.getBinIndex(project, column_y, eval_y, expression_y);
            min_y = index_y.getMin();
            max_y = index_y.getMax();
        }
        
        if (index_x != null && index_y != null && index_x.isNumeric() && index_y.isNumeric()) {
            ScatterplotDrawingRowVisitor drawer = new ScatterplotDrawingRowVisitor(
                columnIndex_x, columnIndex_y, min_x, max_x, min_y, max_y, 
                size, dim_x, dim_y, rotation, dot, color
            );
            
            if (base_color != null) {
                drawer.setColor(base_color);
                
                FilteredRows filteredRows = engine.getAllRows();
                filteredRows.accept(project, drawer);
                
                drawer.setColor(color);
            }
            
            {
                FilteredRows filteredRows = engine.getAllFilteredRows(false);
                filteredRows.accept(project, drawer);
            }
            
            ImageIO.write(drawer.getImage(), "png", output);
        } else {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), "png", output);
        }
        
    }
    
}
