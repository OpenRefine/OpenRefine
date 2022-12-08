
package org.openrefine.model.local.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Utility class to represent the query tree associated with a PLL.
 */
public class QueryTree {

    protected final long id;
    protected final String name;
    protected final List<QueryTree> children;

    /**
     * Constructs a node of a query tree.
     *
     * @param id
     *            the id of the PLL
     * @param name
     *            a short description of what the PLL does
     * @param children
     *            the list of sub-PLLs this PLL builds on
     */
    public QueryTree(long id, String name, QueryTree... children) {
        this.id = id;
        this.name = name;
        this.children = Arrays.asList(children);
    }

    protected void write(Writer writer, String prefix, boolean lastChild) {
        try {
            String connector = lastChild ? "└" : "├";
            writer.write(String.format("%s%s─%s (%d)\n", prefix, connector, name, id));
        } catch (IOException e) {
            // should not happen since we are just writing in memory
            throw new RuntimeException(e);
        }
        for (int i = 0; i != children.size(); i++) {
            children.get(i).write(writer, prefix + (lastChild ? "  " : "│ "), i == children.size() - 1);
        }
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        write(writer, "", true);
        return writer.toString().strip();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryTree queryTree = (QueryTree) o;
        return id == queryTree.id && Objects.equals(name, queryTree.name) && Objects.equals(children, queryTree.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, children);
    }
}
