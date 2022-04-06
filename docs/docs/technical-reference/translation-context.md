# Translation Context

Translation goes beyond the act of rendering spoken or written text from one language to another. Words could have various meanings depending on how, when, and where they are spoken. For example, the word *play* in English could mean *jouer* in French when using it as a verb or *pièce de théâtre* when referring to a theatrical performance.

For successful localization of any text, translation has to be in context.
Translating in context interprets a text from one language to another while ensuring that it communicates the message it is intended to.



# Weblate - web-based localization

As its [documentation](https://docs.weblate.org/en/latest/) says, Weblate is a copylefted libre software web-based continuous localization system.

In Weblate, translations are organized into projects which can contain components in logical sets. These components in turn contain translations into individual languages. Each component corresponds to a translatable file.


## Weblate Internals

The Weblate is a Django-based application and consists of several Django applications. Some modules include:

- **`accounts`**: Concerned with user accounts, profiles, and notifications.
- **`addons`**: To tweak Weblate behavior
- **`api`**: API based on [Django REST framework](https://www.django-rest-framework.org/).
- **`auth`**: Concerned with authentication and permissions.
- **`billing`**: An optional billing module.
- **`lang`**: Module defining language and plural models.
- **`memory`**: Built-in translation memory.
- **`main`**: Main module handling translations.

### Data Schemes: Translation Memory Schema

Weblate uses [JSON Schema](https://json-schema.org/) to define layout of external JSON files.

A **translation memory** (**TM**) is a database which stores text that have been translated in pairs called **Translation Units** (**TUs**). The pair consists of a source (original language) and a target (translated language).

In Weblate a built-in TM was introduced in version and consists of the following:
-   Manually imported translation memory.
-   Automatically stored translations performed in Weblate
-   Automatically imported past translations.

In version 3.2 **translation memory scope** was introduced to allow both privacy and sharing of translations. Some behavior includes:

- Importing arbitrary translation memory data using a command which makes memory content available to all users and projects.
- Storing all user translations automatically in the personal translation memory of each respective user.
- Storing all translations within a project automatically in a translation memory that is only available in that project.
  -Enabling projects with shared translation memory turned on to store all of its translations in a shared translation memory available to all projects.


A user interface is used to manage TMs. It can be used to download, wipe or import translation memory. There are several management commands to manipulate the translation memory content. These operate on the translation memory as a whole, unfiltered by scopes (unless requested by parameters):

- `dump_memory`: Exports the memory into JSON
- `import_memory`: Imports TMX or JSON files into the translation memory