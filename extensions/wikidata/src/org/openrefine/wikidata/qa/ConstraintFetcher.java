package org.openrefine.wikidata.qa;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openrefine.wikidata.utils.EntityCache;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
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
    
    // The following constraints still need to be implemented:
    
    public static String ALLOWED_QUALIFIERS_CONSRAINT_QID = "Q21510851";
    public static String ALLOWED_QUALIFIERS_PID = "P2306";
    
    public static String MANDATORY_QUALIFIERS_CONSTRAINT_QID = "Q21510856";
    public static String MANDATORY_QUALIFIERS_CONSTRAINT_PID = "P2306";
    
    public static String SINGLE_VALUE_CONSRAINT_QID = "Q19474404";
    public static String DISTINCT_VALUES_CONSRAINT_QID = "Q21502410";
    public static String TYPE_CONSTRAINT_QID = "Q21503250";
    
    
    /**
     * Retrieves the regular expression for formatting a property, or null if
     * there is no such constraint
     * @param pid
     * @return the expression of a regular expression which should be compatible with java.util.regex
     */
    public String getFormatRegex(String pid) {
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
    public String getInversePid(String pid) {
        List<SnakGroup> specs = getSingleConstraint(pid, INVERSE_CONSTRAINT_QID);
        
        if(specs != null) {
            List<Value> inverses = findValues(specs, INVERSE_PROPERTY_PID);
            if (! inverses.isEmpty()) {
                return ((EntityIdValue)inverses.get(0)).getId();
            }
        }
        return null;
    }
    
    /**
     * Is this property for values only?
     */
    public boolean isForValuesOnly(String pid) {
        return getSingleConstraint(pid, USED_ONLY_AS_VALUES_CONSTRAINT_QID) != null;
    }
    
    /**
     * Is this property for qualifiers only?
     */
    public boolean isForQualifiersOnly(String pid) {
         return getSingleConstraint(pid, USED_ONLY_AS_QUALIFIER_CONSTRAINT_QID) != null;
    }
    
    /**
     * Is this property for references only?
     */
    public boolean isForReferencesOnly(String pid) {
        return getSingleConstraint(pid, USED_ONLY_AS_REFERENCE_CONSTRAINT_QID) != null;
    }
    
    /**
     * Returns a single constraint for a particular type and a property, or null
     * if there is no such constraint
     * @param pid: the property to retrieve the constraints for
     * @param qid: the type of the constraints
     * @return the list of qualifiers for the constraint, or null if it does not exist
     */
    protected List<SnakGroup> getSingleConstraint(String pid, String qid) {
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
    protected Stream<Statement> getConstraintsByType(String pid, String qid) {
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
    protected List<Statement> getConstraintStatements(String pid) {
        PropertyDocument doc = (PropertyDocument) EntityCache.getEntityDocument(pid);
        StatementGroup group = doc.findStatementGroup(WIKIDATA_CONSTRAINT_PID);
        return group.getStatements();
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
