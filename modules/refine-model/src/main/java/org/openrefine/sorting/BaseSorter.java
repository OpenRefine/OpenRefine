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

package org.openrefine.sorting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openrefine.expr.EvalError;
import org.openrefine.model.Grid;
import org.openrefine.sorting.Criterion.KeyMaker;

/**
 * Instantiates a sorting configuration on a particular grid, to allow comparison of rows or records
 *
 * @param <T>
 *            the type of objects to compare
 */
abstract public class BaseSorter<T> implements Comparator<T>, Serializable {

    private static final long serialVersionUID = 6499317992492625664L;
    protected List<Criterion> _criteria;
    protected KeyMaker[] _keyMakers;
    protected List<ComparatorWrapper> _comparatorWrappers;

    public BaseSorter(Grid state, SortingConfig config) {
        _criteria = config.getCriteria();
        int count = _criteria.size();
        _keyMakers = new KeyMaker[count];
        _comparatorWrappers = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            _keyMakers[i] = _criteria.get(i).createKeyMaker(state.getColumnModel());
            _comparatorWrappers.add(new ComparatorWrapper(i));
        }
    }

    public boolean hasCriteria() {
        return _criteria != null && _criteria.size() > 0;
    }

    public class ComparatorWrapper implements Serializable {

        private static final long serialVersionUID = 3091189718283536100L;
        final public int criterionIndex;
        final protected int multiplier;

        public ComparatorWrapper(int criterionIndex) {
            this.criterionIndex = criterionIndex;
            this.multiplier = _criteria.get(criterionIndex).reverse ? -1 : 1;
        }

        public int compare(T o1, T o2) {
            Criterion c = _criteria.get(criterionIndex);
            Serializable key1 = makeKey(_keyMakers[criterionIndex], c, o1);
            Serializable key2 = makeKey(_keyMakers[criterionIndex], c, o2);

            if (key1 == null) {
                if (key2 == null) {
                    return 0;
                } else if (key2 instanceof EvalError) {
                    return c.blankPosition - c.errorPosition;
                } else {
                    return c.blankPosition;
                }
            } else if (key1 instanceof EvalError) {
                if (key2 == null) {
                    return c.errorPosition - c.blankPosition;
                } else if (key2 instanceof EvalError) {
                    return 0;
                } else {
                    return c.errorPosition;
                }
            } else {
                if (key2 == null) {
                    return -c.blankPosition;
                } else if (key2 instanceof EvalError) {
                    return -c.errorPosition;
                } else {
                    return _keyMakers[criterionIndex].compareKeys(key1, key2) * multiplier;
                }
            }
        }
    }

    abstract protected Serializable makeKey(
            KeyMaker keyMaker, Criterion c, T o);

    protected Serializable[] makeKeys(T o) {
        Serializable[] keys = new Serializable[_keyMakers.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = makeKey(_keyMakers[i], _criteria.get(i), o);
        }
        return keys;
    }

    @Override
    public int compare(T o1, T o2) {
        int c = 0;
        for (int i = 0; c == 0 && i < _comparatorWrappers.size(); i++) {
            c = _comparatorWrappers.get(i).compare(o1, o2);
        }
        return c;
    }
}
