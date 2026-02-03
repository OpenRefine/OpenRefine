Feature: GREL toNumber function partitioning

Feature under test

- `toNumber` converts one non-null argument into a numeric representation (returns Long for integer-like strings, Double for floating/scientific strings, returns the Number instance unchanged for Number inputs, otherwise returns EvalError).

Partitioning scheme (English)

We partition the possible inputs to `toNumber` into the following classes. Representative values and rationale are given.

1. Missing or null argument
   - Representative: no argument, or explicit null
   - Expected outcome: EvalError (expects one non-null arg)

2. Empty string
   - Representative: ""
   - Expected outcome: EvalError (unable to parse as number)

3. Integer-like strings (no dot) that parse as Long
   - Representative: "123", "00123", "-42", "+77"
   - Expected outcome: Long with the same numeric value

4. Decimal strings (contain a dot) that parse as Double
   - Representative: "123.456", "001.234"
   - Expected outcome: Double close to the numeric value

5. Scientific notation strings
   - Representative: "1e3", "2E2"
   - Expected outcome: Double

6. Non-numeric strings or strings with trailing/embedded letters
   - Representative: "abc", "12a"
   - Expected outcome: EvalError (unable to parse)

7. Number inputs (already a Number instance)
   - Representative: Integer, Long, Double values
   - Expected outcome: the same Number instance returned (preserved)

8. Non-String objects whose toString() yields a numeric string
   - Representative: new StringBuilder("88")
   - Expected outcome: parsed as per string content (Long/Double)

Tests added

- `ToNumberPartitionTests.java` implements one test method per partition or closely related partitions. It uses the existing `GrelTestBase.invoke` helper to evaluate `toNumber` as in the project's testing style.

How to run

From repository root:

```bash
# Run only GREL module tests
mvn -pl modules/grel -am test

# Or run the specific test class
mvn -pl modules/grel -am -Dtest=com.google.refine.expr.functions.ToNumberPartitionTests test
```

Notes

- These tests record the current behaviour. If the `toNumber` semantics change (for example, trimming whitespace before parsing is added), tests may need updates.
- The tests intentionally avoid relying on platform-specific behaviors; they use representative values that exercise the relevant code paths in `ToNumber.call`.
