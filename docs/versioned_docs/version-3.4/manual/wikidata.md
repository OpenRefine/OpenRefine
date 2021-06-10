---
id: wikidata
title: Wikidata
sidebar_label: Wikidata
---

## Overview {#overview}

OpenRefine provides powerful ways to both pull data from Wikidata and add data to it. 

You do not need a Wikidata account to reconcile your local OpenRefine project to Wikidata. If you wish to [upload your cleaned dataset to Wikidata](#editing-wikidata-with-openrefine), you will need an [autoconfirmed](https://www.wikidata.org/wiki/Wikidata:Autoconfirmed_users) account, and you must [authorize OpenRefine with that account](#manage-wikidata-account). 

:::info A better resource
The best source for information about how OpenRefine works with Wikidata is [on Wikidata itself, under Tools](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine). That page has tutorials, guidelines on editing, and spaces for discussion and help. The following text on this page reviews the basics and can help you get set up, but the Wikidata help page is more regularly updated when technology or policies change. Links to the Wikidata help page are included throughout this page.
:::

OpenRefine’s connections to Wikidata were formerly an optional extension, but are now included automatically with installation. The Wikidata extension can be removed manually by navigating to your OpenRefine installation folder, and then looking inside `webapp/extensions/` and deleting the `wikidata` folder found there. 

## Reconciling with Wikidata {#reconciling-with-wikidata}

The Wikidata [reconciliation service](reconciling) for OpenRefine [supports](https://reconciliation-api.github.io/testbench/):
*   A large number of potential types to reconcile against
*   Previewing and viewing entities
*   Suggesting entities, types, and properties
*   Augmenting your project with more information pulled from Wikidata. 

You can find documentation and further resources on the reconciliation API [here](https://wikidata.reconci.link/).

For the most part, Wikidata reconciliation behaves the same way other reconciliation services do, but there are a few processes and features specific to Wikidata. 

### Language settings {#language-settings}

You can install a version of the Wikidata reconciliation service that uses your language. First, you need the language code: this is the [two-letter code found on this list](https://en.wikipedia.org/wiki/List_of_Wikipedias), or in the domain name of the desired Wikipedia/Wikidata (for instance, “fr” if your Wikipedia is https://fr.wikipedia.org/wiki/).

Then, open the reconciliation window (under <span class="menuItems">Reconcile</span> → <span class="menuItems">Start reconciling...</span>) and click <span class="menuItems">Add Standard Service</span>. The URL to enter is `https://wikidata.reconci.link/fr/api`, where “fr” is your desired language code.

When reconciling using this interface, items and properties will be displayed in your chosen language if the label is available. The matching score of the reconciliation is not influenced by your choice of language for the service: items are matched by considering all labels and returning the best possible match. The language of your dataset is also irrelevant to your choice of language for the reconciliation service; it simply determines which language labels to return based on the entity chosen.

### Restricting matches by type {#restricting-matches-by-type}

In Wikidata, types are items themselves. For instance, the [university of Ljubljana (Q1377)](https://www.wikidata.org/wiki/Q1377) has the type [public university (Q875538)](https://www.wikidata.org/wiki/Q875538), using the [instance of (P31)](https://www.wikidata.org/wiki/Property:P31) property. Types can be subclasses of other types, using the [subclass of (P279)](https://www.wikidata.org/wiki/Property:P279) property. For instance, [public university (Q875538)](https://www.wikidata.org/wiki/Q875538) is a subclass of [university (Q3918)](https://www.wikidata.org/wiki/Q3918). You can visualize these structures with the [Wikidata Graph Builder](https://angryloki.github.io/wikidata-graph-builder/). 

When you select or enter a type for reconciliation, OpenRefine will include that type and all of its subtypes. For instance, if you select [university (Q3918)](https://www.wikidata.org/wiki/Q3918), then [university of Ljubljana (Q1377)](https://www.wikidata.org/wiki/Q1377) will be a possible match, though that item isn't directly linked to Q3918 - because it is directly linked to Q875538, the subclass of Q3918.

Some items and types may not yet be set as an instance or subclass of anything (because Wikidata is crowdsourced). If you restrict reconciliation to a type, items without the chosen type will not appear in the results, except as a fallback, and will have a lower score.

### Reconciling via unique identifiers {#reconciling-via-unique-identifiers}

You can supply a column of unique identifiers (in the form "Q###" for entities) directly to Wikidata in order to pull more data, but [these strings will not be “reconciled” against the external dataset](reconciling#reconciling-with-unique-identifiers). Apply the operation <span class="menuItems">Reconcile</span> → <span class="menuItems">Use values as identifiers</span> on your column of QIDs. All cells will appear as dark blue “confirmed” matches. Some of the “matches” may be errors, which you will need to hover over or click on to identify. You cannot use this to reconcile properties (in the form "P###").

If the identifier you submit is assigned to multiple Wikidata items (because Wikidata is crowdsourced), all of the items are returned as candidates, with none automatically matched.

### Property paths, special properties, and subfields {#property-paths-special-properties-and-subfields}

Wikidata's hierarchical property structure can be called by using property paths (using |, /, and . symbols). Labels, aliases, descriptions, and sitelinks can also be accessed. You can also match values against subfields, such as latitude and longitude subfields of a geographical coordinate.

For information on how to do this, read the [documentation and further resources here](https://wikidata.reconci.link/#documentation).

## Editing Wikidata with OpenRefine {#editing-wikidata-with-openrefine}

The best resource is the [Editing section](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing) on Wikidata.

As a user-maintained data source, Wikidata can be edited by anyone. OpenRefine makes it simple to upload information in bulk. You simply need to get your information into the correct format, and ensure that it is new (not redundant to information already on Wikidata) and does not conflict with existing Wikidata information.

Wikidata is built by creating entities (such as people, organizations, or places, identified with unique numbers starting with Q), defining properties (unique numbers starting with P), and using properties to define relationships between entities (a Q has a property P, with a value of another Q). 

For example, you may wish to create entities for local authors and the books they've set in your community. Each writer will be an entity with the occupation [author (Q482980)](https://www.wikidata.org/wiki/Q482980), each book will be an entity with the property “instance of” ([P31](https://www.wikidata.org/wiki/Property:P31)) linking it to a class such as [literary work (Q7725634)](https://www.wikidata.org/wiki/Q7725634), and books will be related to authors through a property [author (P50)](https://www.wikidata.org/wiki/Property:P50). Books can have places where they are set, with the property [narrative location (P840)](https://www.wikidata.org/wiki/Property:P840). 

To do this with OpenRefine, you'll need a column of publication titles that you have reconciled (and create new items where needed); each publication will have one or more locations in a “setting” column, which is also reconciled to municipalities or regions where they exist (and create new items where needed). Then you can add those new relationships, and create new entities for authors, books, and places where needed. You do not need columns for properties; those are defined later, in the creation of your [schema](#edit-wikidata-schema).

There is a list of [tutorials and walkthroughs on Wikidata](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing) that will allow you to see the full process. You can save your schemas and drafts in OpenRefine, and your progress stays in draft until you are ready to upload it to Wikidata. You can also find information on [how to design a schema](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Schema_alignment) and [how OpenRefine evaluates your proposed edits for issues](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Quality_assurance).

Batches of edits to Wikidata that are created with OpenRefine can be undone. You can test out the uploading process by reconciling to several “sandbox” entities created specifically for drafting edits and learning about Wikidata:
* https://www.wikidata.org/wiki/Q4115189
* https://www.wikidata.org/wiki/Q13406268
* https://www.wikidata.org/wiki/Q15397819
* https://www.wikidata.org/wiki/Q64768399

If you upload edits that are redundant (that is, all the statements you want to make have already been made), nothing will happen. If you upload edits that conflict with existing information (such as a different birthdate than one already in Wikidata), it will be added as a second statement. OpenRefine produces no warnings as to whether your data replicates or conflicts with existing Wikidata elements. 

You can use OpenRefine's reconciliation preview to look at the target Wikidata elements and see what information they already have, and whether the elements' histories have had similar edits reverted in the past. 

### Edit Wikidata schema {#edit-wikidata-schema}

The best resource is the [Schema alignment page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Schema_alignment) on Wikidata.

A [schema](https://en.wikipedia.org/wiki/Database_schema) is a plan for how to structure information in a database. In OpenRefine, the schema operates as a template for how Wikidata edits should be applied: how to translate your tabular data into statements. With a schema, you can:
*   preview the Wikidata edits and inspect them manually;
*   analyze and fix any issues highlighted by OpenRefine;
*   upload your changes to Wikidata by logging in with your own account;
*   export the changes to the QuickStatements v1 format.

For example, if your dataset has columns for authors, publication titles, and publication years, your schema can be conceptualized as: [publication title] has the author [author], and was published in [publication year]. To establish these facts, you need to establish one or more columns as “items,” for which you will make “statements” that relate them to other columns. 

You can export any schema you create, and import an existing schema for use with a new dataset. This can help you work in batches on a large amount of data while minimizing redundant labor.

Once you select <span class="menuItems">Edit Wikidata schema</span> under the <span class="menuItems">Extensions</span> dropdown menu, your project interface will change. You’ll see new tabs added to the right of “X rows/records" in the grid header: “Schema,” “Issues,” and “Preview.” You can now switch between the tabular grid format of your dataset and the screens that allow you to prepare data for uploading. 

OpenRefine presents you with an easy visual way to map out the relationships in your dataset. Each of the columns of your project will appear at the top of the sceren, and you can simply drag and drop them into the appropriate slots. To get start, select one column as an item. 

![A screenshot of the schema construction window in OpenRefine.](/img/wikidata-schema.png)

You may wish to refer to [this Wikidata tutorial on how OpenRefine handles Wikidata schema](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Tutorials/Basic_editing).

#### Editing terms with your schema {#editing-terms-with-your-schema}

With OpenRefine, you can edit the terms (labels, aliases, descriptions, or sitelinks) of Wikidata entities as well as establish relationships between entities. For example, you may wish to upload pseudonyms, pen names, maiden names, or married names for authors. 

![An author with a number of aliases indicating pseudonyms.](/img/wikidata-terms.png)

You can do so by putting the preferred names in one column of your dataset and alternative names in another column. In the schema interface, add an item for the preferred values, then click “Add term” on the right-hand side of the screen. Select “Alias” from the dropdown, enter in “English” in the language field, and drop your alternative names column into the space. For this example, you should also consider adding those alternative names to the authors' entries using the property [pseudonym (P742)](https://www.wikidata.org/wiki/Property:P742). The "description" and "label" terms can only contain one value, so there is an option to override existing values if needed. Aliases can be potentially infinite. 

![The schema window showing a term being edited.](/img/wikidata-terms2.png)

Terms must always have an associated language. You can select the term's language by typing in the “lang” field, which will auto-complete for you. You cannot edit multiple languages at once, unless you supply a suitable column instead. For example, suppose you had translated publication titles, with data in the following format:

|English title|Translated title|Translation language|
|---|---|---|
|Possession|Besessen|German|
||Обладать|Russian|
|Disgrace|Disgrâce|French|
||Vergogna|Italian|
|Wolf Hall|En la corte del lobo|Spanish|
||ウルフ・ホール|Japanese|

You could upload the “Translated titles” to “Label” with the language specified by “Translation language.” You may wish to fetch the two-letter language code and use that instead for better language matches.

![Constructing a schema with aliases and languages.](/img/wikidata-translated.png)

### Manage Wikidata account {#manage-wikidata-account}

To edit Wikidata directly from OpenRefine, you must log in with a Wikidata account. OpenRefine can only upload edits with Wikidata user accounts that are “[autoconfirmed](https://www.wikidata.org/wiki/Wikidata:Autoconfirmed_users)” - at this time, that means accounts that have more than 50 edits and have existed for longer than four days. 

Use the <span class="menuItems">Extensions</span> menu to select <span class="menuItems">Manage Wikidata account</span> and you will be presented with the following window:

![The Wikidata authorization window in OpenRefine.](/img/wikidata-login.png)

For security reasons, you should not use your main account authorization with OpenRefine. Wikidata allows you to set special passwords to access your account through software. You can find [this setting for your account here](https://www.wikidata.org/wiki/Special:BotPasswords) once logged in. Creating bot access will prompt you for a unique name. You should then enable the following required settings:
* High-volume editing
* Edit existing pages
* Create, edit, and move pages

It will then generate a username (in the form of “yourwikidatausername@yourbotname”) and password for you to use with OpenRefine.

If your account or your bot is not properly authorized, OpenRefine will not display a warning or error when you try to upload your edits.

You can store your unencrypted username and password in OpenRefine, saved locally to your computer and available for future use. For security reasons, you may wish to leave this box unchecked. You can also save your OpenRefine-specific bot password in your browser or with a password management tool. 

### Import and export schema {#import-and-export-schema}

You can save time on repetitive processes by defining a schema on one project, then exporting it and importing for use on new datasets in the future. Or you and your colleagues can share a schema with each other to coordinate your work. 

You can export a schema from a project using <span class="menuItems">Export</span> → <span class="menuItems">Wikidata schema</span>, or by using <span class="menuItems">Extensions</span> → <span class="menuItems">Export schema</span>. OpenRefine will generate a JSON file for you to save and share. You may experience issues with pop-up windows in your browser: consider allowing pop-ups from the OpenRefine URL (`127.0.0.1`) from now on.

You can import a schema using <span class="menuItems">Extensions</span> → <span class="menuItems">Import schema</span>. You can upload a JSON file, or paste JSON statements directly into a field in the window. An imported schema will look for columns with the same names, and you will see an error message if your project doesn't contain matching columns.

### Upload edits to Wikidata {#upload-edits-to-wikidata}

The best resource is the [Uploading page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Uploading) on Wikidata.

There are two menu options in OpenRefine for applying your edits to Wikidata. Under <span class="menuItems">Export</span> you will see <span class="menuItems">Wikidata edits...</span> and under <span class="menuItems">Extensions</span> you will see <span class="menuItems">Upload edits to Wikidata</span>. Both will bring up the same window for you to [log in with a Wikidata account](#manage-wikidata-account). 

Once you are authorized, you will see a window with any outstanding issues. You can ignore these issues, but we recommend you resolve them. 

If you are ready to upload your edits, you can provide an “Edit summary” - a short message describing the batch of edits you are making. It can be helpful to leave notes for yourself, such as “batch 1: authors A-G” or other indicators of your workflow progress. OpenRefine will show the progress of the upload as it is happening, but does not show a confirmaton window. 

If your edits have been successful, you will see them listed on [your Wikidata user contributions page](https://www.wikidata.org/wiki/Special:Contributions/), and on the [Edit groups page](https://editgroups.toolforge.org/). All edits can be undone from this second interface.

### QuickStatements export {#quickstatements-export}

Your OpenRefine data can be exported in a format recognized by [QuickStatements](https://www.wikidata.org/wiki/Help:QuickStatements), a tool that creates Wikidata edits using text commands. OpenRefine generates “version 1” QuickStatements commands. 

There are advantages to using QuickStatements rather than uploading your edits directly to Wikidata, including the way QuickStatements resolves duplicates and redundancies. You can learn more on QuickStatements' [Help page](https://www.wikidata.org/wiki/Help:QuickStatements), and on OpenRefine's [Uploading page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Uploading).

In order to use QuickStatements, you must authorize it with a Wikidata account that is [autoconfirmed](https://www.wikidata.org/wiki/Wikidata:Autoconfirmed_users) (it may appear as “MediaWiki” when you authorize). 

Follow the [steps listed on this page](https://www.wikidata.org/wiki/Help:QuickStatements#Running_QuickStatements). 
To prepare your OpenRefine data into QuickStatements, select <span class="menuItems">Export</span> → <span class="menuItems">QuickStatements file</span>, or <span class="menuItems">Extensions</span> → <span class="menuItems">Export to QuickStatements</span>. Exporting your schema from OpenRefine will generate a text file called `statements.txt` by default. Paste the contents of the text file into a new QuickStatements batch using version 1. You can find [version 1 of the tool (no longer maintained) here](https://wikidata-todo.toolforge.org/quick_statements.php). The text commands will be processed into Wikidata edits and previewed for you to review before submitting. 

### Schema alignment {#schema-alignment}

The best resource is the [Schema alignment page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Schema_alignment) on Wikidata.

### Issue detection {#issue-detection}

The best resource is the [Quality assurance page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Quality_assurance) on Wikidata.

OpenRefine will analyze your schema and make suggestions. It does not check for conflicts in your proposed edits, or tell you about redundancies. 

One of the most common suggestions is to attach [a reference to your edits](https://www.wikidata.org/wiki/Help:Sources) - a citation for where the information can be found. This can be a book or newspaper citation, a URL to an online page, a reference to a physical source in an archival or special collection, or another source. If the source is itself an item on Wikidata, use the relationship [stated in (P248)](https://www.wikidata.org/wiki/Property:P248); otherwise, use [reference URL (P854)](https://www.wikidata.org/wiki/Property:P854) to identify an external source. 
