package org.openrefine.wikidata.manifests.v1;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.CaseFormat;
import org.openrefine.wikidata.manifests.Constraints;
import org.openrefine.wikidata.manifests.constraints.*;

import java.util.HashMap;
import java.util.Map;

public class ConstraintsV1 implements Constraints {


    private static final Map<String, Class<? extends Constraint>> nameToConstraintClass = new HashMap<>();
    private Map<String, Constraint> nameToConstraint = new HashMap<>();

    static {
        registerConstraintClass(AllowedEntityTypesConstraint.class);
        registerConstraintClass(AllowedQualifiersConstraint.class);
        registerConstraintClass(AllowedUnitsConstraint.class);
        registerConstraintClass(CitationNeededConstraint.class);
        registerConstraintClass(ConflictsWithConstraint.class);
        registerConstraintClass(ContemporaryConstraint.class);
        registerConstraintClass(DifferenceWithinRangeConstraint.class);
        registerConstraintClass(DistinctValuesConstraint.class);
        registerConstraintClass(FormatConstraint.class);
        registerConstraintClass(IntegerConstraint.class);
        registerConstraintClass(InverseConstraint.class);
        registerConstraintClass(ItemRequiresStatementConstraint.class);
        registerConstraintClass(MandatoryQualifierConstraint.class);
        registerConstraintClass(MultiValueConstraint.class);
        registerConstraintClass(NoBoundsConstraint.class);
        registerConstraintClass(NoneOfConstraint.class);
        registerConstraintClass(OneOfConstraint.class);
        registerConstraintClass(OneOfQualifierValuePropertyConstraint.class);
        registerConstraintClass(PropertyScopeConstraint.class);
        registerConstraintClass(RangeConstraint.class);
        registerConstraintClass(SingleBestValueConstraint.class);
        registerConstraintClass(SingleValueConstraint.class);
        registerConstraintClass(SymmetricConstraint.class);
        registerConstraintClass(TypeConstraint.class);
        registerConstraintClass(ValueRequiresStatementConstraint.class);
        registerConstraintClass(ValueTypeConstraint.class);
    }

    public ConstraintsV1(ArrayNode constraints) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (JsonNode constraint : constraints) {
            String constraintName = constraint.path("name").textValue();
            if (nameToConstraintClass.containsKey(constraintName)) {
                Constraint constraintObject = mapper.convertValue(constraint, nameToConstraintClass.get(constraintName));
                nameToConstraint.put(constraintName, constraintObject);
            }
        }
    }

    @Override
    public AllowedEntityTypesConstraint getAllowedEntityTypesConstraint() {
        return getConstraint(AllowedEntityTypesConstraint.class);
    }

    @Override
    public AllowedQualifiersConstraint getAllowedQualifiersConstraint() {
        return getConstraint(AllowedQualifiersConstraint.class);
    }

    @Override
    public AllowedUnitsConstraint getAllowedUnitsConstraint() {
        return getConstraint(AllowedUnitsConstraint.class);
    }

    @Override
    public CitationNeededConstraint getCitationNeededConstraint() {
        return getConstraint(CitationNeededConstraint.class);
    }

    @Override
    public ConflictsWithConstraint getConflictsWithConstraint() {
        return getConstraint(ConflictsWithConstraint.class);
    }

    @Override
    public ContemporaryConstraint getContemporaryConstraint() {
        return getConstraint(ContemporaryConstraint.class);
    }

    @Override
    public DifferenceWithinRangeConstraint getDifferenceWithinRangeConstraint() {
        return getConstraint(DifferenceWithinRangeConstraint.class);
    }

    @Override
    public DistinctValuesConstraint getDistinctValuesConstraint() {
        return getConstraint(DistinctValuesConstraint.class);
    }

    @Override
    public FormatConstraint getFormatConstraint() {
        return getConstraint(FormatConstraint.class);
    }

    @Override
    public IntegerConstraint getIntegerConstraint() {
        return getConstraint(IntegerConstraint.class);
    }

    @Override
    public InverseConstraint getInverseConstraint() {
        return getConstraint(InverseConstraint.class);
    }

    @Override
    public ItemRequiresStatementConstraint getItemRequiresStatementConstraint() {
        return getConstraint(ItemRequiresStatementConstraint.class);
    }

    @Override
    public MandatoryQualifierConstraint getMandatoryQualifierConstraint() {
        return getConstraint(MandatoryQualifierConstraint.class);
    }

    @Override
    public MultiValueConstraint getMultiValueConstraint() {
        return getConstraint(MultiValueConstraint.class);
    }

    @Override
    public NoBoundsConstraint getNoBoundsConstraint() {
        return getConstraint(NoBoundsConstraint.class);
    }

    @Override
    public NoneOfConstraint getNoneOfConstraint() {
        return getConstraint(NoneOfConstraint.class);
    }

    @Override
    public OneOfConstraint getOneOfConstraint() {
        return getConstraint(OneOfConstraint.class);
    }

    @Override
    public OneOfQualifierValuePropertyConstraint getOneOfQualifierValuePropertyConstraint() {
        return getConstraint(OneOfQualifierValuePropertyConstraint.class);
    }

    @Override
    public PropertyScopeConstraint getPropertyScopeConstraint() {
        return getConstraint(PropertyScopeConstraint.class);
    }

    @Override
    public RangeConstraint getRangeConstraint() {
        return getConstraint(RangeConstraint.class);
    }

    @Override
    public SingleBestValueConstraint getSingleBestValueConstraint() {
        return getConstraint(SingleBestValueConstraint.class);
    }

    @Override
    public SingleValueConstraint getSingleValueConstraint() {
        return getConstraint(SingleValueConstraint.class);
    }

    @Override
    public SymmetricConstraint getSymmetricConstraint() {
        return getConstraint(SymmetricConstraint.class);
    }

    @Override
    public TypeConstraint getTypeConstraint() {
        return getConstraint(TypeConstraint.class);
    }

    @Override
    public ValueRequiresStatementConstraint getValueRequiresStatementConstraint() {
        return getConstraint(ValueRequiresStatementConstraint.class);
    }

    @Override
    public ValueTypeConstraint getValueTypeConstraint() {
        return getConstraint(ValueTypeConstraint.class);
    }

    private static void registerConstraintClass(Class<? extends Constraint> constraintClass) {
        nameToConstraintClass.put(convertClassName(constraintClass), constraintClass);
    }

    @SuppressWarnings("unchecked")
    private <T> T getConstraint(Class<T> constraintClass) {
        return (T) nameToConstraint.get(convertClassName(constraintClass));
    }

    private static String convertClassName(Class<?> clazz) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clazz.getSimpleName());
    }
}
