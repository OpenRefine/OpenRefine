/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.io.File;

/**
 * A scrutinizer that inspects new entities.
 * 
 * @author Antonin Delpeuch
 */
public class NewEntityScrutinizer extends EditScrutinizer {

    public static final String noLabelType = "new-item-without-labels-or-aliases";
    public static final String noDescType = "new-item-without-descriptions";
    public static final String deletedStatementsType = "new-item-with-deleted-statements";
    public static final String noTypeType = "new-item-without-instance-of-or-subclass-of";
    public static final String newItemType = "new-item-created";
    public static final String duplicateLabelDescriptionType = "duplicate-label-and-description";

    // mediainfo entities
    public static final String newMediaWithoutFilePathType = "new-media-without-file-path";
    public static final String newMediaWithoutFileNameType = "new-media-without-file-name";
    public static final String newMediaWithoutWikitextType = "new-media-without-wikitext";
    public static final String newMediaType = "new-media-created";
    public static final String invalidFilePathType = "invalid-file-path";
    // TODO add checks for bad file names (which are page titles): https://www.mediawiki.org/wiki/Help:Bad_title
    // https://commons.wikimedia.org/wiki/Commons:File_naming

    // map from seen pairs of labels and descriptions in a given language to an example id where this was seen
    Map<LabelDescription, EntityIdValue> labelDescriptionPairs;

    @Override
    public boolean prepareDependencies() {
        return true;
    }

    @Override
    public void batchIsBeginning() {
        labelDescriptionPairs = new HashMap<>();
    }

    @Override
    public void scrutinize(MediaInfoEdit update) {
        if (update.isNew()) {
            info(newMediaType);

            if (update.getFileName() == null || update.getFileName().isBlank()) {
                QAWarning issue = new QAWarning(newMediaWithoutFileNameType, null, QAWarning.Severity.CRITICAL, 1);
                issue.setProperty("example_entity", update.getEntityId());
                addIssue(issue);
            }
            if (update.getFilePath() == null || update.getFilePath().isBlank()) {
                QAWarning issue = new QAWarning(newMediaWithoutFilePathType, null, QAWarning.Severity.CRITICAL, 1);
                issue.setProperty("example_entity", update.getEntityId());
                addIssue(issue);
            } else if (enableSlowChecks) {
                // check that the file exists.
                File file = new File(update.getFilePath());
                if (!file.exists() && !isValidURL(update.getFilePath())) {
                    QAWarning issue = new QAWarning(invalidFilePathType, null, QAWarning.Severity.CRITICAL, 1);
                    issue.setFacetable(false); // for now
                    issue.setProperty("example_path", update.getFilePath());
                    addIssue(issue);
                }
            }

            if (update.getWikitext() == null || update.getWikitext().isBlank()) {
                QAWarning issue = new QAWarning(newMediaWithoutWikitextType, null, QAWarning.Severity.CRITICAL, 1);
                issue.setProperty("example_entity", update.getEntityId());
                addIssue(issue);
            }
        }
    }

    /**
     * Check if a URL looks legitimate for upload. TODO we could potentially do a HEAD request to check it already
     * exists, but perhaps that's too slow even for the slow mode.
     *
     * @param url
     *            the URL to check
     * @return whether the URL is syntactically correct
     */
    protected static boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            // we need to check that we do have a protocol, otherwise invalid local paths (such as "/foo/bar") could
            // be understood as URIs
            return uri.getScheme() != null && uri.getScheme().startsWith("http");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public void scrutinize(ItemEdit update) {
        if (update.isNew()) {
            info(newItemType);

            if (update.getLabels().isEmpty() && update.getLabelsIfNew().isEmpty() && update.getAliases().isEmpty()) {
                QAWarning issue = new QAWarning(noLabelType, null, QAWarning.Severity.CRITICAL, 1);
                issue.setProperty("example_entity", update.getEntityId());
                addIssue(issue);
            }

            if (update.getDescriptions().isEmpty() && update.getDescriptionsIfNew().isEmpty()) {
                QAWarning issue = new QAWarning(noDescType, null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_entity", update.getEntityId());
                addIssue(issue);
            }

            if (!update.getDeletedStatements().isEmpty()) {
                QAWarning issue = new QAWarning(deletedStatementsType, null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_entity", update.getEntityId());
                addIssue(issue);
            }

            // check that the pairs of (label,description) are unique among all new items
            ItemDocument newItem = update.toNewEntity();
            Map<String, MonolingualTextValue> descriptions = newItem.getDescriptions();
            for (MonolingualTextValue label : newItem.getLabels().values()) {
                MonolingualTextValue matchingDescription = descriptions.get(label.getLanguageCode());
                if (matchingDescription != null) {
                    LabelDescription labelDescription = new LabelDescription(
                            label.getLanguageCode(),
                            label.getText(),
                            matchingDescription.getText());
                    EntityIdValue exampleId = labelDescriptionPairs.get(labelDescription);
                    if (exampleId != null) {
                        QAWarning issue = new QAWarning(duplicateLabelDescriptionType, label.getLanguageCode(), QAWarning.Severity.CRITICAL,
                                1);
                        issue.setProperty("first_example_entity", exampleId);
                        issue.setProperty("second_example_entity", update.getEntityId());
                        issue.setProperty("common_label", label.getText());
                        issue.setProperty("common_description", matchingDescription.getText());
                        issue.setProperty("lang_code", label.getLanguageCode());
                        addIssue(issue);
                    } else {
                        labelDescriptionPairs.put(labelDescription, update.getEntityId());
                    }
                }
            }

            // Try to find a "instance of" or "subclass of" claim
            boolean typeFound = false;
            for (Statement statement : update.getAddedStatements()) {
                String pid = statement.getMainSnak().getPropertyId().getId();
                if (manifest.getInstanceOfPid().equals(pid) || manifest.getSubclassOfPid().equals(pid)) {
                    typeFound = true;
                    break;
                }
            }
            if (!typeFound) {
                QAWarning issue = new QAWarning(noTypeType, null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("example_entity", update.getEntityId());
                addIssue(issue);
            }
        }
    }

    /**
     * A pair of a label and a description in a common language.
     */
    private static class LabelDescription {

        protected String label;
        protected String description;
        protected String language;
        protected EntityIdValue exampleEntity;

        public LabelDescription(String label, String description, String language) {
            this.label = label;
            this.description = description;
            this.language = language;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabelDescription that = (LabelDescription) o;
            return label.equals(that.label) &&
                    description.equals(that.description) &&
                    language.equals(that.language);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, description, language);
        }

        @Override
        public String toString() {
            return "LabelDescription{" +
                    "label='" + label + '\'' +
                    ", description='" + description + '\'' +
                    ", language='" + language + '\'' +
                    '}';
        }
    }
}
