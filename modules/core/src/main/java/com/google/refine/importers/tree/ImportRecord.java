/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.importers.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.refine.model.Cell;

/**
 * A record describes a data element in a tree-structure
 *
 */
public class ImportRecord {

    public List<List<Cell>> rows = new BasedList<List<Cell>>();

    /**
     * A List implementation to match the characteristics needed by the import process. It's optimized for a relatively
     * small number of contiguous records at a potentially large offset from zero.
     * <p>
     * I suspect it's usually only a single row, but we support more, just not as efficiently. Depending on the behavior
     * of the ColumnGroups this may not be necessary at all, but I don't fully understand what it does, so we'll just
     * put this hack in place for now.
     * 
     * @param <T>
     */
    class BasedList<T> extends ArrayList<T> {

        private static final long serialVersionUID = 1L;
        int offset = Integer.MAX_VALUE;

        public T set(int index, T element) {
            rebase(index);
            extend(index);
            return super.set(index - offset, element);
        }

        public T get(int index) {
            if (offset == Integer.MAX_VALUE || index - offset > size() - 1) {
                return null;
            }
            return super.get(index - offset);
        }

        private void rebase(final int index) {
            if (index < offset) {
                if (offset < Integer.MAX_VALUE) {
                    int new_offset = Math.max(0, index - 10); // Leave some extra room
                    int delta = offset - new_offset;
                    // Ensure room at top
                    for (int i = 0; i < delta; i++) {
                        add(null);
                    }
                    // Shuffle up
                    for (int i = size(); i > delta; i--) {
                        set(i, get(i - delta));
                    } // Null unused entries
                    for (int i = 0; i < delta; i++) {
                        set(i, null);
                    }
                    offset = new_offset;
                } else {
                    offset = index;
                }
            }
        }

        private void extend(final int index) {
            int i = index - offset;
            while (i >= size()) {
                add(null);
            }
        }
    }
}
