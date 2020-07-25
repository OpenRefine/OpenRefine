package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.spark.api.java.JavaPairRDD;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonProcessingException;

import scala.Tuple2;

/**
 * Stores change data in a rowid-indexed RDD.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class SparkChangeData<T extends Serializable> implements ChangeData<T> {
    
    private final JavaPairRDD<Long, T> data;
    private final SparkDatamodelRunner runner;
    
    public SparkChangeData(JavaPairRDD<Long, T> data, SparkDatamodelRunner runner) {
        this.data = data;
        this.runner = runner;
    }
    
    public JavaPairRDD<Long, T> getData() {
        return data;
    }

    @Override
    public Iterator<IndexedData<T>> iterator() {
        return data.map(tuple -> new IndexedData<T>(tuple._1, tuple._2)).toLocalIterator();
    }

    @Override
    public T get(long rowId) {
        List<T> rows = data.lookup(rowId);
        if (rows.size() == 0) {
            return null;
        } else if (rows.size() > 1){
            throw new IllegalStateException(String.format("Found %d change data elements at index %d", rows.size(), rowId));
        } else {
            return rows.get(0);
        }
    }

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return runner;
    }

    @Override
    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException {
        data.map(r -> serializeIndexedData(serializer, r)).saveAsTextFile(file.getAbsolutePath(), GzipCodec.class);
    }
    
    protected static <T extends Serializable> String serializeIndexedData(ChangeDataSerializer<T> serializer, Tuple2<Long, T> data) throws IOException {
        String serialized = (new IndexedData<T>(data._1, data._2)).writeAsString(serializer);
        return serialized;
    }

}
