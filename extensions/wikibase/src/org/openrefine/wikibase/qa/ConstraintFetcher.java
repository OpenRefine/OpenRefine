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

package org.openrefine.wikibase.qa;

import org.openrefine.wikibase.utils.EntityCache;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides an abstraction over the way constraint definitions are stored in a Wikibase instance.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ConstraintFetcher {

    private String wikibaseConstraintPid;

    private EntityCache entityCache;

    public ConstraintFetcher(EntityCache cache, String wikibaseConstraintPid) {
        entityCache = cache;
        this.wikibaseConstraintPid = wikibaseConstraintPid;
    }

    /**
     * Gets the list of constraints of a particular type for a property
     *
     * @param pid
     *            the property to retrieve the constraints for
     * @param id
     *            the type of the constraints
     * @return the list of matching constraint statements
     */
    public List<Statement> getConstraintsByType(PropertyIdValue pid, String id) {
        Stream<Statement> allConstraints = getConstraintStatements(pid).stream()
                .filter(s -> s.getValue() != null && ((EntityIdValue) s.getValue()).getId().equals(id))
                .filter(s -> !StatementRank.DEPRECATED.equals(s.getRank()));
        return allConstraints.collect(Collectors.toList());
    }

    /**
     * Gets all the constraint statements for a given property
     * 
     * @param pid
     *            the id of the property to retrieve the constraints for
     * @return the list of constraint statements
     */
    private List<Statement> getConstraintStatements(PropertyIdValue pid) {
        PropertyDocument doc = (PropertyDocument) entityCache.get(pid);
        StatementGroup group = doc.findStatementGroup(wikibaseConstraintPid);
        if (group != null) {
            return group.getStatements().stream()
                    .filter(s -> s.getValue() != null && s.getValue() instanceof EntityIdValue)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

}
