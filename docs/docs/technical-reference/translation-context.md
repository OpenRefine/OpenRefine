#  Adding Context To Translation

Translation goes beyond the act of rendering spoken or written text from one language to another. Words could have various meanings depending on how, when, and where they are spoken. For example, the word *play* in English could mean *jouer* in French when using it as a verb or *pièce de théâtre* when referring to a theatrical performance.

For successful localization of any text, translation has to be in context.
Translating in context interprets a text from one language to another while ensuring that it communicates the message it is intended to.




# Weblate - web-based localization

As its [documentation](https://docs.weblate.org/en/latest/) says, Weblate is a copylefted libre software web-based continuous localization system.

In Weblate, translations are organized into projects which can contain components in logical sets. These components in turn contain translations into individual languages. Each component corresponds to a translatable file.


## How Weblate Supports Message Contextualization

Weblate supports contextualized translation in **written forms**. The application provides 3 options from which the translator can get an understanding of the meaning of the string:


- **Nearby strings:** This option displays the text in use. That is, the text is shown as it is used in a paragraph. Taking the example of *play* previously mentioned, in a sentence; *Hamlet by William Shakespeare is the best play I have ever read.* In this sentence the meaning of the word *play* is perfectly communicated


- **History**: Previous translations of the text (in the translator's language) are made available. For example, if the word is *play* and the words available in the translated language, say, French, are *jouer* and *jeu*, the translator understands that the word to be translated is a verb and not a theatrical performance.


- **Other languages:**  The translator also has access to any other available translation of the text. For example *play* in English and *pièce de théâtre* in French, help a Spanish translator to provide *obra de teatro* instead of *jugar* as the translated text.

Alongside these, there is also a glossary of words to help the translator. The words are in a source (original word) to target (translated word)  pair. Each has metadata outlining its age and source age

In summary, Weblate uses a **text-based** support for providing message contexts

## Outline Of How OpenRefine Could Implement the Weblate Support for Message Contextualization

### Setting Up The Glossary
As previously mentioned, every project in Weblate has a glossary. The glossary, as Weblate [defines](https://lstn.wolba.ch/weblate-terms/#:~:text=Every%20project%20on%20Weblate%20is,consistently%20translated%20throughout%20the%20project.), contains strings that may repeat throughout different components and ensures that these strings will be consistently translated throughout the project.

All OpenRefine projects should have glossaries with terminologies specific to OpenRefine. Words added to the glossary should be those that are used multiple times in the application. For example clusters, facets, reconciliation, metadata, schema, tags, etc