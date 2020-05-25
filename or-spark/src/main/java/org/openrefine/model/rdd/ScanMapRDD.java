
package org.openrefine.model.rdd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.spark.Partition;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.rdd.RDD;
import scala.collection.Iterator;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.reflect.ClassTag;

/**
 * The scan map operation on a RDD provides a way to perform a map on each elements where the mapped element can depend
 * on a state accumulated over the previous elements. This is made scalable using the assumption that this state is
 * accumulated using an associative and unital combining function.
 * 
 * This is a generalization of the zipWithIndex function, where the accumulated state is the number of elements seen so
 * far.
 * 
 * @author Antonin Delpeuch
 *
 * @param <S>
 *            the type of state used by the mapper
 * @param <T>
 *            the type of elements of the original RDD
 * @param <U>
 *            the type of elements of the new RDD
 */
public class ScanMapRDD<S extends Serializable, T, U> extends RDD<U> implements Serializable {

    private static final long serialVersionUID = 410341151696633023L;

    private final Function2<S, S, S> combine;
    private final Function2<S, T, U> map;
    private final Function<T, S> feed;

    private final ClassTag<T> tClassTag;
    private final ClassTag<U> uClassTag;

    protected final List<S> partitionStates;

    /**
     * Constructor.
     * 
     * @param parent
     *            the RDD to which the scan map operation is applied
     * @param feed
     *            the function to turn an element into a state
     * @param combine
     *            the associative function to combine states
     * @param map
     *            the map function taking the accumulated state, element, mapping it to the new element
     * @param initialState
     *            a neutral element for the combine
     * @param uClassTag
     * @param tClassTag
     * @param sClasTag
     */
    public ScanMapRDD(
            RDD<T> parent,
            Function<T, S> feed,
            Function2<S, S, S> combine,
            Function2<S, T, U> map,
            S initialState,
            ClassTag<U> uClassTag,
            ClassTag<T> tClassTag,
            ClassTag<S> sClasTag) {
        super(parent, uClassTag);
        this.tClassTag = tClassTag;
        ensureSerializable(tClassTag);
        this.uClassTag = uClassTag;
        ensureSerializable(uClassTag);
        this.combine = combine;
        ensureSerializable(combine);
        this.map = map;
        ensureSerializable(map);
        this.feed = feed;
        ensureSerializable(feed);
        partitionStates = computePartitionStates(parent, initialState, feed, combine, sClasTag);
        ensureSerializable(this);
    }

    protected static void ensureSerializable(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Object not serializable");
        }
    }

    /**
     * Converts to a Java RDD
     */
    public JavaRDD<U> asJavaRDD() {
        return new JavaRDD<U>(this, uClassTag);
    }

    @SuppressWarnings("unchecked")
    public static <S, T> List<S> computePartitionStates(RDD<T> parent, S initialState, Function<T, S> feed, Function2<S, S, S> combine,
            ClassTag<S> stateClassTag) {
        int numPartitions = parent.getNumPartitions();
        List<S> partitionStates = Collections.singletonList(initialState);

        if (parent.getNumPartitions() > 1) {
            partitionStates = new ArrayList<>(parent.getNumPartitions());
            List<Object> partitionIds = new ArrayList<>(numPartitions - 1);
            for (int i = 0; i != numPartitions - 1; i++) {
                partitionIds.add(i);
            }
            Seq<Object> partitionIdObjs = JavaConverters.collectionAsScalaIterable(partitionIds).toSeq();
            // casting directly to K[] can fail
            Object[] objects = (Object[]) parent.context().runJob(
                    parent, computePartialProducts(initialState, feed, combine), partitionIdObjs, stateClassTag);

            S currentState = initialState;
            partitionStates.add(initialState);
            for (int i = 0; i != objects.length; i++) {
                try {
                    currentState = combine.call(currentState, (S) objects[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                partitionStates.add(currentState);
            }
        }
        return partitionStates;
    }

    protected static interface JobSignature<S, T> extends scala.Function2<TaskContext, Iterator<T>, S>, Serializable {

    };

    static <S, T> scala.Function2<TaskContext, Iterator<T>, S> computePartialProducts(
            S initialState, Function<T, S> feed, Function2<S, S, S> combine) {
        return new JobSignature<S, T>() {

            private static final long serialVersionUID = -4871119702625799657L;

            @Override
            public S apply(TaskContext context, Iterator<T> iterator) {
                S state = initialState;
                while (iterator.hasNext()) {
                    T elem = iterator.next();
                    S localState;
                    try {
                        localState = feed.call(elem);
                        state = combine.call(state, localState);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                return state;
            }
        };
    }

    @Override
    public Iterator<U> compute(Partition arg0, TaskContext context) {
        @SuppressWarnings("unchecked")
        ScanMapRDDPartition<S> partition = (ScanMapRDDPartition<S>) arg0;
        Iterator<T> origIter = this.firstParent(tClassTag).iterator(partition.prev, context);
        return new Iterator<U>() {

            S currentState = partition.initialState;

            @Override
            public boolean hasNext() {
                return origIter.hasNext();
            }

            @Override
            public U next() {
                T elem = origIter.next();
                U result = null;
                try {
                    result = map.call(currentState, elem);
                    currentState = combine.call(currentState, feed.call(elem));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            }

        };
    }

    @Override
    public Partition[] getPartitions() {
        Partition[] origPartitions = this.firstParent(elementClassTag()).getPartitions();
        Partition[] newPartitions = new Partition[origPartitions.length];

        for (int i = 0; i != origPartitions.length; i++) {
            newPartitions[i] = new ScanMapRDDPartition<S>(origPartitions[i], partitionStates.get(i));
        }
        return newPartitions;
    }

    protected static class ScanMapRDDPartition<S> implements Partition {

        private static final long serialVersionUID = 8256733549359673469L;

        private final Partition prev;
        protected final S initialState;

        ScanMapRDDPartition(Partition prev, S initialState) {
            this.prev = prev;
            this.initialState = initialState;
        }

        @Override
        public int index() {
            return prev.index();
        }

    }
}
