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

import java.util.List;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.StatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A scrutinizer checking for unsourced statements
 * 
 * @author Antonin Delpeuch
 *
 */
public class UnsourcedScrutinizer extends EditScrutinizer {

    private String citationNeededConstraintQid;
    public static final String generalType = "unsourced-statements";
    public static final String constraintItemType = "no-references-provided";

    @Override
    public void scrutinize(ItemEdit update) {
        scrutinizeStatementEdit(update);
    }

    @Override
    public void scrutinize(MediaInfoEdit update) {
        scrutinizeStatementEdit(update);
    }

    public void scrutinizeStatementEdit(StatementEntityEdit update) {
        for (Statement statement : update.getAddedStatements()) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            List<Statement> constraintDefinitions = _fetcher.getConstraintsByType(pid, citationNeededConstraintQid);
            List<Reference> referenceList = statement.getReferences();

            if (referenceList.isEmpty()) {
                if (!constraintDefinitions.isEmpty()) {
                    QAWarning issue = new QAWarning(constraintItemType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                    issue.setProperty("property_entity", pid);
                    issue.setProperty("example_entity", update.getEntityId());
                    addIssue(issue);
                } else {
                    warning(generalType);
                }
            }
        }
    }

    @Override
    public boolean prepareDependencies() {
        citationNeededConstraintQid = getConstraintsRelatedId("citation_needed_constraint_qid");
        return _fetcher != null && citationNeededConstraintQid != null;
    }
}
