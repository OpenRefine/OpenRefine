
package com.google.refine.translate;

import java.util.List;

public interface TranslationService {

    List<String> translate(
            List<String> texts,
            TranslationLanguage sourceLanguage,
            TranslationLanguage targetLanguage) throws TranslationException;
}
