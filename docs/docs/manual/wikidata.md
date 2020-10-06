---
id: wikidata
title: Wikidata
sidebar_label: Wikidata
---

## Overview

OpenRefine includes powerful ways to both pull data from Wikidata and add data to it. 

OpenRefine’s connections to Wikidata were formerly an optional extension, but are now installed automatically with the downloadable package. The Wikidata extension can be removed manually by navigating to your OpenRefine installation folder, and then looking inside `webapp/extensions/` and deleting the `wikidata` folder inside. 

You do not need a Wikidata account to reconcile your local OpenRefine project to Wikidata. If you wish to upload your cleaned dataset to Wikidata, you will need to register an account, and authorize OpenRefine with that account. 

The best source for information about how OpenRefine works with Wikidata is [on Wikidata itself, under Tools](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine). This has tutorials, guidelines on editing, and spaces for discussion and help.

Everything true about the way OpenRefine works with Wikidata is also true about any other Wikibase. Anything built on the same Wikibase platform will offer a reconciliation endpoint that works the same. To find a Wikibase reconciliation URL, 

## Reconciling with Wikidata

The [Wikidata reconciliation service](reconciling) for OpenRefine [supports](https://reconciliation-api.github.io/testbench/):
*   A large number of potential types to reconcile against
*   Previewing and viewing entities
*   Suggesting entities, types, and properties
*   Augmenting your project with more information pulled from Wikidata. 

You can find the documentation and further resources [here](https://wikidata.reconci.link/).

For the most part, Wikidata reconciliation behaves the same way other reconciliation processes do, but there are a few processes and features specific to Wikidata. 

### Language settings

You can install a version of the Wikidata reconciliation service that uses your language. First, you need the language code: this is the [two-letter WP code found on this list](https://en.wikipedia.org/wiki/List_of_Wikipedias), or in the domain name of the desired Wikipedia (for instance, “fr” if your Wikipedia is [https://fr.wikipedia.org/wiki/](https://fr.wikipedia.org/wiki/)).

Then, open the reconciliation window (under <span class="menuItems">Reconcile</span> → <span class="menuItems">Start reconciling...</span>) and click <span class="menuItems">Add Standard Service</span>. The URL is `https://openrefine-wikidata.toolforge.org/fr/api` where “fr” is replaced by your language code.

When reconciling using this interface, items and properties will be displayed in your language if a translation is available. The matching score of the reconciliation is not influenced by your choice of language: items are matched by considering all labels and keeping the best possible match. So the language of your dataset is irrelevant to the choice of the language for the reconciliation interface.

### Restricting matches by type

In Wikidata, types are items themselves. For instance, the [university of Ljubljana (Q1377)](https://www.wikidata.org/wiki/Q1377) has the type [public university (Q875538)](https://www.wikidata.org/wiki/Q875538), using the [instance of (P31)](https://www.wikidata.org/wiki/Property:P31) property. 
Types can be subclasses of other types, using the [subclass of (P279)](https://www.wikidata.org/wiki/Property:P279) property. For instance, [public university (Q875538)](https://www.wikidata.org/wiki/Q875538) is a subclass of [university (Q3918)](https://www.wikidata.org/wiki/Q3918). You can visualize these structures with the [Wikidata Graph Builder](https://angryloki.github.io/wikidata-graph-builder/). 

When you select or enter a type for reconciliation, OpenRefine will include that type and all its subtypes. For instance, if you select [university (Q3918)](https://www.wikidata.org/wiki/Q3918), then [university of Ljubljana (Q1377)](https://www.wikidata.org/wiki/Q1377) will be a possible match.

Some items may not yet be set as an instance of anything, because Wikidata is crowdsourced. If you restrict reconciliation to a type, these items will not appear in the results, except as a fallback, and will have a lower score.

### Reconciling via unique identifiers

You can supply a column of unique identifiers directly to Wikidata in order to pull more data, but [these strings will not be “reconciled” against the external dataset](reconciling#reconciling-with-unique-identifiers). Apply the operation <span class="menuItems">Reconcile</span> → <span class="menuItems">Use values as identifiers</span> on your column of identifiers. All cells will appear as dark blue “confirmed” matches. Some of the “matches” may be errors, which you will need to hover over or click on to identify.

If the same external identifier is assigned to multiple Wikidata items, all of the items are returned as candidates, with none automatically matched.

### Property paths

This feature is specific to the reconciliation interface for Wikidata. Sometimes, when including extra columns as properties in reconciliation, the relation between the reconciled item and the disambiguating column is not direct - it is not represented as a property itself. For example, take this dataset of cities:

|City|Country|
|---|---|
|Oxford|GB|
|Paris|FR|
|Geneva|CH|
|Cambridge|GB|
|Cambridge|US|
|London|CA|
|London|GB|

To fetch the country code from an item representing a city, you need to follow two properties. First, follow [country (P17)](https://www.wikidata.org/wiki/Property:P17) to get to the item for the country in which this city is located, then follow [ISO 3166-1 alpha-2 code (P297)](https://www.wikidata.org/wiki/Property:P297) to get the two-letter code string.

This is supported by the reconciliation interface, with a syntax inspired by [SPARQL property paths](https://www.w3.org/TR/sparql11-property-paths/): include the “country” column, and for <span class="menuItems">As Property</span> enter the sequence of property identifiers separated by slashes: “P17/P297.”

This additional information can allow OpenRefine to disambiguate namesakes, at least to the country level. “Cambridge, US” is still ambiguous, so there will be multiple items with a perfect matching score, but “Oxford, GB” successfully disambiguates one particular city from other “Oxfords.” 

The endpoint currently supports two property combinators: /, to concatenate two paths as above, and |, to compute the union of the values yielded by two paths. Concatenation / has precedence over disjunction |. The dot character . can be used to denote the empty path. For instance, the following property paths are equivalent:
*   P17|P749/P17
*   P17|(P749/P17)
*   (.|P749)/P17

They fetch the [country (P17)](https://www.wikidata.org/wiki/Property:P17) of an item or that of its [parent organization (P749)](https://www.wikidata.org/wiki/Property:P17).

### Terms: labels, descriptions, aliases, and sitelinks

Entities on Wikidata have what they call “terms:” labels, descriptions, aliases, and sitelinks. Each entity has human-readable preferred names (labels) in one or more languages. Each label comes with a description field, and a space for alternative labels in that language. Entities also can have a link to the related Wikipedia page in one or more langauges. 

For example, [Q5](https://www.wikidata.org/wiki/Q5) has the English label “human” and the aliases “person” and “people,” with a description including “common name of Homo sapiens;” it also has the Italian label “umano” and the alias “persona” with a description “specie a cui appartiene il genere umano.” The English sitelink is “https://en&#46;wikipedia.org/wiki/Human” but Italian Wikipedia has no equivalent article attached to Q5. Not every language will have values, but these terms can still be useful for reconciling. 

After reconciliation, you can extend your data with these terms. You can fetch aliases for a given term, or labels in another languages, by using <span class="menuItems">Edit column</span> → <span class="menuItems">Add columns from reconciled values....</span>. 

You can refer to a term in a specific language by manually entering in a three-letter code. The first letter is the term (“L” for label, “D” for description, “A” for aliases, “S” for sitelink) and the next two are the language code. For example:

*   `Len` for Label in English
*   `Dfi` for Description in Finnish
*   `Apt` for Alias in Portuguese
*   `Sde` for Sitelink in German

Enter in the three-letter code manually in the <span class="menuItems">Add Property</span> field. You can manually enter as many term/language combinations as you require. 

No language fall-back is performed when retrieving the values.

### Comparing values

By default, data in OpenRefine and entities in Wikidata are compared by string fuzzy-matching. There are some exceptions to this:
*   If you are [reconciling by unique identifiers](#reconciling-with-unique-identifiers), then it confirms the exact strings without matching.
*   If the values are integers, exact equality between integers is used.
*   If the values are floating point numbers, the score is 100 if they are equal and decreases towards 0 as their absolute difference increases.
*   If the values are coordinates (specified in the “lat,lng” format on OpenRefine's side), then the matching score is 100 when they are equal and decreases as their distance increases. Currently a score of 0 is reached when the points are 1 kilometre away from each other.

Sometimes, we need a more specific matching on sub-parts of these values. It is possible to select these parts for matching by appending a modifier at the end of the property path:
*   @lat and @lng: latitude and longitude of geographical coordinates (float)
*   @year, @month, @day, @hour, @minute and @second: parts of a time value (integer). They are returned only if the precision of the Wikidata value is good enough to define them.
*   @isodate: returns a date in the ISO format 1987-08-23 (string). A value is always returned.
*   @iso: returns the date and time in the ISO format 1996-03-17T04:15:00+00:00. A value is always returned. 
*   @urlscheme (“https”), @netloc (“www&#46;wikidata.org”) and @urlpath (“/wiki/Q42”) can be used to perform exact matching on parts of URLs.

For times and dates, all values are returned in the UTC time zone.

For instance, if you want to reconcile people by their birthdates, but you only have the month and day, split the birthday dates into two columns, for month and day. Then reconcile using the [date of birth (P569)](https://www.wikidata.org/wiki/Property:P569) property using the  parameters `P569@month` and `P569@day` with your included columns.

## Editing Wikidata with OpenRefine

The best resource is the [Editing section](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing) on Wikidata.

As a user-maintained data source, Wikidata can be edited by anyone. OpenRefine makes it simple to upload information in bulk. You simply need to get your information into the correct format, and ensure that it is new (not redundant to information already on Wikidata) and does not conflict with existing Wikidata information.

Wikidata is built by creating entities (such as people, organizations, or places, identified with unique numbers starting with Q), defining properties (unique numbers starting with P), and using properties to define relationships between entities (a Q has a property P, with a value of another Q). 

For example, you may wish to create entities for local authors and the books they've set in your community. Each writer will be an entity with the occupation [author (Q482980)](https://www.wikidata.org/wiki/Q482980), each book will be an entity with  [literary work (Q7725634)](https://www.wikidata.org/wiki/Q7725634), and books will be related to authors through a property [author (P50)](https://www.wikidata.org/wiki/Property:P50). Books can have places where they are set, with [setting (Q617332)](https://www.wikidata.org/wiki/Q617332). In OpenRefine, you'll need a column of publication titles that you have reconciled (and then created new items where needed); each publication will have one or more locations in a “setting” column, which is also reconciled to municipalities or regions where they exist (and have new items where needed). Then you can add those new relationships to each book, and create new entities for both books and places.

There is a list of [tutorials and walkthroughs on Wikidata](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing) that will allow you to see the full process. You can save your schemas and drafts in OpenRefine, and your progress stays in draft until you are sure you’re ready to upload it to Wikidata. You can also find information on [how to design a schema](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Schema_alignment) and [how OpenRefine evaluates your proposed edits for issues](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Quality_assurance).

Batches of edits to Wikidata that are created with OpenRefine can be undone. You can test out the uploading process by reconciling to several “sandbox” entities created specifically for drafting edits and learning about Wikidata:
* https://www.wikidata.org/wiki/Q4115189
* https://www.wikidata.org/wiki/Q13406268
* https://www.wikidata.org/wiki/Q15397819
* https://www.wikidata.org/wiki/Q64768399

If you upload edits that are redundant (that is, all the statements you want to make have already been made), nothing will happen. If you upload edits that conflict with existing information (such as a different birthdate than one already in Wikidata), it will be added as a second statement. OpenRefine produces no warnings as to whether your data replicates or conflicts existing Wikidata elements. 

You can use OpenRefine's previews to look at the target Wikidata elements and see what information they already have, and whether the elements' histories have had similar edits reverted in the past. 

Wikidata is unlike Wikipedia in that there are fewer editors supervising consistency. Many entities' discussion pages are not used; rather, discussion happens on individual users' pages and on project pages. 

### Edit Wikidata schema

The best resource is the [Schema alignment page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Schema_alignment) on Wikidata.

A [schema](https://en.wikipedia.org/wiki/Database_schema) is the plan for how to structure information in a database. In OpenRefine, the schema operates as a template for how Wikidata edits should be applied: how to translate your tabular data into statements. With a schema, you can:
*   preview the Wikidata edits and inspect them manually;
*   analyze and fix any issues raised automatically by the tool;
*   upload your changes to Wikidata by logging in with your own account;
*   export the changes to the QuickStatements v1 format.

For example, if your dataset has columns for authors, publication titles, and publication years, your schema can be conceptualized as: [publication title] has the author [author], and was published in [publication year]. To establish these facts, you need to establish one or more columns as “items,” for which you will make “statements” that relate them to other columns. 

You can export any schema you create, and import an existing schema for use with a new dataset. This can help you work in batches on a large amount of data with a minimum of redundant labor.

Once you select <span class="menuItems">Edit Wikidata schema</span> under the <span class="menuItems">Extensions</span> dropdown menu, your project interface will change. You’ll see new tabs added to the right of “X rows/records" in the grid header: “Schema,” “Issues,” and “Preview.” You can now flip between the tabular grid format of your dataset, or the screens that allow you to prepare data for uploading. 

OpenRefine presents you with an easy visual way to map out the relationships in your dataset. Each of the columns of your project will appear at the top of the sceren, and you can simply drag and drop them into the appropriate slots. To get start, select one column as an item. 

![A screenshot of the schema construction window in OpenRefine.](/img/wikidata-schema.png)

There is [a Wikidata tutorial on how OpenRefine handles Wikidata schema](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Tutorials/Basic_editing).

#### Editing terms with your schema

You may wish to include edits to [terms](#terms-labels-descriptions-aliases-and-sitelinks) (labels, aliases, descriptions, or sitelinks) as well as establishing relationships between entities. 

For example, you may wish to upload pseudonyms, pen names, maiden or married names for historical authors. You can do so by putting the preferred names in one column of your dataset and alternative names in another column. In the schema interface, add an item for the preferred values, then click “Add term” on the right-hand side of the screen. Select “Alias” from the dropdown, enter in “English” in the language field, and drop your alternative names column into the space. 

Terms must always have a language selected. You cannot edit multiple languages at once, unless you drop a suitable column into the “language” field. For example, if you had translated publication titles, with data in the format

|English title|Translated title|Translation language|
|---|---|---|
|Possession|Besessen|German|
||Обладать|Russian|
|Disgrace|Disgrâce|French|
||Vergogna|Italian|
|Wolf Hall|En la corte del lobo|Spanish|
||ウルフ・ホール|Japanese|

You could upload translated titles to “Label” with the language from “Translation language.” You may wish to fetch the two-letter language code and use that instead for better language matches.

### Manage Wikidata account

To edit Wikidata directly from OpenRefine, you must have a Wikidata account and log in with it in OpenRefine. OpenRefine can only upload edits with Wikidata user accounts that are “[autoconfirmed](https://www.wikidata.org/wiki/Wikidata:Autoconfirmed_users)” - that is, accounts that have more than 50 edits and have existed for longer than four days. 

Use the Extensions menu to select <span class="menuItems">Manage Wikidata account</span> and you will be presented with the following window:

![The Wikidata authorization window in OpenRefine.](/img/wikidata-login.png)

For security reasons, it is suggested that you not use your main account authorization with OpenRefine. Wikidata allows you to set special passwords to access your account through software. You can find this setting for your account at [https://www.wikidata.org/wiki/Special:BotPasswords](https://www.wikidata.org/wiki/Special:BotPasswords) once logged in. Creating bot access will prompt you for a unique name, and allow you to enable the following required settings:
* High-volume editing
* Edit existing pages
* Create, edit, and move pages

It will then generate a username (in the form of “yourwikidatausername@yourbotname”) and password for you to use with OpenRefine.

If your account or your bot is not properly authorized, OpenRefine will not display a warning or error when you try to upload your edits.

You may also wish to store your unencrypted username and password in OpenRefine, saved locally to your computer. For security reasons, you may wish to leave this box unchecked. You can save your OpenRefine-specific bot password in your browser or with a password management tool. 

### Import and export schema

You can save time on repetitive processes by defining a schema on one project, then exporting it and importing for use on new datasets in the future. Or you and your colleagues can share a schema with each other to coordinate your work. 

You can export a schema from a project using <span class="menuItems">Export</span> → <span class="menuItems">Wikidata schema</span>, or by using <span class="menuItems">Extensions</span> → <span class="menuItems">Export schema</span>. OpenRefine will generate a JSON file for you to save and share. You may experience issues with pop-up windows in your browser: consider allowing pop-ups for the OpenRefine URL (`127.0.0.1`) from now on.

You can import a schema using <span class="menuItems">Extensions</span> → <span class="menuItems">Import schema</span>. You can upload a JSON file, or paste JSON statements directly into a field in the window. An imported schema will look for columns with the same names, and you will see an error message if your project doesn't contain matching columns.

### Upload edits to Wikidata

The best resource is the [Uploading page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Uploading) on Wikidata.

There are two menu option for applying your edits to Wikidata. Under <span class="menuItems">Export</span> you will see <span class="menuItems">Wikidata edits...</span> and under <span class="menuItems">Extensions</span> you will see <span class="menuItems">Upload edits to Wikidata</span>. Both will bring up the same window for you to [log in with your Wikidata account](#manage-wikidata-account). 

Once you are authorized, you will see a window with any outstanding issues. You can ignore these issues, but we recommend you resolve them. 

If you are ready to upload your edits, you can provide an “Edit summary” - a short message describing the batch of edits you are making. It can be helpful to leave notes for yourself, such as “batch 1: authors A-G” or other indicators of your workflow progress. OpenRefine will show the progress of the upload as it is happening, but does not show a confirmaton window. 

If you have made edits successfully, you will see them on [your Wikidata user contributions page](https://www.wikidata.org/wiki/Special:Contributions/), and on the [Edit groups page](https://editgroups.toolforge.org/). 

All edits can be undone from this interface.

### QuickStatements export

Your OpenRefine data can be exported in a format recognized by [QuickStatements](https://www.wikidata.org/wiki/Help:QuickStatements), a tool that creates Wikidata edits using text commands. OpenRefine generates “version 1” QuickStatements commands. In order to use QuickStatements, you must authorize it with a Wikidata account (it may appear as “MediaWiki” when you authorize). 

Any dataset can be converted into QuickStatements text commands. You can follow the steps listed on [this page](https://www.wikidata.org/wiki/Help:QuickStatements#Running_QuickStatements). 

Under the <span class="menuItems">Export</span> menu, look for <span class="menuItems">QuickStatements file</span>; under <span class="menuItems">Extensions</span> look for <span class="menuItems">Export to QuickStatements</span>. Exporting your schema from OpenRefine will generated a text file called `statements.txt` by default. Paste the contents of the text file into a new QuickStatements batch using version 1. You can find version 1 of the tool (no longer maintained) [here](https://wikidata-todo.toolforge.org/quick_statements.php). The text commands will be processed into Wikidata edits and previewed for you to review before submitting. 

There are advantages to using QuickStatements rather than uploading your edits directly to Wikidata, including the way QuickStatements resolves duplicates and redundancies. You can learn more on QuickStatements' [Help page](https://www.wikidata.org/wiki/Help:QuickStatements), and on OpenRefine's [Uploading page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Uploading).

### Schema alignment

The best resource is the [Schema alignment page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Schema_alignment) on Wikidata.

### Issue detection

The best resource is the [Quality assurance page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Quality_assurance) on Wikidata.

OpenRefine will analyze your schema and make suggestions. It does not check for conflicts in your proposed edits, or tell you about redundancies. 

One of the most common suggestions is to attach [a reference to your edits](https://www.wikidata.org/wiki/Help:Sources) - a citation for where the information can be found. This can be a book or newspaper citation, a URL to an online page, a reference to a physical source in an archival or special collection, or another source. If the source is itself an item on Wikidata, use the relationship [stated in (P248)](https://www.wikidata.org/wiki/Property:P248); otherwise, use [reference URL (P854)](https://www.wikidata.org/wiki/Property:P854) to identify an external source. 
