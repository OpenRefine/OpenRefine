/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2018 Antonin Delpeuch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.schema;

import java.util.Objects;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WbRankConstant implements WbExpression<StatementRank> {

    private String rank;

    @JsonCreator
    public WbRankConstant(@JsonProperty("rank") String rank) {
        // no validation needed; it is "normal" by default
        this.rank = rank;
    }

    @Override
    public StatementRank evaluate(ExpressionContext ctxt) {
        // didn't find a function to convert string to StatementRank
        if (Objects.equals(this.rank, "preferred")) {
            return StatementRank.PREFERRED;
        } else if (Objects.equals(this.rank, "normal")) {
            return StatementRank.NORMAL;
        } else {
            return StatementRank.DEPRECATED;
        }
    }

    @JsonProperty("rank")
    public String getValue() {
        return rank;
    }

    @Override
    public boolean equals(Object other) {
        if(other == null || !WbStringConstant.class.isInstance(other)) {
            return false;
        }
        return rank.equals(((WbStringConstant)other).getValue());
    }

    @Override
    public int hashCode() {
        return rank.hashCode();
    }
}
