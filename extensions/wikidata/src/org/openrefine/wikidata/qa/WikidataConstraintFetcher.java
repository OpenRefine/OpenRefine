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
package org.openrefine.wikidata.qa;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openrefine.wikidata.utils.EntityCache;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * This class provides an abstraction over the way constraint definitions are
 * stored in Wikidata.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WikidataConstraintFetcher implements ConstraintFetcher {

    public static String WIKIDATA_CONSTRAINT_PID = "P2302";

    public static String FORMAT_CONSTRAINT_QID = "Q21502404";
    public static String FORMAT_REGEX_PID = "P1793";

    public static String INVERSE_CONSTRAINT_QID = "Q21510855";
    public static String INVERSE_PROPERTY_PID = "P2306";
    public static String SYMMETRIC_CONSTRAINT_QID = "Q21510862";

    public static String SCOPE_CONSTRAINT_QID = "Q53869507";
    public static String SCOPE_CONSTRAINT_PID = "P5314";
    public static String SCOPE_CONSTRAINT_VALUE_QID = "Q54828448";
    public static String SCOPE_CONSTRAINT_QUALIFIER_QID = "Q54828449";
    public static String SCOPE_CONSTRAINT_REFERENCE_QID = "Q54828450";

    public static String USED_ONLY_AS_QUALIFIER_CONSTRAINT_QID = "Q21510863";

    public static String USED_ONLY_AS_REFERENCE_CONSTRAINT_QID = "Q21528959";

    public static String ALLOWED_QUALIFIERS_CONSTRAINT_QID = "Q21510851";
    public static String ALLOWED_QUALIFIERS_CONSTRAINT_PID = "P2306";

    public static String MANDATORY_QUALIFIERS_CONSTRAINT_QID = "Q21510856";
    public static String MANDATORY_QUALIFIERS_CONSTRAINT_PID = "P2306";
    
    public static String ALLOWED_VALUES_CONSTRAINT_QID = "Q21510859";
    public static String ALLOWED_VALUES_CONSTRAINT_PID = "P2305";

    public static String DISALLOWED_VALUES_CONSTRAINT_QID = "Q52558054";
    public static String DISALLOWED_VALUES_CONSTRAINT_PID = "P2305";

    public static String SINGLE_VALUE_CONSTRAINT_QID = "Q19474404";
    public static String SINGLE_BEST_VALUE_CONSTRAINT_QID = "Q52060874";
    public static String DISTINCT_VALUES_CONSTRAINT_QID = "Q21502410";
    
    public static String NO_BOUNDS_CONSTRAINT_QID = "Q51723761";
    public static String INTEGER_VALUED_CONSTRAINT_QID = "Q52848401";
    
    public static String ALLOWED_UNITS_CONSTRAINT_QID = "Q21514353";
    public static String ALLOWED_UNITS_CONSTRAINT_PID = "P2305";
    
    public static String ALLOWED_ENTITY_TYPES_QID = "Q52004125";
    public static String ALLOWED_ITEM_TYPE_QID = "Q29934200";
    public static String ALLOWED_ENTITY_TYPES_PID = "P2305";
    

    // The following constraints still need to be implemented:

    public static String TYPE_CONSTRAINT_QID = "Q21503250";

    protected EntityCache entityCache;
    
    public WikidataConstraintFetcher(EntityCache cache) {
        entityCache = cache;
    }

    @Override
    public String getFormatRegex(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, FORMAT_CONSTRAINT_QID);
        if (specs != null) {
            List<Value> regexes = findValues(specs, FORMAT_REGEX_PID);
            if (!regexes.isEmpty()) {
                return ((StringValue) regexes.get(0)).getString();
            }
        }
        return null;
    }

    @Override
    public PropertyIdValue getInversePid(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, INVERSE_CONSTRAINT_QID);

        if (specs != null) {
            List<Value> inverses = findValues(specs, INVERSE_PROPERTY_PID);
            if (!inverses.isEmpty()) {
                return (PropertyIdValue) inverses.get(0);
            }
        }
        return null;
    }

    @Override
    public boolean allowedAsValue(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, SCOPE_CONSTRAINT_QID);
        
        if (specs != null) {
            ItemIdValue target = Datamodel.makeWikidataItemIdValue(SCOPE_CONSTRAINT_VALUE_QID);
            return findValues(specs, SCOPE_CONSTRAINT_PID).contains(target);
        }
        return true;
    }

    @Override
    public boolean allowedAsQualifier(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, SCOPE_CONSTRAINT_QID);
        
        if (specs != null) {
            ItemIdValue target = Datamodel.makeWikidataItemIdValue(SCOPE_CONSTRAINT_QUALIFIER_QID);
            return findValues(specs, SCOPE_CONSTRAINT_PID).contains(target);
        }
        return true;
    }

    @Override
    public boolean allowedAsReference(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, SCOPE_CONSTRAINT_QID);
        
        if (specs != null) {
            ItemIdValue target = Datamodel.makeWikidataItemIdValue(SCOPE_CONSTRAINT_REFERENCE_QID);
            return findValues(specs, SCOPE_CONSTRAINT_PID).contains(target);
        }
        return true;
    }

    @Override
    public Set<PropertyIdValue> allowedQualifiers(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, ALLOWED_QUALIFIERS_CONSTRAINT_QID);

        if (specs != null) {
            List<Value> properties = findValues(specs, ALLOWED_QUALIFIERS_CONSTRAINT_PID);
            return properties.stream()
                    .filter(e -> e != null)
                    .map(e -> (PropertyIdValue) e)
                    .collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Set<PropertyIdValue> mandatoryQualifiers(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, MANDATORY_QUALIFIERS_CONSTRAINT_QID);

        if (specs != null) {
            List<Value> properties = findValues(specs, MANDATORY_QUALIFIERS_CONSTRAINT_PID);
            return properties.stream()
                    .filter(e -> e != null)
                    .map(e -> (PropertyIdValue) e)
                    .collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public boolean hasSingleValue(PropertyIdValue pid) {
        return getSingleConstraint(pid, SINGLE_VALUE_CONSTRAINT_QID) != null;
    }
    
    @Override
    public boolean hasSingleBestValue(PropertyIdValue pid) {
        return getSingleConstraint(pid, SINGLE_BEST_VALUE_CONSTRAINT_QID) != null;
    }

    @Override
    public boolean hasDistinctValues(PropertyIdValue pid) {
        return getSingleConstraint(pid, DISTINCT_VALUES_CONSTRAINT_QID) != null;
    }
    
    @Override
    public boolean isSymmetric(PropertyIdValue pid) {
        return getSingleConstraint(pid, SYMMETRIC_CONSTRAINT_QID) != null;
    }

    @Override
    public Set<Value> allowedValues(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, ALLOWED_VALUES_CONSTRAINT_QID);

        if (specs != null) {
            List<Value> properties = findValues(specs, ALLOWED_VALUES_CONSTRAINT_PID);
            return properties.stream().collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Set<Value> disallowedValues(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, DISALLOWED_VALUES_CONSTRAINT_QID);

        if (specs != null) {
            List<Value> properties = findValues(specs, DISALLOWED_VALUES_CONSTRAINT_PID);
            return properties.stream().collect(Collectors.toSet());
        }
        return null;
    }
    
    @Override
    public boolean boundsAllowed(PropertyIdValue pid) {
        return getSingleConstraint(pid, NO_BOUNDS_CONSTRAINT_QID) == null;
    }
    
    @Override
    public boolean integerValued(PropertyIdValue pid) {
        return getSingleConstraint(pid, INTEGER_VALUED_CONSTRAINT_QID) != null;
    }
    
    @Override
    public Set<ItemIdValue> allowedUnits(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, ALLOWED_UNITS_CONSTRAINT_QID);

        if (specs != null) {
            List<Value> properties = findValues(specs, ALLOWED_UNITS_CONSTRAINT_PID);
            return properties.stream().map(e -> e == null ? null : (ItemIdValue) e).collect(Collectors.toSet());
        }
        return null;
    }
    
    @Override
    public boolean usableOnItems(PropertyIdValue pid) {
        List<SnakGroup> constraint = getSingleConstraint(pid, ALLOWED_ENTITY_TYPES_QID);
        if (constraint != null) {
            return findValues(constraint, ALLOWED_ENTITY_TYPES_PID).contains(
                    Datamodel.makeWikidataItemIdValue(ALLOWED_ITEM_TYPE_QID));
        }
        return true;
    }

    /**
     * Returns a single constraint for a particular type and a property, or null if
     * there is no such constraint
     * 
     * @param pid:
     *            the property to retrieve the constraints for
     * @param qid:
     *            the type of the constraints
     * @return the list of qualifiers for the constraint, or null if it does not
     *         exist
     */
    protected List<SnakGroup> getSingleConstraint(PropertyIdValue pid, String qid) {
        Statement statement = getConstraintsByType(pid, qid).findFirst().orElse(null);
        if (statement != null) {
            return statement.getClaim().getQualifiers();
        }
        return null;
    }

    /**
     * Gets the list of constraints of a particular type for a property
     * 
     * @param pid:
     *            the property to retrieve the constraints for
     * @param qid:
     *            the type of the constraints
     * @return the stream of matching constraint statements
     */
    protected Stream<Statement> getConstraintsByType(PropertyIdValue pid, String qid) {
        Stream<Statement> allConstraints = getConstraintStatements(pid).stream()
                .filter(s -> s.getValue() != null && ((EntityIdValue) s.getValue()).getId().equals(qid))
                .filter(s -> !StatementRank.DEPRECATED.equals(s.getRank()));
        return allConstraints;
    }

    /**
     * Gets all the constraint statements for a given property
     * 
     * @param pid
     *            : the id of the property to retrieve the constraints for
     * @return the list of constraint statements
     */
    protected List<Statement> getConstraintStatements(PropertyIdValue pid) {
        PropertyDocument doc = (PropertyDocument) entityCache.get(pid);
        StatementGroup group = doc.findStatementGroup(WIKIDATA_CONSTRAINT_PID);
        if (group != null) {
            return group.getStatements().stream()
                    .filter(s -> s.getValue() != null && s.getValue() instanceof EntityIdValue)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<Statement>();
        }
    }

    /**
     * Returns the values of a given property in qualifiers
     * 
     * @param groups:
     *            the qualifiers
     * @param pid:
     *            the property to filter on
     * @return
     */
    protected List<Value> findValues(List<SnakGroup> groups, String pid) {
        List<Value> results = new ArrayList<>();
        for (SnakGroup group : groups) {
            if (group.getProperty().getId().equals(pid)) {
                for (Snak snak : group.getSnaks())
                    results.add(snak.getValue());
            }
        }
        return results;
    }
}
