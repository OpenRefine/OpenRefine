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

package org.openrefine.commands.column;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.commands.Command;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class GetColumnsInfoCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Project project = getProject(request);
            
            GridState grid = project.getCurrentGridState();
            AggregationState initial = new AggregationState();
            initial.statistics = grid.getColumnModel().getColumns().stream().map(c -> new ColumnStatistics(c.getName(), 0, 0)).collect(Collectors.toList());
            AggregationState aggregated = grid.aggregateRows(new Aggregator(), initial);
            
            respondJSON(response, aggregated);
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }
    
    private static class AggregationState implements Serializable {
		private static final long serialVersionUID = -7825020364579861122L;
		@JsonValue
		public List<ColumnStatistics> getStatistics() {
			return statistics;
		}
		public List<ColumnStatistics> statistics;
    }
    
    private static class ColumnStatistics implements Serializable {

		private static final long serialVersionUID = -8845292788196741719L;

		public ColumnStatistics(
    			String name,
    			long numericCount,
    			long otherCount) {
    		this.name = name;
    		this.numericCount = numericCount;
    		this.otherCount = otherCount;
    	}
    	@JsonProperty("name")
    	public final String name;
    	@JsonProperty("numeric_row_count")
    	public final long numericCount;
    	@JsonProperty("other_count")
    	public final long otherCount;
    	
    	@JsonProperty("is_numeric")
    	public boolean isNumeric() {
    		return numericCount > otherCount;
    	}

    	public ColumnStatistics sum(ColumnStatistics other) {
    		return new ColumnStatistics(
    				name,
    				numericCount + other.numericCount,
    				otherCount + other.otherCount);
    	}
    	
    	public ColumnStatistics withValue(Object value) {
    		if (value instanceof Number) {
    			return new ColumnStatistics(name, numericCount + 1, otherCount);
    		} else {
    			return new ColumnStatistics(name, numericCount, otherCount + 1);
    		}
    	}
    	
    }
    
    private static class Aggregator implements RowAggregator<AggregationState> {

		private static final long serialVersionUID = 6172712485208754636L;

		@Override
		public AggregationState sum(AggregationState first, AggregationState second) {
			AggregationState result = new AggregationState();
			result.statistics = new ArrayList<>();
			for(int i = 0; i != first.statistics.size(); i++) {
				result.statistics.add(first.statistics.get(i).sum(second.statistics.get(i)));
			}
			return result;
		}

		@Override
		public AggregationState withRow(AggregationState state, long rowId, Row row) {
			AggregationState result = new AggregationState();
			result.statistics = new ArrayList<>();
			for(int i = 0; i != state.statistics.size(); i++) {
				result.statistics.add(state.statistics.get(i).withValue(row.getCellValue(i)));
			}
			return result;
		}
    	
    }
}
