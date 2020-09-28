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

package org.openrefine.browsing.facets;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrefine.browsing.util.ExpressionBasedRowEvaluable;
import org.openrefine.browsing.util.ScatterplotFacetAggregator;
import org.openrefine.browsing.util.ScatterplotFacetState;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScatterplotFacet implements Facet {

    public static final int LIN = 0;
    public static final int LOG = 1;
    
    public static final int NO_ROTATION = 0;
    public static final int ROTATE_CW = 1;
    public static final int ROTATE_CCW = 2;
    
    public static final String NAME = "name";
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
    
    public enum Rotation {
    	@JsonProperty("none")
    	NO_ROTATION,
    	@JsonProperty("cw")
    	ROTATE_CW,
    	@JsonProperty("ccw")
    	ROTATE_CCW
    }
    
    public enum Dimension {
    	@JsonProperty("lin")
    	LIN,
    	@JsonProperty("log")
    	LOG
    }
    
    /*
     * Configuration, from the client side
     */
    public static class ScatterplotFacetConfig implements FacetConfig, Serializable {

		private static final long serialVersionUID = -2612356006098930005L;

		@JsonProperty("name")
        protected final String name; // name of facet
    
        @JsonProperty(X_EXPRESSION)
        protected final String expression_x; // expression to compute the x numeric value(s) per row
        @JsonProperty(Y_EXPRESSION)
        protected final String expression_y; // expression to compute the y numeric value(s) per row
        @JsonProperty(X_COLUMN_NAME)
		public final String columnName_x; // column to base the x expression on, if any
        @JsonProperty(Y_COLUMN_NAME)
		public final String columnName_y; // column to base the y expression on, if any
        
        @JsonProperty(DIM_X)
        public Dimension dim_x;
        @JsonProperty(DIM_Y)
        public Dimension dim_y;
        @JsonProperty(ROTATION)
        public Rotation rotation;
        
        @JsonProperty(FROM_X)
        public double fromX; // the numeric selection for the x axis, from 0 to 1
        @JsonProperty(TO_X)
        public double toX;
        @JsonProperty(FROM_Y)
        public double fromY; // the numeric selection for the y axis, from 0 to 1
        @JsonProperty(TO_Y)
        public double toY;
        
        // the bounds of the scatterplot, used to interpret the values
        // above after transformation
        @JsonProperty(MIN_X)
        public double minX;
        @JsonProperty(MAX_X)
        public double maxX;
        @JsonProperty(MIN_Y)
        public double minY;
        @JsonProperty(MAX_Y)
        public double maxY;
        
        // Only used for plotting
        @JsonProperty(DOT)
		public final double dot;
        @JsonProperty(SIZE)
		public final int size;
        @JsonIgnore
        protected String color_str;
        @JsonIgnore
		public Color getColor() {
            return color_str == null ? null : new Color(Integer.parseInt(color_str,16));
        }
        @JsonIgnore
        protected String baseColor_str;
        @JsonIgnore
        public Color getBaseColor() {
            return baseColor_str == null ? null : new Color(Integer.parseInt(baseColor_str,16));
        }
        
        // Derived configuration
        private Evaluable evaluableX;
        private Evaluable evaluableY;
        private String errorMessageX;
        private String errorMessageY;
        
        @JsonCreator
        public ScatterplotFacetConfig(
        		@JsonProperty("name")
        		String name,
        		@JsonProperty(X_EXPRESSION)
        		String expressionX,
        		@JsonProperty(Y_EXPRESSION)
        		String expressionY,
        		@JsonProperty(X_COLUMN_NAME)
        		String columnNameX,
        		@JsonProperty(Y_COLUMN_NAME)
        		String columnNameY,
        		@JsonProperty(SIZE)
        		int size,
        		@JsonProperty(COLOR)
        		String color,
        		@JsonProperty(BASE_COLOR)
        		String baseColor,
        		@JsonProperty(DOT)
        		double dot,
        		@JsonProperty(FROM_X)
        		double fromX,
        		@JsonProperty(TO_X)
        		double toX,
        		@JsonProperty(FROM_Y)
        		double fromY,
                @JsonProperty(MIN_X)
                double minX,
                @JsonProperty(MAX_X)
                double maxX,
                @JsonProperty(MIN_Y)
                double minY,
                @JsonProperty(MAX_Y)
                double maxY,
                @JsonProperty(TO_Y)
        		double toY,
        		@JsonProperty(DIM_X)
        		Dimension dimX,
        		@JsonProperty(DIM_Y)
        		Dimension dimY,
        		@JsonProperty(ROTATION)
        		Rotation r) {
        	this.name = name;
        	this.expression_x = expressionX != null ? expressionX : "value";
        	this.expression_y = expressionY != null ? expressionY : "value";
        	this.columnName_x = columnNameX;
        	this.columnName_y = columnNameY;
        	this.size = size;
        	this.color_str = color;
        	this.baseColor_str = baseColor;
        	this.dot = dot;
        	this.fromX = fromX;
        	this.toX = toX;
        	this.fromY = fromY;
        	this.toY = toY;
        	this.minX = minX;
        	this.maxX = maxX;
        	this.minY = minY;
        	this.maxY = maxY;
        	this.dim_x = dimX == null ? Dimension.LIN : dimX;
        	this.dim_y = dimY == null ? Dimension.LIN : dimY;
        	this.rotation = r == null ? Rotation.NO_ROTATION : r;
        	try {
	            evaluableX = MetaParser.parse(this.expression_x);
	            errorMessageX = null;
	        } catch (ParsingException e) {
	            errorMessageX = e.getMessage();
	            evaluableX = null;
	        }
        	try {
	            evaluableY = MetaParser.parse(this.expression_y);
	            errorMessageY = null;
	        } catch (ParsingException e) {
	            errorMessageY = e.getMessage();
	            evaluableY = null;
	        }
        }

        @Override
        public String getJsonType() {
            return "core/scatterplot";
        }

		@Override
		public Set<String> getColumnDependencies() {
			if (evaluableX == null || evaluableY == null) {
				return null;
			}
			Set<String> dependenciesX = evaluableX.getColumnDependencies(columnName_x);
			Set<String> dependenciesY = evaluableY.getColumnDependencies(columnName_y);
			if (dependenciesX == null || dependenciesY == null) {
				return null;
			}
			Set<String> columnNames = new HashSet<>();
			columnNames.addAll(dependenciesX);
			columnNames.addAll(dependenciesY);
			return columnNames;
		}

		@Override
		public ScatterplotFacetConfig renameColumnDependencies(Map<String, String> substitutions) {
			if (evaluableX == null || evaluableY == null) {
				return null;
			}
			Evaluable renamedX = evaluableX.renameColumnDependencies(substitutions);
			Evaluable renamedY = evaluableY.renameColumnDependencies(substitutions);
			if (renamedX == null || renamedY == null) {
				return null;
			}
			return new ScatterplotFacetConfig(
	        		name,
	        		evaluableX.getFullSource(),
	        		evaluableY.getFullSource(),
	        		substitutions.getOrDefault(columnName_x, columnName_x),
	        		substitutions.getOrDefault(columnName_y, columnName_y),
	        		size,
	        		color_str,
	        		baseColor_str,
	        		dot,
	        		fromX,
	        		toX,
	        		fromY,
	        		toY,
	        		minX,
	        		maxX,
	        		minY,
	        		maxY,
	        		dim_x,
	        		dim_y,
	        		rotation);
		}

		@Override
		public boolean isNeutral() {
			return !(fromX > 0 || toX < 1 || fromY > 0 || toY < 1) || (minX == 0 && maxX == 0 && minY == 0 && maxY == 0);
		}

		@Override
		public ScatterplotFacet apply(ColumnModel columnModel) {
			return new ScatterplotFacet(this,
					columnModel.getColumnIndexByName(columnName_x),
					columnModel.getColumnIndexByName(columnName_y));
		}
		
		@JsonIgnore
		public Evaluable getEvaluableX() {
			return evaluableX;
		}
		
		@JsonIgnore
		public Evaluable getEvaluableY() {
			return evaluableY;
		}
    }
    
    ScatterplotFacetConfig config;

    /*
     * Derived configuration data
     */
    protected int        columnIndex_x;
    protected int        columnIndex_y;
    protected Evaluable  eval_x;
    protected Evaluable  eval_y;
    protected String     errorMessage_x;
    protected String     errorMessage_y;

    final static Logger logger = LoggerFactory.getLogger("scatterplot_facet");
    
    public ScatterplotFacet(ScatterplotFacetConfig config, int cellIndexX, int cellIndexY) {
    	this.config = config;
        
        columnIndex_x = cellIndexX;
        columnIndex_y = cellIndexY;
        if (cellIndexX == -1) {
            errorMessage_x = "No column named " + config.columnName_x;
        } else {
        	errorMessage_x = config.errorMessageX;
        }
        if (cellIndexY == -1) {
        	errorMessage_y = "No column named " + config.columnName_y;
        } else {
        	errorMessage_y = config.errorMessageY;
        }
    }
    
    private static double s_rotateScale = 1 / Math.sqrt(2.0);
    
    public static AffineTransform createRotationMatrix(Rotation rotation, double l) {
        if (rotation == Rotation.ROTATE_CW) {
            AffineTransform t = AffineTransform.getTranslateInstance(0, l / 2);
            t.scale(s_rotateScale, s_rotateScale);
            t.rotate(-Math.PI / 4);
            return t;
        } else if (rotation == Rotation.ROTATE_CCW) {
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
            double minX, double maxX, double minY, double maxY,
            Dimension dimX,
            Dimension dimY,
            double l,
            AffineTransform t) {
        
        double x = p.x;
        double y = p.y;
        
        double relativeX = x - minX;
        double rangeX = maxX - minX;
                
        if (dimX == Dimension.LOG) {
            x = Math.log10(Math.abs(relativeX) + 1) * l / Math.log10(rangeX + 1);
        } else {
            x = relativeX * l / rangeX;
        }

        double relativeY = y - minY;
        double rangeY = maxY - minY;
        if (dimY == Dimension.LOG) {
            y = Math.log10(Math.abs(relativeY) + 1) * l / Math.log10(rangeY + 1);
        } else {
            y = relativeY * l / rangeY;
        }
        
        p.x = x;
        p.y = y;
        if (t != null) {
            t.transform(p, p);
        }
        
        return p;
    }

	@Override
	public FacetConfig getConfig() {
		return config;
	}

	@Override
	public FacetState getInitialFacetState() {
		return new ScatterplotFacetState(new double[] {}, new double[] {}, new boolean[] {}, 0);
	}

	@Override
	public FacetAggregator<ScatterplotFacetState> getAggregator() {
		if (config.evaluableX != null && config.evaluableY != null) {
			return new ScatterplotFacetAggregator(config,
				new ExpressionBasedRowEvaluable(config.columnName_x, columnIndex_x, config.evaluableX),
				new ExpressionBasedRowEvaluable(config.columnName_y, columnIndex_y, config.evaluableY));
		} else {
			return null;
		}
	}

	@Override
	public ScatterplotFacetResult getFacetResult(FacetState state) {
		ScatterplotFacetState statistics = (ScatterplotFacetState) state;
		return new ScatterplotFacetResult(config, errorMessage_x, errorMessage_y, statistics);
	}
    
}
