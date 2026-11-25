
package com.google.refine.grel.ast;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.util.JsonValueConverter;
import com.google.refine.grel.Scanner;

/**
 * An AST node which represents a spread argument, such as {@code a...} in {@code or(a...)}. It may only appear among
 * the arguments of a {@link FunctionCallExpr} (including the elements of an {@link ArrayExpr}), which replaces it by
 * the elements of the array it evaluates to.
 */
public class SpreadExpr extends GrelExpr {

    protected final Evaluable inner;

    /**
     * @param inner
     *            the expression whose elements are spread
     */
    public SpreadExpr(Evaluable inner) {
        this.inner = inner;
    }

    /**
     * Evaluates the expression whose elements are spread. Splicing those elements into the argument list is done by the
     * enclosing {@link FunctionCallExpr}, using {@link #toElements(Object)}.
     */
    @Override
    public Object evaluate(Properties bindings) {
        return inner.evaluate(bindings);
    }

    /**
     * Lists the elements of a value which can be spread: a Java array, a collection or a JSON array.
     *
     * @param value
     *            the value to spread
     * @return its elements, or null if the value cannot be spread
     */
    public static Object[] toElements(Object value) {
        if (value instanceof Object[]) {
            return (Object[]) value;
        } else if (value != null && value.getClass().isArray()) { // array of primitives
            Object[] elements = new Object[Array.getLength(value)];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = Array.get(value, i);
            }
            return elements;
        } else if (value instanceof Collection<?>) {
            return ((Collection<?>) value).toArray();
        } else if (value instanceof ArrayNode) {
            ArrayNode a = (ArrayNode) value;
            Object[] elements = new Object[a.size()];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = JsonValueConverter.convert(a.get(i));
            }
            return elements;
        }
        return null;
    }

    @Override
    public Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        return inner.getColumnDependencies(baseColumn);
    }

    @Override
    public Evaluable renameColumnDependencies(Map<String, String> substitutions) {
        return new SpreadExpr(inner.renameColumnDependencies(substitutions));
    }

    @Override
    public String toString() {
        return inner + Scanner.SPREAD;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SpreadExpr other = (SpreadExpr) obj;
        return Objects.equals(inner, other.inner);
    }

}
