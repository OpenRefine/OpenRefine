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

package org.openrefine.model.changes;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.ModelException;
import org.openrefine.model.Row;
import org.openrefine.model.RowCellMapper;
import org.openrefine.model.RowMapper;

public abstract class ColumnAdditionChange extends RowMapChange {
    final protected String          _columnName;
    final protected int             _columnIndex;
    
    public ColumnAdditionChange(String columnName, int columnIndex, EngineConfig config) {
    	super(config);
        _columnName = columnName;
        _columnIndex = columnIndex;
    }
    
    public abstract RowCellMapper getRowCellMapper(ColumnModel columnModel);

	@Override
	public boolean isImmediate() {
		return true;
	}
	
	@Override
	public ColumnModel getNewColumnModel(GridState grid, ChangeContext context) throws DoesNotApplyException {
		ColumnMetadata column = new ColumnMetadata(_columnName);
		try {
			return grid.getColumnModel().insertColumn(_columnIndex, column);
		} catch(ModelException e) {
			throw new Change.DoesNotApplyException(
					String.format("A column with name '{}' cannot be added as the name conflicts with an existing column", _columnName));
		}
	}

	@Override
	public RowMapper getPositiveRowMapper(GridState state, ChangeContext context) {
		return wrapMapper(getRowCellMapper(state.getColumnModel()), _columnIndex);
	}
	
	public static RowMapper wrapMapper(RowCellMapper mapper, int columnIndex) {
		return new RowMapper() {
			private static final long serialVersionUID = 1L;

			@Override
			public Row call(long rowId, Row row) {
				Cell cell = mapper.apply(rowId, row);
				return row.insertCell(columnIndex, cell);
			}
			
		};
	}

}
