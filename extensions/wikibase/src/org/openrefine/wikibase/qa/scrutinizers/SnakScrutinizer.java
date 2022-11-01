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

import java.util.Iterator;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A scrutinizer that inspects snaks individually, no matter whether they appear as main snaks, qualifiers or
 * references.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class SnakScrutinizer extends StatementScrutinizer {

    /**
     * This is the method that subclasses should override to implement their checks.
     * 
     * @param snak:
     *            the snak to inspect
     * @param entityId:
     *            the entity on which it is going to (dis)appear
     * @param added:
     *            whether this snak is going to be added or deleted
     */
    public abstract void scrutinize(Snak snak, EntityIdValue entityId, boolean added);

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        // Main snak
        scrutinize(statement.getClaim().getMainSnak(), entityId, added);

        // Qualifiers
        scrutinizeSnakSet(statement.getClaim().getAllQualifiers(), entityId, added);

        // References
        for (Reference ref : statement.getReferences()) {
            scrutinizeSnakSet(ref.getAllSnaks(), entityId, added);
        }
    }

    protected void scrutinizeSnakSet(Iterator<Snak> snaks, EntityIdValue entityId, boolean added) {
        while (snaks.hasNext()) {
            Snak snak = snaks.next();
            scrutinize(snak, entityId, added);
        }
    }
}
