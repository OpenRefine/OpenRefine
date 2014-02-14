package com.google.refine.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectDataStore;
import com.google.refine.ProjectMetadata;
import com.google.refine.history.History;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;


public class FileProjectDataStore extends ProjectDataStore {
    final static Logger logger = LoggerFactory.getLogger("FileProjectDataStore");
    
    private File projectDir;
    private boolean deleted = false;
    
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
        
    public FileProjectDataStore(Project project, File workspaceDir, String projectDirSuffix) {
        super(project);
        
        projectDir = new File(workspaceDir, project.id + projectDirSuffix);
    }
    
    @Override
    public boolean save() throws IOException {
        writeLock.lock();
        
        try {
            if (deleted)
                return false;
            
            if (!projectDir.exists())
                projectDir.mkdir();
            
            File tempFile = new File(projectDir, "data.temp.zip");
            try {
                saveToFile(tempFile);
            } catch (IOException e) {
                logger.error("Could not save project. projectDir.exists():" + projectDir.exists() + " tempFile.exists():" + tempFile.exists(), e);
                logger.warn("Failed to save project {}", project.id);
                try {
                    tempFile.delete();
                } catch (Exception e2) {
                    // just ignore - file probably was never created.
                }
                throw e;
            }

            File file = new File(projectDir, "data.zip");
            File oldFile = new File(projectDir, "data.old.zip");

            if (file.exists()) {
                file.renameTo(oldFile);
            }

            tempFile.renameTo(file);
            if (oldFile.exists()) {
                oldFile.delete();
            }

            project.setLastSave();

            logger.info("Saved project '{}'", project.id);
        }
        finally {
            writeLock.unlock();
        }
        
        return true;
    }

    private void saveToFile(File file) throws IOException  {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            Pool pool = new Pool();

            out.putNextEntry(new ZipEntry("data.txt"));
            try {
                project.saveToOutputStream(out, pool);
            } finally {
                out.closeEntry();
            }

            out.putNextEntry(new ZipEntry("pool.txt"));
            try {
                pool.save(out);
            } finally {
                out.closeEntry();
            }
        } finally {
            out.close();
        }
    }
    
    @Override
    public boolean load() {
        readLock.lock();
        
        try {
            if (!projectDir.exists())
                return false;
            
            try {
                File file = new File(projectDir, "data.zip");
                if (file.exists()) {
                    loadFromFile(file);
                    return true;
                }
            } catch (Exception e) {
                logger.error(e.toString());
            }
    
            try {
                File file = new File(projectDir, "data.temp.zip");
                if (file.exists()) {
                    loadFromFile(file);
                    return true;
                }
            } catch (Exception e) {
                logger.error(e.toString());
            }
    
            try {
                File file = new File(projectDir, "data.old.zip");
                if (file.exists()) {
                    loadFromFile(file);
                    return true;
                }
            } catch (Exception e) {
                logger.error(e.toString());
            }
        } finally {
            readLock.unlock();
        }

        return false;
    }

    private void loadFromFile(File file) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        try {
            Pool pool = new Pool();
            ZipEntry poolEntry = zipFile.getEntry("pool.txt");
            if (poolEntry != null) {
                pool.load(zipFile.getInputStream(poolEntry));
            } // else, it's a legacy project file

            project.loadFromInputStream(zipFile.getInputStream(zipFile.getEntry("data.txt")), pool);
        } finally {
            zipFile.close();
        }
    }
    
    @Override
    public void delete() {
        writeLock.lock();
        
        try {
            if (deleted)
                return;
            
            if (projectDir.exists())
                deleteDir(projectDir);
            
            deleted = true;
        } finally {
            writeLock.unlock();
        }
    }

    private void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        
        dir.delete();
    }
    
    @Override
    public void exportProject(TarOutputStream tos) throws IOException {
        readLock.lock();
        
        try {
            if (!projectDir.exists())
                throw new DirectoryNotFoundException("Export failed: Project directory " + projectDir + " does not exist.");
        
            this.tarDir("", projectDir, tos);
        } finally {
            readLock.unlock();
        }
    }

    private void tarDir(String relative, File dir, TarOutputStream tos) throws IOException{
        File[] files = dir.listFiles();
        for (File file : files) {
            if (!file.isHidden()) {
                String path = relative + file.getName();

                if (file.isDirectory()) {
                    tarDir(path + File.separator, file, tos);
                } else {
                    TarEntry entry = new TarEntry(path);

                    entry.setMode(TarEntry.DEFAULT_FILE_MODE);
                    entry.setSize(file.length());
                    entry.setModTime(file.lastModified());

                    tos.putNextEntry(entry);

                    copyFile(file, tos);

                    tos.closeEntry();
                }
            }
        }
    }
    
    @Override
    public void importProject(InputStream inputStream, boolean gziped) throws IOException {
        writeLock.lock();
        
        try {
            projectDir.mkdirs();
    
            if (gziped) {
                GZIPInputStream gis = new GZIPInputStream(inputStream);
                untar(projectDir, gis);
            } else {
                untar(projectDir, inputStream);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void untar(File destDir, InputStream inputStream) throws IOException {
        TarInputStream tin = new TarInputStream(inputStream);
        TarEntry tarEntry = null;

        while ((tarEntry = tin.getNextEntry()) != null) {
            File destEntry = new File(destDir, tarEntry.getName());
            File parent = destEntry.getParentFile();

            if (!parent.exists()) {
                parent.mkdirs();
            }

            if (tarEntry.isDirectory()) {
                destEntry.mkdirs();
            } else {
                FileOutputStream fout = new FileOutputStream(destEntry);
                try {
                    tin.copyEntryContents(fout);
                } finally {
                    fout.close();
                }
            }
        }
        
        tin.close();
    }

    private void copyFile(File file, OutputStream os) throws IOException {
        final int buffersize = 4096;

        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] buf = new byte[buffersize];
            int count;

            while((count = fis.read(buf, 0, buffersize)) != -1) {
                os.write(buf, 0, count);
            }
        } finally {
            fis.close();
        }
    }
    
    public void deleteChange(long historyEntryId) {
        writeLock.lock();
        
        try {
            if (!projectDir.exists())
                return;
            
            File historyEntryFile = getChangeFile(historyEntryId);
            
            if (historyEntryFile.exists())
                historyEntryFile.delete();
        } finally {
            writeLock.unlock();
        }
    }
    
    public void loadChange(HistoryEntry historyEntry) {
        readLock.lock();
        
        try {
            File changeFile = getChangeFile(historyEntry.id);
    
            try {
                loadChange(historyEntry, changeFile);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load change file " + changeFile.getAbsolutePath(), e);
            }
        } finally {
            readLock.unlock();
        }
    }
    
    private void loadChange(HistoryEntry historyEntry, File file) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        try {
            Pool pool = new Pool();
            ZipEntry poolEntry = zipFile.getEntry("pool.txt");
            if (poolEntry != null) {
                pool.load(new InputStreamReader(
                    zipFile.getInputStream(poolEntry)));
            } // else, it's a legacy project file

            historyEntry.setChange(History.readOneChange(
                    zipFile.getInputStream(zipFile.getEntry("change.txt")), pool));
        } finally {
            zipFile.close();
        }
    }
    
    public void saveChange(HistoryEntry historyEntry) throws IOException {
        writeLock.lock();
        
        try {
            if (deleted)
                return;
            
            File changeFile = getChangeFile(historyEntry.id);
            
            if (!(changeFile.exists())) {
                File historyDir = changeFile.getParentFile();
                
                if (!historyDir.exists())
                    historyDir.mkdirs();
                
                saveChange(historyEntry, changeFile);
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    private void saveChange(HistoryEntry historyEntry, File file) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            Pool pool = new Pool();

            out.putNextEntry(new ZipEntry("change.txt"));
            try {
                History.writeOneChange(out, historyEntry.getChange(), pool);
            } finally {
                out.closeEntry();
            }

            out.putNextEntry(new ZipEntry("pool.txt"));
            try {
                pool.save(out);
            } finally {
                out.closeEntry();
            }
        } finally {
            out.close();
        }
    }
    
    private File getChangeFile(long historyEntryId) {
        File historyDir = new File(projectDir, "history");
        return new File(historyDir, historyEntryId + ".change.zip");
    }
    
    @Override
    public ProjectMetadata loadMetadata() {
        readLock.lock();
        
        try {
            try {
                return loadMetadataFromFile(new File(projectDir, "metadata.json"));
            } catch (Exception e) {
            }
    
            try {
                return loadMetadataFromFile(new File(projectDir, "metadata.temp.json"));
            } catch (Exception e) {
            }
    
            try {
                return loadMetadataFromFile(new File(projectDir, "metadata.old.json"));
            } catch (Exception e) {
            }
        } finally {
            readLock.unlock();
        }

        return null;
    }

    private ProjectMetadata loadMetadataFromFile(File metadataFile) throws Exception {
        FileReader reader = new FileReader(metadataFile);
        try {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject obj = (JSONObject) tokener.nextValue();

            return ProjectMetadata.loadFromJSON(obj);
        } finally {
            reader.close();
        }
    }
    
    public void saveMetadata(ProjectMetadata metadata) throws JSONException, IOException {
        writeLock.lock();
        
        try {
            if (deleted)
                return;
            
            if (!projectDir.exists())
                projectDir.mkdir();
            
            File tempFile = new File(projectDir, "metadata.temp.json");
            saveMetadataToFile(metadata, tempFile);
    
            File file = new File(projectDir, "metadata.json");
            File oldFile = new File(projectDir, "metadata.old.json");
    
            if (oldFile.exists()) {
                oldFile.delete();
            }
            
            if (file.exists()) {
                file.renameTo(oldFile);
            }
    
            tempFile.renameTo(file);
        } finally {
            writeLock.unlock();
        }
    }

    private static void saveMetadataToFile(ProjectMetadata projectMeta, File metadataFile) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile));
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            projectMeta.write(jsonWriter);
        } finally {
            writer.close();
        }
    }
    

    /**
     * Reconstruct the project metadata on a best efforts basis.  The name is
     * gone, so build something descriptive from the column names.  Recover the
     * creation and modification times based on whatever files are available.
     * 
     * @return
     */
    @Override
    public ProjectMetadata recoverMetadata() {
        ProjectMetadata pm = null;
        
        readLock.lock();
        
        try {
            if (load()) {
                List<String> columnNames = project.columnModel.getColumnNames();
                String tempName = "<recovered project> - " + columnNames.size() 
                        + " cols X " + project.rows.size() + " rows - "
                        + StringUtils.join(columnNames,'|');
                
                long createdTime = System.currentTimeMillis();
                long lastModifiedTime = 0;

                File dataFile = new File(projectDir, "data.zip");
                createdTime = lastModifiedTime = dataFile.lastModified();

                File historyDir = new File(projectDir,"history");
                File[] files = historyDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        long time = f.lastModified();
                        createdTime = Math.min(createdTime, time);
                        lastModifiedTime = Math.max(lastModifiedTime, time);
                    }
                }
                
                pm = new ProjectMetadata(new Date(createdTime),new Date(lastModifiedTime), tempName);
                logger.error("Partially recovered missing metadata project in directory " + projectDir.getAbsolutePath() + " - " + tempName);
            }
        } finally {
            readLock.unlock();
        }
        
        return pm;
    }
}
