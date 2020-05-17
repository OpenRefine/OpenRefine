
package org.openrefine.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.testng.Assert;

import org.openrefine.model.GridState.Metadata;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;

/**
 * A massively inefficient but very simple implementation of the datamodel, for testing purposes.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TestingDatamodelRunner implements DatamodelRunner {

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

    @Override
    public GridState loadGridState(File path) throws IOException {
        File gridPath = new File(path, GridState.GRID_PATH);
        File metadataPath = new File(path, GridState.METADATA_PATH);

        List<Row> rows = new ArrayList<>();

        // list the files in the directory
        List<File> files = Arrays.asList(gridPath.listFiles());
        files.sort(new Comparator<File>() {

            @Override
            public int compare(File arg0, File arg1) {
                return arg0.getPath().compareTo(arg1.getPath());
            }

        });
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
                        String line = iterator.next().strip();
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
    public GridState create(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        return new TestingGridState(columnModel, rows, overlayModels);
    }

    @Override
    public FileSystem getFileSystem() throws IOException {
        return new LocalFileSystem();
    }

}
