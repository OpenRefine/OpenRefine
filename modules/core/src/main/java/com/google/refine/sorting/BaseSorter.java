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

package com.google.refine.sorting;

import java.util.List;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Project;
import com.google.refine.sorting.Criterion.KeyMaker;

abstract public class BaseSorter {

    protected Criterion[] _criteria;
    protected KeyMaker[] _keyMakers;
    protected ComparatorWrapper[] _comparatorWrappers;
    protected List<Object[]> _keys;

    public class ComparatorWrapper {

        final public int criterionIndex;
        final protected int multiplier;

        public ComparatorWrapper(int criterionIndex) {
            this.criterionIndex = criterionIndex;
            this.multiplier = _criteria[criterionIndex].reverse ? -1 : 1;
        }

        public Object getKey(Project project, Object o, int index) {
            while (index >= _keys.size()) {
                _keys.add(null);
            }

            Object[] keys = _keys.get(index);
            if (keys == null) {
                keys = makeKeys(project, o, index);
                _keys.set(index, keys);
            }
            return keys[criterionIndex];
        }

        public int compare(Project project, Object o1, int i1, Object o2, int i2) {
            Criterion c = _criteria[criterionIndex];
            Object key1 = getKey(project, o1, i1);
            Object key2 = getKey(project, o2, i2);

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

    public void initializeFromConfig(Project project, SortingConfig config) {
        _criteria = config.getCriteria();
        int count = _criteria.length;
        _keyMakers = new KeyMaker[count];
        _comparatorWrappers = new ComparatorWrapper[count];

        for (int i = 0; i < count; i++) {
            _keyMakers[i] = _criteria[i].createKeyMaker();
            _comparatorWrappers[i] = new ComparatorWrapper(i);
        }
    }

    public boolean hasCriteria() {
        return _criteria != null && _criteria.length > 0;
    }

    abstract protected Object makeKey(
            Project project, KeyMaker keyMaker, Criterion c, Object o, int index);

    protected Object[] makeKeys(Project project, Object o, int index) {
        Object[] keys = new Object[_keyMakers.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = makeKey(project, _keyMakers[i], _criteria[i], o, index);
        }
        return keys;
    }

    protected int compare(Project project, Object o1, int i1, Object o2, int i2) {
        int c = 0;
        for (int i = 0; c == 0 && i < _comparatorWrappers.length; i++) {
            c = _comparatorWrappers[i].compare(project, o1, i1, o2, i2);
        }
        return c;
    }
}
