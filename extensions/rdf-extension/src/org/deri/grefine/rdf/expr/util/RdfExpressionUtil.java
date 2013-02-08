package org.deri.grefine.rdf.expr.util;

import java.util.Properties;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Row;

public class RdfExpressionUtil {

	public static Object evaluate(Evaluable eval,Properties bindings,Row row,int rowIndex,String columnName,int cellIndex){
		Cell cell; 
		if(cellIndex<0){
         	cell= new Cell(rowIndex,null);
         }else{
         	cell= row.getCell(cellIndex);
         }
        ExpressionUtils.bind(bindings, row, rowIndex, columnName, cell);
		return eval.evaluate(bindings);
	}
}
