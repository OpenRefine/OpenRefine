
package com.google.refine.expr.functions.strings;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.optimaize.langdetect.i18n.LdLocale;
import org.junit.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Map;

public class DetectLanguageTest extends RefineTest {

    String functionName;

    @Override
    @BeforeTest
    public void init() {
        functionName = "detectLanguage";
    }

    @Test
    public void testDetectLanguage() {
        Map<String, LdLocale> samples = Map.of(
                "I am very sure this works", LdLocale.fromString("en"),
                "Je suis très sûr que cela fonctionne", LdLocale.fromString("fr"),
                "Estoy muy seguro de que esto funciona", LdLocale.fromString("es"),
                "Ich bin sehr sicher dass dies funktioniert", LdLocale.fromString("de"));

        samples.forEach((s, ldLocale) -> {
            Assert.assertEquals(invoke(functionName, s), ldLocale.getLanguage());
            Assert.assertTrue(invoke(functionName, s) instanceof String);
        });
    }

    @Test
    public void testDetectLanguageInvalidParams() {
        Assert.assertTrue(invoke(functionName) instanceof EvalError);
        Assert.assertTrue(invoke(functionName,
                "Estoy muy seguro de que esto funciona",
                "Ich bin sehr sicher dass dies funktioniert") instanceof EvalError);
    }

}
