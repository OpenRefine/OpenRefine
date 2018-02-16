/*
 * 
 * Copyright 2010, Google Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package com.google.refine.history;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
// import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.Pool;

/**
 * Track done and undone changes. Done changes can be undone; undone changes can be redone. Each
 * change is actually not tracked directly but through a history entry. The history entry stores
 * only the metadata, while the change object stores the actual data. Thus the history entries are
 * much smaller and can be kept in memory, while the change objects are only loaded into memory on
 * demand.
 */
public class History implements Jsonizable {
  static public Change readOneChange(InputStream in, Pool pool) throws Exception {
    LineNumberReader reader = new LineNumberReader(new InputStreamReader(in, "UTF-8"));
    try {
      return readOneChange(reader, pool);
    } finally {
      reader.close();
    }
  }

  static public Change readOneChange(LineNumberReader reader, Pool pool) throws Exception {
    /* String version = */ reader.readLine();

    String className = reader.readLine();
    Class<? extends Change> klass = getChangeClass(className);

    Method load = klass.getMethod("load", LineNumberReader.class, Pool.class);

    return (Change) load.invoke(null, reader, pool);
  }

  static public void writeOneChange(OperationRegistry opRegistry, PreferenceStore prefStore, 
      OutputStream out, Change change, Pool pool) throws IOException {
    Writer writer = new OutputStreamWriter(out, "UTF-8");
    try {
      History.writeOneChange(opRegistry, prefStore, writer, change, pool);
    } finally {
      writer.flush();
    }
  }

  static public void writeOneChange(OperationRegistry opRegistry, PreferenceStore prefStore, 
      Writer writer, Change change, Pool pool) throws IOException {
    Properties options = new Properties();
    options.setProperty("mode", "save");
    options.put("pool", pool);

    writeOneChange(opRegistry, prefStore, writer, change, options);
  }

  static public void writeOneChange(OperationRegistry opRegistry, PreferenceStore prefStore, 
      Writer writer, Change change, Properties options) throws IOException {
    writer.write(RefineServlet.VERSION);
    writer.write('\n');
    writer.write(change.getClass().getName());
    writer.write('\n');

    change.save(opRegistry, prefStore, writer, options);
  }

  @SuppressWarnings("unchecked")
  static public Class<? extends Change> getChangeClass(String className)
      throws ClassNotFoundException {
    return (Class<? extends Change>) RefineServlet.getClass(className);
  }

  protected long _projectID;
  protected List<HistoryEntry> _pastEntries; // done changes, can be undone
  protected List<HistoryEntry> _futureEntries; // undone changes, can be redone

  public History(Project project) {
    _projectID = project.id;
    _pastEntries = new ArrayList<>();
    _futureEntries = new ArrayList<>();
  }

  /**
   * Adds a HistoryEntry to the list of past histories Adding a new entry clears all currently held
   * future histories
   * 
   * @param entry
   */
  public void addEntry(HistoryEntryManager historyManager, Project project, HistoryEntry entry) {
    // NOTE: project lock must be acquired *first* to prevent deadlocks, so we use a
    // synchronized block instead of synchronizing the entire method.
    synchronized (this) {
      entry.apply(historyManager, project);
      _pastEntries.add(entry);

      project.getMetadata().updateModified();

      // Any new change will clear all future entries.
      List<HistoryEntry> futureEntries = _futureEntries;
      _futureEntries = new ArrayList<>();

      for (HistoryEntry entry2 : futureEntries) {
        try {
          // remove residual data on disk
          entry2.delete();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  synchronized public List<HistoryEntry> getLastPastEntries(int count) {
    if (count <= 0) {
      return new LinkedList<>(_pastEntries);
    } else {
      return _pastEntries.subList(Math.max(_pastEntries.size() - count, 0), _pastEntries.size());
    }
  }

  synchronized public void undoRedo(HistoryEntryManager historyManager, Project project, long lastDoneEntryID) {
    if (lastDoneEntryID == 0) {
      // undo all the way back to the start of the project
      undo(project, _pastEntries.size());
    } else {
      for (int i = 0; i < _pastEntries.size(); i++) {
        if (_pastEntries.get(i).id == lastDoneEntryID) {
          undo(project, _pastEntries.size() - i - 1);
          return;
        }
      }

      for (int i = 0; i < _futureEntries.size(); i++) {
        if (_futureEntries.get(i).id == lastDoneEntryID) {
          redo(historyManager, project, i + 1);
          return;
        }
      }
    }
  }

  synchronized public long getPrecedingEntryID(long entryID) {
    if (entryID == 0) {
      return -1;
    } else {
      for (int i = 0; i < _pastEntries.size(); i++) {
        if (_pastEntries.get(i).id == entryID) {
          return i == 0 ? 0 : _pastEntries.get(i - 1).id;
        }
      }

      for (int i = 0; i < _futureEntries.size(); i++) {
        if (_futureEntries.get(i).id == entryID) {
          if (i > 0) {
            return _futureEntries.get(i - 1).id;
          } else if (_pastEntries.size() > 0) {
            return _pastEntries.get(_pastEntries.size() - 1).id;
          } else {
            return 0;
          }
        }
      }
    }
    return -1;
  }

  protected HistoryEntry getEntry(long entryID) {
    for (int i = 0; i < _pastEntries.size(); i++) {
      if (_pastEntries.get(i).id == entryID) {
        return _pastEntries.get(i);
      }
    }

    for (int i = 0; i < _futureEntries.size(); i++) {
      if (_futureEntries.get(i).id == entryID) {
        return _futureEntries.get(i);
      }
    }
    return null;
  }

  protected void undo(Project project, int times) {

    while (times > 0 && _pastEntries.size() > 0) {
      HistoryEntry entry = _pastEntries.get(_pastEntries.size() - 1);

      entry.revert(project);

      project.getMetadata().updateModified();
      times--;

      _pastEntries.remove(_pastEntries.size() - 1);
      _futureEntries.add(0, entry);
    }
  }

  protected void redo(HistoryEntryManager historyManager, Project project, int times) {

    while (times > 0 && _futureEntries.size() > 0) {
      HistoryEntry entry = _futureEntries.get(0);

      entry.apply(historyManager, project);

      project.getMetadata().updateModified();
      times--;

      _pastEntries.add(entry);
      _futureEntries.remove(0);
    }
  }

  @Override
  synchronized public void write(OperationRegistry opRegistry, PreferenceStore prefStore, 
      JSONWriter writer, Properties options) throws JSONException {

    writer.object();

    writer.key("past");
    writer.array();
    for (HistoryEntry entry : _pastEntries) {
      entry.write(opRegistry, prefStore, writer, options);
    }
    writer.endArray();

    writer.key("future");
    writer.array();
    for (HistoryEntry entry : _futureEntries) {
      entry.write(opRegistry, prefStore, writer, options);
    }
    writer.endArray();

    writer.endObject();
  }

  /*
   * NOTE: this method is called from the autosave thread with the Project lock already held, so no
   * other synchronized method here can aquire that Project lock or a deadlock will result.be
   * careful of thread synchronization to avoid deadlocks.
   */
  synchronized public void save(Writer writer, Properties options) throws IOException {
    writer.write("pastEntryCount=");
    writer.write(Integer.toString(_pastEntries.size()));
    writer.write('\n');
    for (HistoryEntry entry : _pastEntries) {
      entry.save(writer, options);
      writer.write('\n');
    }

    writer.write("futureEntryCount=");
    writer.write(Integer.toString(_futureEntries.size()));
    writer.write('\n');
    for (HistoryEntry entry : _futureEntries) {
      entry.save(writer, options);
      writer.write('\n');
    }

    writer.write("/e/\n");
  }

  synchronized public void load(OperationRegistry opRegistry, HistoryEntryManager historyManager, Project project, LineNumberReader reader) throws Exception {
    String line;
    while ((line = reader.readLine()) != null && !"/e/".equals(line)) {
      int equal = line.indexOf('=');
      CharSequence field = line.subSequence(0, equal);
      String value = line.substring(equal + 1);

      if ("pastEntryCount".equals(field)) {
        int count = Integer.parseInt(value);

        for (int i = 0; i < count; i++) {
          _pastEntries.add(HistoryEntry.load(opRegistry, historyManager, project, reader.readLine()));
        }
      } else if ("futureEntryCount".equals(field)) {
        int count = Integer.parseInt(value);

        for (int i = 0; i < count; i++) {
          _futureEntries.add(HistoryEntry.load(opRegistry, historyManager, project, reader.readLine()));
        }
      }
    }
  }
}
