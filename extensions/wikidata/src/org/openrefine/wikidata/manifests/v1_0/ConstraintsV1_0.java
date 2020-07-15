package org.openrefine.wikidata.manifests.v1_0;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.openrefine.wikidata.manifests.Constraints;
import org.openrefine.wikidata.manifests.constraints.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConstraintsV1_0 implements Constraints {


    private static final Map<String, Class<? extends Constraint>> nameToConstraintClass = new HashMap<>();
    private Map<String, Constraint> nameToConstraint = new HashMap<>();

    static {
        nameToConstraintClass.put("allowed_entity_types_constraint", AllowedEntityTypesConstraint.class);
        nameToConstraintClass.put("allowed_qualifiers_constraint", AllowedQualifiersConstraint.class);
        nameToConstraintClass.put("allowed_units_constraint", AllowedUnitsConstraint.class);
        nameToConstraintClass.put("citation_needed_constraint", CitationNeededConstraint.class);
        nameToConstraintClass.put("conflicts_with_constraint", ConflictsWithConstraint.class);
        nameToConstraintClass.put("contemporary_constraint", ContemporaryConstraint.class);
        nameToConstraintClass.put("difference_within_range_constraint", DifferenceWithinRangeConstraint.class);
        nameToConstraintClass.put("distinct_values_constraint", DistinctValuesConstraint.class);
        nameToConstraintClass.put("format_constraint", FormatConstraint.class);
        nameToConstraintClass.put("integer_constraint", IntegerConstraint.class);
        nameToConstraintClass.put("inverse_constraint", InverseConstraint.class);
        nameToConstraintClass.put("item_requires_statement_constraint", ItemRequiresStatementConstraint.class);
        nameToConstraintClass.put("mandatory_qualifier_constraint", MandatoryQualifierConstraint.class);
        nameToConstraintClass.put("multi_value_constraint", MultiValueConstraint.class);
        nameToConstraintClass.put("no_bounds_constraint", NoBoundsConstraint.class);
        nameToConstraintClass.put("none_of_constraint", NoneOfConstraint.class);
        nameToConstraintClass.put("one_of_constraint", OneOfConstraint.class);
        nameToConstraintClass.put("property_scope_constraint", PropertyScopeConstraint.class);
        nameToConstraintClass.put("range_constraint", RangeConstraint.class);
        nameToConstraintClass.put("single_best_value_constraint", SingleBestValueConstraint.class);
        nameToConstraintClass.put("single_value_constraint", SingleValueConstraint.class);
        nameToConstraintClass.put("symmetric_constraint", SymmetricConstraint.class);
        nameToConstraintClass.put("type_constraint", TypeConstraint.class);
        nameToConstraintClass.put("value_requires_statement_constraint", ValueRequiresStatementConstraint.class);
        nameToConstraintClass.put("value_type_constraint", ValueTypeConstraint.class);
    }

    public ConstraintsV1_0(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // underscore style -> camel style
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        ArrayNode constraints = (ArrayNode) node;
        for (Iterator<JsonNode> it = constraints.elements(); it.hasNext(); ) {
            JsonNode constraint = it.next();
            String constraintName = constraint.path("name").textValue();
            if (nameToConstraintClass.containsKey(constraintName)) {
                Constraint constraintObject = mapper.convertValue(constraint, nameToConstraintClass.get(constraintName));
                nameToConstraint.put(constraintName, constraintObject);
            }
        }
    }

    @Override
    public AllowedEntityTypesConstraint getAllowedEntityTypesConstraint() {
        return (AllowedEntityTypesConstraint) nameToConstraint.get("allowed_entity_types_constraint");
    }

    @Override
    public AllowedQualifiersConstraint getAllowedQualifiersConstraint() {
        return (AllowedQualifiersConstraint) nameToConstraint.get("allowed_qualifiers_constraint");
    }

    @Override
    public AllowedUnitsConstraint getAllowedUnitsConstraint() {
        return (AllowedUnitsConstraint) nameToConstraint.get("allowed_units_constraint");
    }

    @Override
    public CitationNeededConstraint getCitationNeededConstraint() {
        return (CitationNeededConstraint) nameToConstraint.get("citation_needed_constraint");
    }

    @Override
    public ConflictsWithConstraint getConflictsWithConstraint() {
        return (ConflictsWithConstraint) nameToConstraint.get("conflicts_with_constraint");
    }

    @Override
    public ContemporaryConstraint getContemporaryConstraint() {
        return (ContemporaryConstraint) nameToConstraint.get("contemporary_constraint");
    }

    @Override
    public DifferenceWithinRangeConstraint getDifferenceWithinRangeConstraint() {
        return (DifferenceWithinRangeConstraint) nameToConstraint.get("difference_within_range_constraint");
    }

    @Override
    public DistinctValuesConstraint getDistinctValuesConstraint() {
        return (DistinctValuesConstraint) nameToConstraint.get("distinct_values_constraint");
    }

    @Override
    public FormatConstraint getFormatConstraint() {
        return (FormatConstraint) nameToConstraint.get("format_constraint");
    }

    @Override
    public IntegerConstraint getIntegerConstraint() {
        return (IntegerConstraint) nameToConstraint.get("integer_constraint");
    }

    @Override
    public InverseConstraint getInverseConstraint() {
        return (InverseConstraint) nameToConstraint.get("inverse_constraint");
    }

    @Override
    public ItemRequiresStatementConstraint getItemRequiresStatementConstraint() {
        return (ItemRequiresStatementConstraint) nameToConstraint.get("item_requires_statement_constraint");
    }

    @Override
    public MandatoryQualifierConstraint getMandatoryQualifierConstraint() {
        return (MandatoryQualifierConstraint) nameToConstraint.get("mandatory_qualifier_constraint");
    }

    @Override
    public MultiValueConstraint getMultiValueConstraint() {
        return (MultiValueConstraint) nameToConstraint.get("multi_value_constraint");
    }

    @Override
    public NoBoundsConstraint getNoBoundsConstraint() {
        return (NoBoundsConstraint) nameToConstraint.get("no_bounds_constraint");
    }

    @Override
    public NoneOfConstraint getNoneOfConstraint() {
        return (NoneOfConstraint) nameToConstraint.get("none_of_constraint");
    }

    @Override
    public OneOfConstraint getOneOfConstraint() {
        return (OneOfConstraint) nameToConstraint.get("one_of_constraint");
    }

    @Override
    public PropertyScopeConstraint getPropertyScopeConstraint() {
        return (PropertyScopeConstraint) nameToConstraint.get("property_scope_constraint");
    }

    @Override
    public RangeConstraint getRangeConstraint() {
        return (RangeConstraint) nameToConstraint.get("range_constraint");
    }

    @Override
    public SingleBestValueConstraint getSingleBestValueConstraint() {
        return (SingleBestValueConstraint) nameToConstraint.get("single_best_value_constraint");
    }

    @Override
    public SingleValueConstraint getSingleValueConstraint() {
        return (SingleValueConstraint) nameToConstraint.get("single_value_constraint");
    }

    @Override
    public SymmetricConstraint getSymmetricConstraint() {
        return (SymmetricConstraint) nameToConstraint.get("symmetric_constraint");
    }

    @Override
    public TypeConstraint getTypeConstraint() {
        return (TypeConstraint) nameToConstraint.get("type_constraint");
    }

    @Override
    public ValueRequiresStatementConstraint getValueRequiresStatementConstraint() {
        return (ValueRequiresStatementConstraint) nameToConstraint.get("value_requires_statement_constraint");
    }

    @Override
    public ValueTypeConstraint getValueTypeConstraint() {
        return (ValueTypeConstraint) nameToConstraint.get("value_type_constraint");
    }
}
