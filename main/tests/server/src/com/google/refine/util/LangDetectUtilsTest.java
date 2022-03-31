
package com.google.refine.util;

import com.google.common.base.Optional;
import com.optimaize.langdetect.i18n.LdLocale;
import junit.framework.TestCase;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

public class LangDetectUtilsTest extends TestCase {

    @Test
    public void testDetect() throws IOException {
        List<SampleTexts> sampleTexts = List.of(
                new SampleTexts("I am very sure this works", "English", LdLocale.fromString("en")),
                new SampleTexts("Je suis très sûr que cela fonctionne", "French", LdLocale.fromString("fr")),
                new SampleTexts("Estoy muy seguro de que esto funciona", "Spanish", LdLocale.fromString("es")),
                new SampleTexts("Ich bin sehr sicher dass dies funktioniert", "German", LdLocale.fromString("de")));

        sampleTexts.forEach(sample -> {
            Optional<LdLocale> lang;
            try {
                lang = LangDetectUtils.detect(sample.getText());
                if (lang.isPresent()) {
                    assertEquals(lang.get(), sample.getLdLocale());
                    // print out that the language was detected
                    System.out.println(sample.getText() + " was detected as " + lang.get().getLanguage());
                } else {
                    System.out.println(sample.getText() + " was not detected");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }
}

class SampleTexts {

    private final String text;
    private final String language;
    private final LdLocale ldLocale;

    public SampleTexts(String text, String language, LdLocale ldLocale) {
        this.text = text;
        this.ldLocale = ldLocale;
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public String getLanguage() {
        return language;
    }

    public LdLocale getLdLocale() {
        return ldLocale;
    }
}
