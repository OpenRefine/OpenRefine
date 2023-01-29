
package org.openrefine.runners.local.pll;

/**
 * A class of pairs, similar to scala's Tuple2.
 * 
 * @author Antonin Delpeuch
 *
 * @param <U>
 * @param <V>
 */
public class Tuple2<U, V> {

    private final U key;
    private final V value;

    public Tuple2(U left, V right) {
        this.key = left;
        this.value = right;
    }

    public U getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public static <U, V> Tuple2<U, V> of(U key, V value) {
        return new Tuple2<U, V>(key, value);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)",
                key == null ? "null" : key.toString(),
                value == null ? "null" : value.toString());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Tuple2<?, ?>)) {
            return false;
        }
        Tuple2<?, ?> tuple = (Tuple2<?, ?>) other;
        return (((key == null && tuple.key == null) || key.equals(tuple.key)) &&
                ((value == null && tuple.value == null) || value.equals(tuple.value)));
    }

    @Override
    public int hashCode() {
        return key.hashCode() + 11 * value.hashCode();
    }
}
