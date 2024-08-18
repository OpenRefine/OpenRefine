
package com.google.refine.expr.functions.strings;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import com.optimaize.langdetect.i18n.LdLocale;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.GrelTestBase;

public class DetectLanguageTest extends GrelTestBase {

    String functionName = "detectLanguage";

    @Test
    public void testDetectLanguage() {
        Map<String, LdLocale> samples = Map.of(
                "I am very sure this works", LdLocale.fromString("en"),
                "Je suis très sûr que cela fonctionne", LdLocale.fromString("fr"),
                "Estoy muy seguro de que esto funciona", LdLocale.fromString("es"),
                "Ich bin sehr sicher dass dies funktioniert", LdLocale.fromString("de"));

        samples.forEach((s, ldLocale) -> {
            assertEquals(invoke(functionName, s), ldLocale.getLanguage());
            assertTrue(invoke(functionName, s) instanceof String);
        });
    }

    @Test
    public void testDetectLanguageInvalidParams() {
        assertTrue(invoke(functionName) instanceof EvalError);
        assertTrue(invoke(functionName,
                "Estoy muy seguro de que esto funciona",
                "Ich bin sehr sicher dass dies funktioniert") instanceof EvalError);
    }

}
