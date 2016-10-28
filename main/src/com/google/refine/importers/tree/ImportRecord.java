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
     * A List implementation to match the characteristics needed by the 
     * import process.  It's optimized for a relatively small number of 
     * contiguous records at a potentially large offset from zero.
     * <p>
     * I suspect it's usually only a single row, but we support more, just
     * not as efficiently.  Depending on the behavior of the ColumnGroups
     * this may not be necessary at all, but I don't fully understand what it
     * does, so we'll just put this hack in place for now.
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
                    for (int i = size(); i > delta; i --) {
                        set(i,get(i-delta));
                    } // Null unused entries
                    for (int i = 0; i < delta; i++) {
                        set(i,null);
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