package org.openrefine.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TestingChangeData<T extends Serializable> implements ChangeData<T> {
    
    private Map<Long, T> data;
    
    public TestingChangeData(Map<Long, T> data) {
        this.data = data;
    }

    @Override
    public T get(long rowId) {
        return data.get(rowId);
    }

    @Override
    @JsonIgnore
    public DatamodelRunner getDatamodelRunner() {
        return new TestingDatamodelRunner();
    }

    @Override
    public void saveToFile(File file) throws IOException {
        
        file.mkdirs();
        File partFile = new File(file, "part-00000.gz");
        FileOutputStream fos = null;
        GZIPOutputStream gos = null;
        
        try {
            fos = new FileOutputStream(partFile);
            gos = new GZIPOutputStream(fos);
            for(IndexedData<T> row : this) {
                ParsingUtilities.mapper.writeValue(gos, row);
                gos.write('\n');
            }
        } finally {
            if (gos != null) {
                gos.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Override
    public Iterator<IndexedData<T>> iterator() {
        List<IndexedData<T>> indexed = data
                .entrySet()
                .stream()
                .map(e -> new IndexedData<T>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        indexed.sort(new Comparator<IndexedData<T>>() {

            @Override
            public int compare(IndexedData<T> arg0, IndexedData<T> arg1) {
                return (int)(arg0.getId() - arg1.getId());
            }
            
        });
        return indexed.iterator();
    }

}
