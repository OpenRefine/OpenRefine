
package org.openrefine.importing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FormatRegistry {

    // Mapping from format to label, e.g., "text" to "Text files", "text/xml" to "XML files"
    final static private Map<String, ImportingFormat> formatToRecord = new HashMap<String, ImportingFormat>();

    // Mapping from format to guessers
    final static private Map<String, List<FormatGuesser>> formatToGuessers = new HashMap<String, List<FormatGuesser>>();

    // Mapping from file extension to format, e.g., ".xml" to "text/xml"
    final static private Map<String, String> extensionToFormat = new HashMap<String, String>();

    // Mapping from mime type to format, e.g., "application/json" to "text/json"
    final static private Map<String, String> mimeTypeToFormat = new HashMap<String, String>();

    static public void registerFormat(String format, String label) {
        registerFormat(format, label, null, null);
    }

    static public void registerFormat(String format, String label, String uiClass, ImportingParser parser) {
        formatToRecord.put(format, new ImportingFormat(format, label, true, uiClass, parser));
    }

    static public void registerFormat(
            String format, String label, boolean download, String uiClass, ImportingParser parser) {
        formatToRecord.put(format, new ImportingFormat(format, label, download, uiClass, parser));
    }

    static public void registerFormatGuesser(String format, FormatGuesser guesser) {
        List<FormatGuesser> guessers = formatToGuessers.get(format);
        if (guessers == null) {
            guessers = new LinkedList<FormatGuesser>();
            formatToGuessers.put(format, guessers);
        }
        guessers.add(0, guesser); // prepend so that newer guessers take priority
    }

    static public void registerExtension(String extension, String format) {
        extensionToFormat.put(extension.startsWith(".") ? extension : ("." + extension), format);
    }

    static public void registerMimeType(String mimeType, String format) {
        mimeTypeToFormat.put(mimeType, format);
    }

    static public String getFormatFromFileName(String fileName) {
        int start = 0;
        while (true) {
            int dot = fileName.indexOf('.', start);
            if (dot < 0) {
                break;
            }

            String extension = fileName.substring(dot);
            String format = extensionToFormat.get(extension);
            if (format != null) {
                return format;
            } else {
                start = dot + 1;
            }
        }
        return null;
    }

    static public String getFormatFromMimeType(String mimeType) {
        return mimeTypeToFormat.get(mimeType);
    }

    static public String getFormat(String fileName, String mimeType) {
        String fileNameFormat = getFormatFromFileName(fileName);
        if (mimeType != null) {
            mimeType = mimeType.split(";")[0];
        }
        String mimeTypeFormat = mimeType == null ? null : getFormatFromMimeType(mimeType);
        if (mimeTypeFormat == null) {
            return fileNameFormat;
        } else if (fileNameFormat == null) {
            return mimeTypeFormat;
        } else if (mimeTypeFormat.startsWith(fileNameFormat)) {
            // mime type-base format is more specific
            return mimeTypeFormat;
        } else {
            return fileNameFormat;
        }
    }

    public static Map<String, String> getExtensionToFormat() {
        return extensionToFormat;
    }

    public static Map<String, String> getMimeTypeToFormat() {
        return mimeTypeToFormat;
    }

    public static Map<String, ImportingFormat> getFormatToRecord() {
        return formatToRecord;
    }

    public static Map<String, List<FormatGuesser>> getFormatToGuessers() {
        return formatToGuessers;
    }
}
