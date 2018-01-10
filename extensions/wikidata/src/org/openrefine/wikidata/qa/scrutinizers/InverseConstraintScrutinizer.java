package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * A scrutinizer that checks for missing inverse statements in
 * edit batches.
 * 
 * @author antonin
 *
 */
public class InverseConstraintScrutinizer extends StatementScrutinizer {

    private Map<PropertyIdValue, PropertyIdValue> _inverse;
    private Map<PropertyIdValue, Map<EntityIdValue, Set<EntityIdValue> >> _statements;
    
    public InverseConstraintScrutinizer() {
        _inverse = new HashMap<>();
        _statements = new HashMap<>();
    }
    
    protected PropertyIdValue getInverseConstraint(PropertyIdValue pid) {
        if (_inverse.containsKey(pid)) {
            return _inverse.get(pid);
        } else {
            PropertyIdValue inversePid = _fetcher.getInversePid(pid);
            _inverse.put(pid, inversePid);
            _statements.put(pid, new HashMap<EntityIdValue,Set<EntityIdValue>>());
            
            // We are doing this check because we do not have any guarantee that
            // the inverse constraints are consistent on Wikidata.
            if (inversePid != null && !_inverse.containsKey(inversePid)) {
                _inverse.put(inversePid, pid);
                _statements.put(inversePid, new HashMap<EntityIdValue,Set<EntityIdValue>>());
            }
            return inversePid;
        }
    }
    
    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        if (!added) {
            return; // TODO support for deleted statements
        }
        
        Value mainSnakValue = statement.getClaim().getMainSnak().getValue();
        if (ItemIdValue.class.isInstance(mainSnakValue)) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            PropertyIdValue inversePid = getInverseConstraint(pid);
            if (inversePid != null) {
                EntityIdValue targetEntityId = (EntityIdValue) mainSnakValue;
                Set<EntityIdValue> currentValues = _statements.get(pid).get(entityId);
                if (currentValues == null) {
                    currentValues = new HashSet<EntityIdValue>();
                    _statements.get(pid).put(entityId, currentValues);
                }
                currentValues.add(targetEntityId);
            }
        }
    }
    
    @Override
    public void batchIsFinished() {
        // For each pair of inverse properties (in each direction)
        for(Entry<PropertyIdValue,PropertyIdValue> propertyPair : _inverse.entrySet()) {
            // Get the statements made for the first
            for(Entry<EntityIdValue, Set<EntityIdValue>> itemLinks : _statements.get(propertyPair.getKey()).entrySet()) {
                // For each outgoing link
                for(EntityIdValue idValue : itemLinks.getValue()) {
                    // Check that they are in the statements made for the second
                    Set<EntityIdValue> reciprocalLinks = _statements.get(propertyPair.getValue()).get(idValue);
                    if (reciprocalLinks == null || !reciprocalLinks.contains(itemLinks.getKey())) {
                        important("missing-inverse-statements");
                    }
                }
            }
        }
    }

}
