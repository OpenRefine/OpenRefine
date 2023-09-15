---
id: translating
title: Translate the OpenRefine interface 
sidebar_label: Translate the OpenRefine interface
---

Currently supported languages include English, Spanish, Chinese, French, Hebrew, Italian and Japanese.

![Translation status](https://hosted.weblate.org/widgets/openrefine/-/287x66-grey.png)

You can help translate OpenRefine into your language by visiting [Weblate](https://hosted.weblate.org/engage/openrefine/?utm_source=widget) which provides a web based UI to edit and add translations and sends automatic pull requests back to our project.

Click to help translate --> [Weblate](https://hosted.weblate.org/engage/openrefine/?utm_source=widget)

## User entry of language data ## {#user-entry-of-language-data-}
 
Localized strings are entered in a .json file, one per language. They are located in the folder `main/webapp/modules/core/langs/` in a file named `translation-xx`.json, where xx is the language code (i.e. fr for French).

### Simple case of localized string ### {#simple-case-of-localized-string-}
This is an example of a simple string, with the start of the JSON file. This example is for French.
```
{
    "name": "Français",
    "core-index/help": "Aide",
    (… more lines)
}
```

So the key `core-index/help` will render as `"Aide"` in French.

### Localization with a parameterized value ### {#localization-with-a-parameterized-value-}
In this example, the name of the column (represented by `$1` in this example), will be substituted with the string of the name of the column.

`"core-facets/edit-facet-title": "Cliquez ici pour éditer le nom de la facette\nColonne : $1",`

### Localization with a singular/plural value ### {#localization-with-a-singularplural-value-}
In this example, one of the parameter will have a different string depending if the value is 1 or another value.
In this example, the string for page, the second parameter, `$2`, will have an « s » or not depending on the value of `$2`.

`"core-views/goto-page": "$1 de $2 {{plural:$2|page|pages}}"`

## Front End Coding {#front-end-coding}

The OpenRefine front end has been localized using the [Wikidata jquery.i18n library](https://github.com/OpenRefine/OpenRefine/pull/1285. The localized text is stored in a JSON dictionary on the server and retrieved with a new OpenRefine command.

### Adding a new string {#adding-a-new-string}

There should be no hard-coded language strings in the HTML or JSON used for the front end.  If you need a new string, first check the existing strings to make sure there isn't an equivalent string, **in an equivalent context**, that you can reuse.  Context is important because it can affect how the same literal English text is translated. This cuts down on the amount of text which needs to be translated.

Strings should be entire sentences or phrases and should include substitution variables for any parameters. Do not concatenate strings in either Java or Javascript (or implicitly by laying them out in a specific order). So, instead of `"You have " + count + " row(s)"` (or worse `count != 1 ? " rows" : " row"`), internationalize everything together so that it can be translated taking into account word ordering and plurals for different languages, ie `"You have $1 {{plural $1: row|rows}}"`, passing the parameter(s) into the `$.i18n` call.

If there's no string you can reuse, allocate an available key in the appropriate translation dictionary and add the default string, e.g.

```json
...,
"section/newkey": "new default string for this key",
...
```

and then set the text (or HTML) of your HTML element using i18n helper method. So given an HTML fragment like:
```html
<label id="new-element-id">[untranslated text would have appeared here before]</label>
```
we could set its text using:
```
$('#new-html-element-id').text($.i18n('section/newkey']));
```
or, if you need to embed HTML tags:
```
$('#new-html-element-id').html($.i18n('section/newkey']);
```

### Adding a new language {#adding-a-new-language}

The language dictionaries are stored in the `langs` subdirectory for the module e.g.

* https://github.com/OpenRefine/OpenRefine/tree/master/main/webapp/modules/core/langs for the main interface
* https://github.com/OpenRefine/OpenRefine/tree/master/extensions/gdata/module/langs for google spreadsheet connection
* https://github.com/OpenRefine/OpenRefine/tree/master/extensions/database/module/langs for database via JDBC
* https://github.com/OpenRefine/OpenRefine/tree/master/extensions/wikidata/module/langs for Wikidata

To add support for a new language, copy `translation-en.json` to `translation-<locale>.json` and have your translator translate all the value strings (ie right hand side).

#### Main interface {#main-interface}
 The translation is best done [with Weblate](https://hosted.weblate.org/engage/openrefine/?utm_source=widget). Files are periodically merged by the developer team.

Run the latest (hopefully cloned from github) version and check whether translated words fit to the layout. Not all items can be translated word by word, especially into non-Ìndo-European languages.

If you see any text which remains in English even when you have checked all items, please create bug report in the issue tracker so that the developers can fix it.

#### Extensions {#extensions}

Extensions can be translated via Weblate just like the core software.

The new extension for Wikidata contains lots of domain-specific concepts, with which you may not be familiar. The Wikidata may not have reconciliation service for your language. I recommend checking the glossary(https://www.wikidata.org/wiki/Wikidata:Glossary) to be consistent.

By default, the system tries to load the language file corresponding to the currently in-use browser language. To override this setting a new menu item ("Language Settings") has been added at the index page.
To support a new language file, the developer should add a corresponding entry to the dropdown menu in this file: `/OpenRefine/main/webapp/modules/core/scripts/index/lang-settings-ui.html`. The entry should look like:
```javascript
<option value="<locale>">[Language Label]</option>
```

## Server / Backend Coding {#server--backend-coding}

Currently no back end functions are translated, so things like error messages, undo history, etc may appear in English form. Rather than sending raw error text to the front end, it's better to send an error code which is translated into text on the front end. This allows for multiple languages to be supported.
