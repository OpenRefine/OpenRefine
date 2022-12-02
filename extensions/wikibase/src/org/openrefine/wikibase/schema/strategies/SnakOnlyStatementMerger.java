
package org.openrefine.wikibase.schema.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikibase.schema.WbStatementExpr;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.NoValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.SomeValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.datamodel.implementation.SomeValueSnakImpl;
import org.wikidata.wdtk.datamodel.implementation.NoValueSnakImpl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Merging strategy which only looks at the main value of statements (in addition to their property, but that is
 * expected). This is what QuickStatements does.
 * 
 * @author Antonin Delpeuch
 *
 */
public class SnakOnlyStatementMerger implements StatementMerger {

    ValueMatcher valueMatcher;

    @JsonCreator
    public SnakOnlyStatementMerger(
            @JsonProperty("valueMatcher") ValueMatcher valueMatcher) {
        this.valueMatcher = valueMatcher;
    }

    @Override
    public boolean match(Statement existing, Statement added) {
        Snak existingSnak = existing.getMainSnak();
        Snak addedSnak = added.getMainSnak();

        return match(existingSnak, addedSnak);
    }

    @Override
    public Statement merge(Statement existing, Statement added) {
        List<SnakGroup> existingQualifiers = existing.getQualifiers();
        List<SnakGroup> addedQualifiers = added.getQualifiers();

        // flatten snak groups
        List<Snak> existingSnaks = flatten(existingQualifiers);
        List<Snak> addedSnaks = flatten(addedQualifiers);

        List<Snak> mergedSnaks = new ArrayList<>(existingSnaks);
        for (Snak addedSnak : addedSnaks) {
            boolean matchingSnakFound = mergedSnaks.stream()
                    .anyMatch(existingSnak -> match(existingSnak, addedSnak));
            if (!matchingSnakFound) {
                mergedSnaks.add(addedSnak);
            }
        }
        List<SnakGroup> groupedQualifiers = WbStatementExpr.groupSnaks(mergedSnaks);
        Claim newClaim = Datamodel.makeClaim(existing.getSubject(), existing.getMainSnak(), groupedQualifiers);
        List<Reference> references = mergeReferences(existing.getReferences(), added.getReferences());
        return Datamodel.makeStatement(newClaim, references, existing.getRank(), existing.getStatementId());
    }

    @JsonProperty("valueMatcher")
    public ValueMatcher getValueMatcher() {
        return valueMatcher;
    }

    /**
     * Matches two snaks using the underlying value matcher. The snaks must have the same property id to match.
     * 
     * @param existingSnak
     * @param addedSnak
     * @return
     */
    public boolean match(Snak existingSnak, Snak addedSnak) {
        // Deliberately only comparing the pids and not the siteIRIs to avoid spurious mismatches due to federation
        if (!existingSnak.getPropertyId().getId().equals(addedSnak.getPropertyId().getId())) {
            return false;
        } else if (existingSnak instanceof NoValueSnak) {
            return addedSnak instanceof NoValueSnak;
        } else if (existingSnak instanceof SomeValueSnak) {
            return addedSnak instanceof SomeValueSnak;
        } else if (addedSnak instanceof ValueSnak) {
            // existingSnak is also a ValueSnak per the cases above
            Value existingValue = ((ValueSnak) existingSnak).getValue();
            Value addedValue = ((ValueSnak) addedSnak).getValue();
            return valueMatcher.match(existingValue, addedValue);
        } else {
            // mismatching value / somevalue / novalue between addedSnak and existingSnak
            return false;
        }
    }

    public List<Reference> mergeReferences(List<Reference> existing, List<Reference> added) {
        List<Reference> allReferences = new ArrayList<>(existing);
        Set<Reference> seenReferences = new HashSet<>(existing);
        for (Reference reference : added) {
            if (!seenReferences.contains(reference)) {
                seenReferences.add(reference);
                allReferences.add(reference);
            }
        }
        return allReferences;
    }

    public static List<Snak> flatten(List<SnakGroup> snakGroups) {
        return snakGroups.stream()
                .flatMap(group -> group.getSnaks().stream())
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "SnakOnlyStatementMerger [valueMatcher=" + valueMatcher + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueMatcher);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SnakOnlyStatementMerger other = (SnakOnlyStatementMerger) obj;
        return Objects.equals(valueMatcher, other.valueMatcher);
    }

}
