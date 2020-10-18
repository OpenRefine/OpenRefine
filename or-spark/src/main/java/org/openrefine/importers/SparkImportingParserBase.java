package org.openrefine.importers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.openrefine.ProjectMetadata;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.SparkDatamodelRunner;
import org.openrefine.model.SparkGridState;
import org.openrefine.util.JSONUtilities;

import com.fasterxml.jackson.databind.node.ObjectNode;

import scala.Tuple2;

public abstract class SparkImportingParserBase extends HDFSImporter {

	protected SparkImportingParserBase(SparkDatamodelRunner runner) {
		super(runner);
	}
	
    /**
     * Parses a RDD from a Spark URI. This results in a grid of cell values, potentially null.
     * 
     * @param metadata
     * @param job
     * @param fileSource
     * @param sparkURI
     * @param limit
     * @param options
     * @return
     */
	protected abstract JavaPairRDD<Long, List<Serializable>> parseRawCells(ProjectMetadata metadata, ImportingJob job,
			String fileSource, String sparkURI, long limit, ObjectNode options);
	
    @Override
    public GridState parseOneFile(ProjectMetadata metadata, ImportingJob job, String fileSource,
                String sparkURI, long limit, ObjectNode options) throws Exception {
        int ignoreLines = Math.max(JSONUtilities.getInt(options, "ignoreLines", -1), 0);
        int headerLines = Math.max(JSONUtilities.getInt(options, "headerLines", 1), 0);
        int skipDataLines = Math.max(JSONUtilities.getInt(options, "skipDataLines", 0), 0);
        String[] optionColumnNames = JSONUtilities.getStringArray(options, "columnNames");
        long limit2 = JSONUtilities.getLong(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }
        
        JavaPairRDD<Long, List<Serializable>> rawCells = parseRawCells(metadata, job, fileSource, sparkURI, limit2, options);
        
        // Parse column names
        List<String> columnNames = new ArrayList<>();
        
        if (optionColumnNames.length > 0) {
        	for (int i = 0; i != optionColumnNames.length; i++) {
        		ImporterUtilities.appendColumnName(columnNames, i, optionColumnNames[i]);
        	}
        } else if (headerLines > 0) {
        	int numTake = ignoreLines + headerLines;
        	
        	List<Tuple2<Long,List<Serializable>>> firstLines = rawCells.take(numTake);
        	for(int i = ignoreLines; i < firstLines.size(); i++) {
	        	List<Serializable> headerLine = firstLines.get(i)._2;
				for(int j = 0; j != headerLine.size(); j++) {
					Serializable cellValue = headerLine.get(j);
	        		ImporterUtilities.appendColumnName(columnNames, j, cellValue == null ? "" : cellValue.toString());
	        	}
        	}        	
        } else {
        	// Take the first line of non-ignored data and use it to deduce the number of columns
        	int numTake = ignoreLines + skipDataLines + 1;
        	List<Tuple2<Long,List<Serializable>>> firstLines = rawCells.take(numTake);
        	if (firstLines.size() == numTake) {
        		int size = firstLines.get(numTake-1)._2.size();
				for(int i = 0; i != size; i++) {
        			ImporterUtilities.appendColumnName(columnNames, i, "");
        			// Empty string will later be translated to "Column i"
        		}
        	}
        }
        
        ColumnModel columnModel = ImporterUtilities.setupColumns(columnNames);
        
		JavaPairRDD<Long, Row> grid = tail(rawCells, ignoreLines + headerLines + skipDataLines)
        		.mapValues(toRow);
		return new SparkGridState(columnModel, grid, Collections.emptyMap(), (SparkDatamodelRunner)runner);
    }

    /**
     * Skips the first n lines in a RDD indexed by row ids.
     * @return
     */
    protected static JavaPairRDD<Long, List<Serializable>> tail(JavaPairRDD<Long, List<Serializable>> rdd, long skip) {
		return rdd.filter(new Function<Tuple2<Long, List<Serializable>>, Boolean>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Boolean call(Tuple2<Long, List<Serializable>> v1) throws Exception {
				return v1._1 >= skip;
			}
			
		}).mapToPair(new PairFunction<Tuple2<Long, List<Serializable>>, Long,List<Serializable>>(){
			private static final long serialVersionUID = 1L;

			@Override
			public Tuple2<Long, List<Serializable>> call(Tuple2<Long, List<Serializable>> v1) throws Exception {
				return new Tuple2<Long, List<Serializable>>(v1._1 + skip, v1._2);
			}
			
		});
    }
    
    protected static Function<List<Serializable>, Row> toRow = new Function<List<Serializable>, Row>() {

		private static final long serialVersionUID = 1L;

		@Override
		public Row call(List<Serializable> v1) throws Exception {
			List<Cell> cells = v1.stream()
					.map(v -> v == null ? null : new Cell(v, null))
					.collect(Collectors.toList());
			return new Row(cells);
		}
    	
    };
}
