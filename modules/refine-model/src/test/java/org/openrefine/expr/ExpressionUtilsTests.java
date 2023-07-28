/*

Copyright 2017, Owen Stephens
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
    * Neither the name of the copyright holder nor the names of its
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

package org.openrefine.expr;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.Record;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Row;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpressionUtilsTests {

    // -----------------tests------------

    @Test
    public void testSameValueTrue() {
        Assert.assertTrue(ExpressionUtils.sameValue(null, null));
        Assert.assertTrue(ExpressionUtils.sameValue("", ""));
        Assert.assertTrue(ExpressionUtils.sameValue("one", "one"));
        Assert.assertTrue(ExpressionUtils.sameValue(1, 1));
        Assert.assertTrue(ExpressionUtils.sameValue(1.0, 1.00));
        Assert.assertTrue(ExpressionUtils.sameValue(true, true));
    }

    @Test
    public void testSameValueFalse() {
        Assert.assertFalse(ExpressionUtils.sameValue("", null));
        Assert.assertFalse(ExpressionUtils.sameValue(null, ""));
        Assert.assertFalse(ExpressionUtils.sameValue("one", "two"));
        Assert.assertFalse(ExpressionUtils.sameValue(1, 2));
        Assert.assertFalse(ExpressionUtils.sameValue(1, 1.0));
        Assert.assertFalse(ExpressionUtils.sameValue(true, false));
    }

    @Test
    public void testDependsOnPendingValuesRow() {
        Evaluable evaluable = mock(Evaluable.class);
        when(evaluable.getColumnDependencies("foo")).thenReturn(Collections.singleton("foo"));
        ColumnModel columnModel = new ColumnModel(Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar")));

        Row row = new Row(Arrays.asList(new Cell("a", null), Cell.PENDING_NULL));

        Assert.assertFalse(ExpressionUtils.dependsOnPendingValues(evaluable, "foo", columnModel, row, null));
    }

    @Test
    public void testDependsOnPendingValuesRecord() {
        Evaluable evaluable = mock(Evaluable.class);
        when(evaluable.getColumnDependencies("foo")).thenReturn(null);
        ColumnModel columnModel = new ColumnModel(Arrays.asList(new ColumnMetadata("foo"), new ColumnMetadata("bar")));

        Row row = new Row(Arrays.asList(new Cell("a", null), Cell.PENDING_NULL));
        Record record = new Record(4L, Arrays.asList(row));

        Assert.assertTrue(ExpressionUtils.dependsOnPendingValues(evaluable, "foo", columnModel, row, record));
    }
}