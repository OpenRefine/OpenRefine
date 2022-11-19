
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.editing.MediaFileUtils;
import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileNameScrutinizer extends EditScrutinizer {

    // see https://commons.wikimedia.org/wiki/Commons:File_naming
    public static final int maxFileNameLength = 240;
    public static final Pattern forbiddenFileNameChars = Pattern.compile(
            ".*([^ %!\"$&'()*,\\-./0-9:;=?@A-Z\\\\^_`a-z~\\x80-\\xFF+]|%[0-9A-Fa-f]{2}|&[A-Za-z0-9\\x80-\\xff]+;|&#[0-9]+;|&#x[0-9A-Fa-f]+;).*");

    public static final String duplicateFileNamesInBatchType = "duplicate-file-names-in-batch";
    public static final String fileNamesAlreadyExistOnWikiType = "file-names-already-exist-on-wiki";
    public static final String invalidCharactersInFileNameType = "invalid-characters-in-file-name";
    public static final String fileNameTooLongType = "file-name-too-long";
    public static final String missingFileNameExtensionType = "missing-file-name-extension";
    public static final String inconsistentFileNameAndPathExtensionType = "inconsistent-file-name-and-path-extension";

    protected Set<String> seenFileNames;

    @Override
    public boolean prepareDependencies() {
        return true;
    }

    @Override
    public void scrutinize(ItemEdit edit) {
        ;
    }

    @Override
    public void batchIsBeginning() {
        seenFileNames = new HashSet<>();
    }

    /**
     * Attempt of a local implementation of the file name normalization that is done in MediaWiki. Assumes a non-empty
     * file name as input. This assumes that some special characters have been replaced already: `:`, `/` and `\`
     * 
     * @return
     */
    protected String normalizeFileNameSpaces(String filename) {
        String replaced = filename.replaceAll("_", " ");
        return replaced.substring(0, 1).toUpperCase() + replaced.substring(1);
    }

    @Override
    public void scrutinize(MediaInfoEdit edit) {
        String fileName = edit.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            // empty file names for new items are handled in NewEntityScrutinizer
            return;
        }

        if (edit.isNew()) {
            // check whether multiple files in the batch to be uploaded have the same filename
            String normalizedFileName = normalizeFileNameSpaces(fileName);
            if (seenFileNames.contains(normalizedFileName)) {
                QAWarning issue = new QAWarning(duplicateFileNamesInBatchType, null, QAWarning.Severity.CRITICAL,
                        1);
                issue.setProperty("example_filename", fileName);
                issue.setFacetable(false);
                addIssue(issue);
            } else {
                seenFileNames.add(normalizedFileName);
            }

            // check whether filenames exceed the maximum length (240 bytes, which we take to be 240 characters)
            // TODO check for the length as bytes in the corresponding enconding (which one?)
            // see https://commons.wikimedia.org/wiki/Commons:File_naming
            if (fileName.length() > maxFileNameLength) {
                QAWarning issue = new QAWarning(fileNameTooLongType, null, QAWarning.Severity.CRITICAL,
                        1);
                issue.setProperty("example_filename", fileName);
                issue.setProperty("max_length", Integer.toString(maxFileNameLength));
                addIssue(issue);
            }

            // Invalid characters
            Matcher matcher = forbiddenFileNameChars.matcher(fileName);
            if (matcher.matches()) {
                QAWarning issue = new QAWarning(invalidCharactersInFileNameType, null, QAWarning.Severity.CRITICAL,
                        1);
                issue.setProperty("example_filename", fileName);
                issue.setProperty("invalid_character", matcher.group(1));
                addIssue(issue);
            }

            // Extensions
            if (!fileName.contains(".")) {
                QAWarning issue = new QAWarning(missingFileNameExtensionType, null, QAWarning.Severity.CRITICAL,
                        1);
                issue.setProperty("example_filename", fileName);
                addIssue(issue);
            } else {
                String[] parts = fileName.split("\\.");
                String extension = parts[parts.length - 1].toLowerCase();
                // Check that the file path has the same extension
                String filePath = edit.getFilePath();
                if (filePath != null) {
                    File file = new File(filePath);
                    String name = file.getName();
                    if (name.contains(".")) {
                        String[] pathParts = name.split("\\.");
                        String pathExtension = pathParts[pathParts.length - 1].toLowerCase();

                        if (!extension.equals(pathExtension)) {
                            QAWarning issue = new QAWarning(inconsistentFileNameAndPathExtensionType, null,
                                    QAWarning.Severity.WARNING, 1);
                            issue.setProperty("example_filename", fileName);
                            issue.setProperty("example_filepath", filePath);
                            addIssue(issue);
                        }
                    }
                }
            }

            // TODO check whether the extension is allowed on the wiki
            // (can we retrieve the list of allowed extensions via the API?)
        }
    }

    @Override
    public void batchIsFinished() {
        if (enableSlowChecks) {
            // check whether the filenames already exist on the wiki
            MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
            try {
                Set<String> existing = mediaFileUtils.checkIfPageNamesExist(seenFileNames.stream().collect(Collectors.toList()));
                if (!existing.isEmpty()) {
                    QAWarning issue = new QAWarning(fileNamesAlreadyExistOnWikiType, null,
                            QAWarning.Severity.CRITICAL, existing.size());
                    issue.setProperty("example_filename", existing.stream().findFirst().get());
                    issue.setFacetable(false);
                    addIssue(issue);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MediaWikiApiErrorException e) {
                e.printStackTrace();
            }
        }
    }
}
