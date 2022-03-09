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

import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public abstract class StatementScrutinizer extends EditScrutinizer {

    @Override
    public void scrutinize(TermedStatementEntityEdit update) {
        EntityIdValue currentEntityId = update.getEntityId();
        for (Statement statement : update.getAddedStatements()) {
            scrutinize(statement, currentEntityId, true);
        }
        for (Statement statement : update.getDeletedStatements()) {
            scrutinize(statement, currentEntityId, false);
        }
    }

    /**
     * The method that should be overridden by subclasses, implementing the checks
     * on one statement
     * 
     * @param statement:
     *            the statement to scrutinize
     * @param entityId:
     *            the id of the entity on which this statement is made or removed
     * @param added:
     *            whether this statement was added or deleted
     */
    public abstract void scrutinize(Statement statement, EntityIdValue entityId, boolean added);
}
