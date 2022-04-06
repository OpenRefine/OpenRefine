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
package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A scrutinizer that inspects new items.
 * 
 * @author Antonin Delpeuch
 */
public class NewItemScrutinizer extends EditScrutinizer {

    public static final String noLabelType = "new-item-without-labels-or-aliases";
    public static final String noDescType = "new-item-without-descriptions";
    public static final String deletedStatementsType = "new-item-with-deleted-statements";
    public static final String noTypeType = "new-item-without-instance-of-or-subclass-of";
    public static final String newItemType = "new-item-created";

    @Override
    public boolean prepareDependencies() {
        return true;
    }

    @Override
    public void scrutinize(TermedStatementEntityEdit update) {
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

}
