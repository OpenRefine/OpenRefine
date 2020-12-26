
package org.openrefine.operations.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.openrefine.operations.column.ColumnSplitOperation.Mode;

/**
 * Provides different ways to split a cell value into multiple strings.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface CellValueSplitter extends Serializable {

    public List<String> split(String source);

    /**
     * Constructs a CellValueSplitter according to the supplied settings.
     * 
     * @param mode
     *            whether to split by separator or fixed lengths
     * @param separator
     *            the separator to use (set to null if using lengths)
     * @param regex
     *            whether to interpret the separator as a regular expression
     * @param fieldLengths
     *            the lengths of the fields to extract (set to null if using a separator)
     * @param maxColumns
     *            the maximum number of values to extract (ignored for lengths)
     */
    public static CellValueSplitter construct(Mode mode, String separator, Boolean regex, int[] fieldLengths,
            Integer maxColumns) {
        if (Mode.Lengths.equals(mode)) {
            return CellValueSplitter.splitByLengths(fieldLengths);
        } else {
            if (regex) {
                Pattern pattern = Pattern.compile(separator, Pattern.UNICODE_CHARACTER_CLASS);
                return CellValueSplitter.splitByRegex(pattern, maxColumns == null ? 0 : maxColumns);
            } else {
                return CellValueSplitter.splitBySeparator(separator, maxColumns == null ? 0 : maxColumns);
            }
        }
    }

    public static CellValueSplitter splitByLengths(int[] lengths) {
        return new CellValueSplitter() {

            private static final long serialVersionUID = -8087516195285863794L;

            @Override
            public List<String> split(String source) {
                List<String> results = new ArrayList<>(lengths.length + 1);

                int lastIndex = 0;
                for (int length : lengths) {
                    int from = lastIndex;
                    int to = Math.min(from + length, source.length());

                    results.add(source.substring(from, to));

                    lastIndex = to;
                }

                return results;
            }

        };
    }

    public static CellValueSplitter splitBySeparator(String separator, int maxColumns) {
        return new CellValueSplitter() {

            private static final long serialVersionUID = 5678119132735565975L;

            @Override
            public List<String> split(String source) {
                return Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(source, separator, 0));
            }

        };
    }

    public static CellValueSplitter splitByRegex(Pattern regex, int maxColumns) {
        return new CellValueSplitter() {

            private static final long serialVersionUID = -6979838040900570895L;

            @Override
            public List<String> split(String source) {
                String[] split = regex.split(source);
                return Arrays.asList(split);
            }

        };

    }
}
