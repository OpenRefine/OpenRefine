
package org.openrefine.runners.testing;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import com.google.common.io.CountingInputStream;
import org.testng.Assert;

import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.model.*;
import org.openrefine.model.Grid.Metadata;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;

/**
 * A massively inefficient but very simple implementation of the datamodel, for testing purposes.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TestingRunner implements Runner {

    public TestingRunner() {

    }

    /**
     * Constructor provided to match the expected signature of runners.
     */
    public TestingRunner(RunnerConfiguration configuration) {

    }

    /**
     * Asserts that an object is serializable using Java serialization.
     * 
     * @param obj
     */
    protected static void ensureSerializable(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Object not serializable");
        }
    }

    /**
     * Serializes an object and then deserializes it back. This ensures that the object is serializable and is useful to
     * simulate distributed environments (for instance to null out all transient fields).
     * 
     * @param <T>
     * @param obj
     * @return
     */
    protected static <T> T serializeAndDeserialize(T obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Object not serializable");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Assert.fail("Object could not be deserialized");
        }
        return null; // unreachable
    }

    @Override
    public Grid loadGrid(File path) throws IOException {
        File gridPath = new File(path, Grid.GRID_PATH);
        File metadataPath = new File(path, Grid.METADATA_PATH);

        List<Row> rows = new ArrayList<>();

        // list the files in the directory
        List<File> files = sortedListFiles(gridPath);
        for (File partitionFile : files) {
            if (partitionFile.getName().startsWith("part")) {
                LineNumberReader ln = null;
                GZIPInputStream gis = null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(partitionFile);
                    gis = new GZIPInputStream(fis);
                    ln = new LineNumberReader(new InputStreamReader(gis));
                    Iterator<String> iterator = ln.lines().iterator();
                    while (iterator.hasNext()) {
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
        return new TestingGrid(metadata.columnModel, rows, metadata.overlayModels);
    }

    @Override
    public <T> ChangeData<T> loadChangeData(File path, ChangeDataSerializer<T> serializer) throws IOException {
        Map<Long, T> data = new HashMap<>();
        List<File> files = sortedListFiles(path);
        for (File partitionFile : files) {
            if (partitionFile.getName().startsWith("part")) {
                LineNumberReader ln = null;
                GZIPInputStream gis = null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(partitionFile);
                    gis = new GZIPInputStream(fis);
                    ln = new LineNumberReader(new InputStreamReader(gis));
                    Iterator<String> iterator = ln.lines().iterator();
                    while (iterator.hasNext()) {
                        String line = iterator.next().trim();
                        if (line.isEmpty()) {
                            break;
                        }
                        IndexedData<T> indexedData = IndexedData.read(line, serializer);
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
    public Grid create(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        return new TestingGrid(columnModel, rows, overlayModels);
    }

    @Override
    public <T> ChangeData<T> create(List<IndexedData<T>> changeData) {
        return new TestingChangeData<T>(changeData.stream()
                .filter(id -> id.getData() != null)
                .collect(Collectors.toMap(IndexedData::getId, IndexedData::getData)));
    }

    @Override
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding) throws IOException {
        return loadTextFile(path, progress, GRID_ENCODING, Long.MAX_VALUE);
    }

    @Override
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding, long limit) throws IOException {
        LineNumberReader reader = null;
        try {
            File file = new File(path);
            CountingInputStream inputStream = new CountingInputStream(new FileInputStream(file));
            progress.readingFile(file.getName(), inputStream.getCount());
            reader = new LineNumberReader(new InputStreamReader(inputStream));
            List<Row> rows = reader.lines()
                    .map(line -> new Row(Collections.singletonList(new Cell(line, null))))
                    .limit(limit)
                    .collect(Collectors.toList());
            progress.readingFile(file.getName(), inputStream.getCount());

            ColumnModel columnModel = new ColumnModel(Collections.singletonList(new ColumnMetadata("Column")));
            return new TestingGrid(columnModel, rows, Collections.emptyMap());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    @Override
    public boolean supportsProgressReporting() {
        return true;
    }

}
