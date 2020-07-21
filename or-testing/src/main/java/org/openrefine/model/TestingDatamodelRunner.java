package org.openrefine.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.openrefine.model.GridState.Metadata;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;
import org.testng.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * A massively inefficient but very simple implementation of the datamodel,
 * for testing purposes.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TestingDatamodelRunner implements DatamodelRunner {

    /**
     * Asserts that an object is serializable using Java serialization.
     * @param obj
     */
    protected static void ensureSerializable(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
        } catch(IOException e) {
            e.printStackTrace();
            Assert.fail("Object not serializable");
        }
    }

    @Override
    public GridState loadGridState(File path) throws IOException {
        File gridPath = new File(path, GridState.GRID_PATH);
        File metadataPath = new File(path, GridState.METADATA_PATH);
        
        List<Row> rows = new ArrayList<>();
        
        // list the files in the directory
        List<File> files = sortedListFiles(gridPath);
        for(File partitionFile : files) {
            if (partitionFile.getName().startsWith("part")) {
                LineNumberReader ln = null;
                GZIPInputStream gis = null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(partitionFile);
                    gis = new GZIPInputStream(fis);
                    ln = new LineNumberReader(new InputStreamReader(gis));
                    Iterator<String> iterator = ln.lines().iterator();
                    while(iterator.hasNext()) {
                        String line = iterator.next().trim();
                        if (line.isEmpty()) {
                            break;
                        }
                        rows.add(ParsingUtilities.mapper.readValue(line, IndexedRow.class).getRow());
                    }
                } finally {
                    if (ln != null) {
                        ln.close();
                    }
                    if (gis != null) {
                        gis.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                }
            }
        }
        
        Metadata metadata = ParsingUtilities.mapper.readValue(metadataPath, Metadata.class);
        return new TestingGridState(metadata.columnModel, rows, metadata.overlayModels);
    }
    
    @Override
    public <T extends Serializable> ChangeData<T> loadChangeData(File path, TypeReference<IndexedData<T>> expectedType) throws IOException {
        Map<Long, T> data = new HashMap<>();
        List<File> files = sortedListFiles(path);
        for(File partitionFile : files) {
            if (partitionFile.getName().startsWith("part")) {
                LineNumberReader ln = null;
                GZIPInputStream gis = null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(partitionFile);
                    gis = new GZIPInputStream(fis);
                    ln = new LineNumberReader(new InputStreamReader(gis));
                    Iterator<String> iterator = ln.lines().iterator();
                    while(iterator.hasNext()) {
                        String line = iterator.next().trim();
                        if (line.isEmpty()) {
                            break;
                        }
                        IndexedData<T> indexedData = ParsingUtilities.mapper.readValue(line, expectedType);
                        data.put(indexedData.getId(), indexedData.getData());
                    }
                } finally {
                    if (ln != null) {
                        ln.close();
                    }
                    if (gis != null) {
                        gis.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                }
            }
        }
        return new TestingChangeData<T>(data);
    }
    
    private List<File> sortedListFiles(File directory) {
        List<File> files = Arrays.asList(directory.listFiles());
        files.sort(new Comparator<File>() {

            @Override
            public int compare(File arg0, File arg1) {
                return arg0.getPath().compareTo(arg1.getPath());
            }
            
        });
        return files;
    }


    @Override
    public GridState create(ColumnModel columnModel, List<Row> rows, Map<String,OverlayModel> overlayModels) {
        return new TestingGridState(columnModel, rows, overlayModels);
    }

    @Override
    public FileSystem getFileSystem() throws IOException {
        return new LocalFileSystem();
    }

    @Override
    public <T extends Serializable> ChangeData<T> create(List<IndexedData<T>> changeData) {
        return new TestingChangeData<T>(changeData.stream().collect(Collectors.toMap(IndexedData::getId, IndexedData::getData)));
    }

}
