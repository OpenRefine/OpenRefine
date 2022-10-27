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

import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.LabeledStatementEntityEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

/**
 * A scrutinizer that inspects the values of snaks and terms
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class ValueScrutinizer extends SnakScrutinizer {

    @Override
    public void scrutinize(EntityEdit update) {
        super.scrutinize(update);

        if (update instanceof LabeledStatementEntityEdit) {
            for (MonolingualTextValue label : ((LabeledStatementEntityEdit) update).getLabels()) {
                scrutinize(label);
            }
            for (MonolingualTextValue label : ((LabeledStatementEntityEdit) update).getLabelsIfNew()) {
                scrutinize(label);
            }
        }
        if (update instanceof TermedStatementEntityEdit) {
            for (MonolingualTextValue alias : ((TermedStatementEntityEdit) update).getAliases()) {
                scrutinize(alias);
            }
            for (MonolingualTextValue description : ((TermedStatementEntityEdit) update).getDescriptions()) {
                scrutinize(description);
            }
            for (MonolingualTextValue description : ((TermedStatementEntityEdit) update).getDescriptionsIfNew()) {
                scrutinize(description);
            }
        }
    }

    public abstract void scrutinize(Value value);

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (!added) {
            return;
        }
        if (snak instanceof ValueSnak) {
            scrutinize(((ValueSnak) snak).getValue());
        }
    }

}
