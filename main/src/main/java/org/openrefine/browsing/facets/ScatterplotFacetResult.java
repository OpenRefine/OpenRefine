package org.openrefine.browsing.facets;

import org.openrefine.browsing.facets.ScatterplotFacet.Dimension;
import org.openrefine.browsing.facets.ScatterplotFacet.Rotation;
import org.openrefine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import org.openrefine.browsing.util.ScatterplotFacetState;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.primitives.Doubles;

/**
 * Represents the statistics sent to the frontend
 * for a scatterplot facet. The scatterplot image is
 * sent separately.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ScatterplotFacetResult implements FacetResult {
	
	private ScatterplotFacetConfig config;
	private String errorMessageX;
	private String errorMessageY;
	private double minX; 
    private double maxX;
    private double minY;
    private double maxY;
    private ScatterplotFacetState facetState;
	
	public ScatterplotFacetResult(
			ScatterplotFacetConfig config,
			String errorMessageX,
			String errorMessageY,
			ScatterplotFacetState facetState) {
		this.config = config;
		this.errorMessageX = errorMessageX;
		this.errorMessageY = errorMessageY;
		this.facetState = facetState;
		minX = 0;
		minY = 0;
		maxX = 0;
		maxY = 0;
		if (errorMessageX == null) {
			double[] valuesX = facetState.getValuesX();
			minX = Doubles.min(valuesX);
			maxX = Doubles.max(valuesX);
		}
		if (errorMessageY == null) {
			double[] valuesY = facetState.getValuesY();
			minY = Doubles.min(valuesY);
			maxY = Doubles.max(valuesY);
		}
	}
	
    @JsonProperty(ScatterplotFacet.NAME)
    public String getName() {
        return config.name;
    }
    
    @JsonProperty(ScatterplotFacet.X_COLUMN_NAME)
    public String getXColumnName() {
        return config.columnName_x;
    }
    
    @JsonProperty(ScatterplotFacet.X_EXPRESSION)
    public String getXExpression() {
        return config.expression_x;
    }
    
    @JsonProperty(ScatterplotFacet.Y_COLUMN_NAME)
    public String getYColumnName() {
        return config.columnName_y;
    }
    
    @JsonProperty(ScatterplotFacet.Y_EXPRESSION)
    public String getYExpression() {
        return config.expression_y;
    }
    
    @JsonProperty(ScatterplotFacet.SIZE)
    public int getSize() {
        return config.size;
    }
    
    @JsonProperty(ScatterplotFacet.DIM_X)
    public Dimension getDimX() {
        return config.dim_x;
    }
    
    @JsonProperty(ScatterplotFacet.DIM_Y)
    public Dimension getDimY() {
        return config.dim_y;
    }
    
    @JsonProperty(ScatterplotFacet.DOT)
    public double getDot() {
        return config.dot;
    }
    
    @JsonProperty(ScatterplotFacet.ROTATION)
    public Rotation getRotation() {
        return config.rotation;
    }
    
    @JsonProperty(ScatterplotFacet.ERROR_X)
    @JsonInclude(Include.NON_NULL)
    public String getErrorX() {
        return errorMessageX;
    }
    
    @JsonProperty(ScatterplotFacet.FROM_X)
    @JsonInclude(Include.NON_NULL)
    public Double getFromX() {
        if (errorMessageX == null && !Double.isInfinite(minX) && !Double.isInfinite(maxX)) {
            return config.fromX;
        }
        return null;
    }
    
    @JsonProperty(ScatterplotFacet.TO_X)
    @JsonInclude(Include.NON_NULL)
    public Double getToX() {
        if (errorMessageX == null && !Double.isInfinite(minX) && !Double.isInfinite(maxX)) {
            return config.toX;
        }
        return null;
    }
    
    @JsonProperty(ScatterplotFacet.ERROR_Y)
    @JsonInclude(Include.NON_NULL)
    public String getErrorY() {
        return errorMessageY;
    }
    
    @JsonProperty(ScatterplotFacet.FROM_Y)
    @JsonInclude(Include.NON_NULL)
    public Double getFromY() {
        if (errorMessageY == null && !Double.isInfinite(minY) && !Double.isInfinite(maxY)) {
            return config.fromY;
        }
        return null;
    }
    
    @JsonProperty(ScatterplotFacet.TO_Y)
    @JsonInclude(Include.NON_NULL)
    public Double getToY() {
        if (errorMessageY == null && !Double.isInfinite(minY) && !Double.isInfinite(maxY)) {
            return config.toY;
        }
        return null;
    }
    
    @JsonProperty(ScatterplotFacet.MIN_X)
    @JsonInclude(Include.NON_NULL)
    public Double getMinX() {
        if (errorMessageX == null && !Double.isInfinite(minX)) {
            return minX;
        }
        return null;
    }
    
    @JsonProperty(ScatterplotFacet.MAX_X)
    @JsonInclude(Include.NON_NULL)
    public Double getMaxX() {
        if (errorMessageX == null && !Double.isInfinite(maxX)) {
            return maxX;
        }
        return null;
    }
    
    @JsonProperty(ScatterplotFacet.MIN_Y)
    @JsonInclude(Include.NON_NULL)
    public Double getMinY() {
        if (errorMessageY == null && !Double.isInfinite(minY)) {
            return minY;
        }
        return null;
    }
    
    @JsonProperty(ScatterplotFacet.MAX_Y)
    @JsonInclude(Include.NON_NULL)
    public Double getMaxY() {
        if (errorMessageY == null && !Double.isInfinite(maxY)) {
            return maxY;
        }
        return null;
    }
    
    @JsonIgnore
    public ScatterplotFacetState getFacetState() {
    	return facetState;
    }
}
