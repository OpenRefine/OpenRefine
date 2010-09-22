package com.google.refine.history;

import java.io.Writer;
import java.util.Properties;


public interface HistoryEntryManager {
    public void loadChange(HistoryEntry historyEntry);
    public void saveChange(HistoryEntry historyEntry) throws Exception;
    public void save(HistoryEntry historyEntry, Writer writer, Properties options);
    public void delete(HistoryEntry historyEntry);
}
