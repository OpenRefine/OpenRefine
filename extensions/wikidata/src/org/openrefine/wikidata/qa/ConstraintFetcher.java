package org.openrefine.wikidata.qa;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openrefine.wikidata.utils.EntityCache;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * This class provides an abstraction over the way constraint
 * definitions are stored in Wikidata.
 * 
 * @author antonin
 *
 */
public class ConstraintFetcher {
    public static String WIKIDATA_CONSTRAINT_PID = "P2302";
    
    public static String FORMAT_CONSTRAINT_QID = "Q21502404";
    public static String FORMAT_REGEX_PID = "P1793";
    
    public static String INVERSE_CONSTRAINT_QID = "Q21510855";
    public static String INVERSE_PROPERTY_PID = "P2306";
    
    public static String USED_ONLY_AS_VALUES_CONSTRAINT_QID = "Q21528958";
    
    public static String USED_ONLY_AS_QUALIFIER_CONSTRAINT_QID = "Q21510863";
    
    public static String USED_ONLY_AS_REFERENCE_CONSTRAINT_QID = "Q21528959";
    
    public static String ALLOWED_QUALIFIERS_CONSTRAINT_QID = "Q21510851";
    public static String ALLOWED_QUALIFIERS_CONSTRAINT_PID = "P2306";
    
    public static String MANDATORY_QUALIFIERS_CONSTRAINT_QID = "Q21510856";
    public static String MANDATORY_QUALIFIERS_CONSTRAINT_PID = "P2306";
    
    public static String SINGLE_VALUE_CONSTRAINT_QID = "Q19474404";
    public static String DISTINCT_VALUES_CONSTRAINT_QID = "Q21502410";
    
    // The following constraints still need to be implemented:
    
    public static String TYPE_CONSTRAINT_QID = "Q21503250";
    
    
    /**
     * Retrieves the regular expression for formatting a property, or null if
     * there is no such constraint
     * @param pid
     * @return the expression of a regular expression which should be compatible with java.util.regex
     */
    public String getFormatRegex(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, FORMAT_CONSTRAINT_QID);
        if (specs != null) {
            List<Value> regexes = findValues(specs, FORMAT_REGEX_PID);
            if (! regexes.isEmpty()) {
                return ((StringValue)regexes.get(0)).getString();
            }
        }
        return null;
    }
    
    /**
     * Retrieves the property that is the inverse of a given property
     * @param pid: the property to retrieve the inverse for
     * @return the pid of the inverse property
     */
    public PropertyIdValue getInversePid(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, INVERSE_CONSTRAINT_QID);
        
        if(specs != null) {
            List<Value> inverses = findValues(specs, INVERSE_PROPERTY_PID);
            if (! inverses.isEmpty()) {
                return (PropertyIdValue)inverses.get(0);
            }
        }
        return null;
    }
    
    /**
     * Is this property for values only?
     */
    public boolean isForValuesOnly(PropertyIdValue pid) {
        return getSingleConstraint(pid, USED_ONLY_AS_VALUES_CONSTRAINT_QID) != null;
    }
    
    /**
     * Is this property for qualifiers only?
     */
    public boolean isForQualifiersOnly(PropertyIdValue pid) {
         return getSingleConstraint(pid, USED_ONLY_AS_QUALIFIER_CONSTRAINT_QID) != null;
    }
    
    /**
     * Is this property for references only?
     */
    public boolean isForReferencesOnly(PropertyIdValue pid) {
        return getSingleConstraint(pid, USED_ONLY_AS_REFERENCE_CONSTRAINT_QID) != null;
    }
    
    /**
     * Get the list of allowed qualifiers (as property ids) for this property (null if any)
     */
    public Set<PropertyIdValue> allowedQualifiers(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, ALLOWED_QUALIFIERS_CONSTRAINT_QID);
        
        if (specs != null) {
            List<Value> properties = findValues(specs, ALLOWED_QUALIFIERS_CONSTRAINT_PID);
            return properties.stream().map(e -> (PropertyIdValue) e).collect(Collectors.toSet());
        }
        return null;
    }
    
    /**
     * Get the list of mandatory qualifiers (as property ids) for this property (null if any)
     */
    public Set<PropertyIdValue> mandatoryQualifiers(PropertyIdValue pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, MANDATORY_QUALIFIERS_CONSTRAINT_QID);
        
        if (specs != null) {
            List<Value> properties = findValues(specs, MANDATORY_QUALIFIERS_CONSTRAINT_PID);
            return properties.stream().map(e -> (PropertyIdValue) e).collect(Collectors.toSet());
        }
        return null;
    }
    
    /**
     * Is this property expected to have at most one value per item?
     */
    public boolean hasSingleValue(PropertyIdValue pid) {
        return getSingleConstraint(pid, SINGLE_VALUE_CONSTRAINT_QID) != null;
    }
    
    /**
     * Is this property expected to have distinct values?
     */
    public boolean hasDistinctValues(PropertyIdValue pid) {
        return getSingleConstraint(pid, DISTINCT_VALUES_CONSTRAINT_QID) != null;
    }
    
    /**
     * Returns a single constraint for a particular type and a property, or null
     * if there is no such constraint
     * @param pid: the property to retrieve the constraints for
     * @param qid: the type of the constraints
     * @return the list of qualifiers for the constraint, or null if it does not exist
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
     * @param pid: the property to retrieve the constraints for
     * @param qid: the type of the constraints
     * @return the stream of matching constraint statements
     */
    protected Stream<Statement> getConstraintsByType(PropertyIdValue pid, String qid) {
        Stream<Statement> allConstraints = getConstraintStatements(pid)
                .stream()
                .filter(s -> ((EntityIdValue) s.getValue()).getId().equals(qid));
        return allConstraints;
    }
    
    /**
     * Gets all the constraint statements for a given property
     * @param pid : the id of the property to retrieve the constraints for
     * @return the list of constraint statements
     */
    protected List<Statement> getConstraintStatements(PropertyIdValue pid) {
        PropertyDocument doc = (PropertyDocument) EntityCache.getEntityDocument(pid);
        StatementGroup group = doc.findStatementGroup(WIKIDATA_CONSTRAINT_PID);
        if (group != null) {
            return group.getStatements();
        } else {
            return new ArrayList<Statement>();
        }
    }
    
    /**
     * Returns the values of a given property in qualifiers
     * @param groups: the qualifiers
     * @param pid: the property to filter on
     * @return
     */
    protected List<Value> findValues(List<SnakGroup> groups, String pid) {
        List<Value> results = new ArrayList<>();
        for(SnakGroup group : groups) {
            if (group.getProperty().getId().equals(pid)) {
                for (Snak snak : group.getSnaks())
                    results.add(snak.getValue());
            }
        }
        return results;
    }
}
