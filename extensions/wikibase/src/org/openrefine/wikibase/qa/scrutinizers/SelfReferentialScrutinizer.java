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
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

/**
 * A scrutinizer that checks for self-referential statements. These statements are flagged by Wikibase as suspicious.
 * 
 * @author Antonin Delpeuch
 *
 */
public class SelfReferentialScrutinizer extends SnakScrutinizer {

    public static final String type = "self-referential-statements";

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (!added) {
            return;
        }
        if (snak instanceof ValueSnak && entityId.equals(((ValueSnak) snak).getValue())) {
            QAWarning issue = new QAWarning(type, null, QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", entityId);
            addIssue(issue);
        }
    }

    @Override
    public boolean prepareDependencies() {
        return true;
    }
}
