package org.openrefine.browsing.util;

import java.util.Properties;

import org.openrefine.browsing.facets.FacetAggregator;
import org.openrefine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;

import com.google.common.primitives.Booleans;
import com.google.common.primitives.Doubles;

public class ScatterplotFacetAggregator implements FacetAggregator<ScatterplotFacetState> {

	private static final long serialVersionUID = -4133898620996291915L;
	
	private final ScatterplotFacetConfig    _config;
	private final RowEvaluable              _evalX;
	private final RowEvaluable              _evalY;
	
	public ScatterplotFacetAggregator(
			ScatterplotFacetConfig config,
			RowEvaluable evalX,
			RowEvaluable evalY) {
		_config = config;
		_evalX = evalX;
		_evalY = evalY;
	}
	
	@Override
	public ScatterplotFacetState withRow(ScatterplotFacetState state, long rowId, Row row) {
		return withRow(state, rowId, row, true);
	}
	
	@Override
	public ScatterplotFacetState withRowOutsideView(ScatterplotFacetState state, long rowId, Row row) {
		return withRow(state, rowId, row, false);
	}

	public ScatterplotFacetState withRow(ScatterplotFacetState state, long rowId, Row row, boolean inView) {
		Properties x_bindings = ExpressionUtils.createBindings();
        Object vx = _evalX.eval(rowId, row, x_bindings);
        
        Properties y_bindings = ExpressionUtils.createBindings();
        Object vy = _evalY.eval(rowId, row, y_bindings);
        
		if (ExpressionUtils.isError(vx) || ExpressionUtils.isError(vy)) {
            return state;
        } else if (ExpressionUtils.isNonBlankData(vx) && ExpressionUtils.isNonBlankData(vy)) {
            if (vx instanceof Number && vy instanceof Number) {
                double dx = ((Number) vx).doubleValue();
                double dy = ((Number) vy).doubleValue();
                if (!Double.isInfinite(dx) && 
                        !Double.isNaN(dx) && 
                        !Double.isInfinite(dy) && 
                        !Double.isNaN(dy)) {
                	return state.addValue(dx, dy, inView);
                }
            }
        }
		// in all other cases, ignore the values
        return state;
	}

	@Override
	public ScatterplotFacetState sum(ScatterplotFacetState first, ScatterplotFacetState second) {
		double[] newValuesX = Doubles.concat(first.getValuesX(), second.getValuesX());
		double[] newValuesY = Doubles.concat(first.getValuesY(), second.getValuesY());
		boolean[] inView = Booleans.concat(first.getInView(), second.getInView());
		
		return new ScatterplotFacetState(newValuesX, newValuesY, inView, first.getValuesCount() + second.getValuesCount());
	}

	@Override
	public RowFilter getRowFilter() {
        if (!_config.isNeutral() && 
                _evalX != null &&
                _evalY != null) 
        {
            return new ScatterplotRowFilter(_evalX, _evalY,
            		_config.minX, _config.maxX, _config.minY, _config.maxY,
            		_config.fromX, _config.toX, _config.fromY, _config.toY,
            		_config.dim_x, _config.dim_y, _config.size, _config.rotation);
        } else {
            return null;
        }
	}

	@Override
	public RecordFilter getRecordFilter() {
		RowFilter rowFilter = getRowFilter();
        if (rowFilter == null) {
            return null;
        }
        return new AnyRowRecordFilter(rowFilter);
	}

}
