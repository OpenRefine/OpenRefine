---
id: overview
title: Overview of Wikibase support
sidebar_label: Overview
---

[Wikibase](https://wikiba.se/) is free software (a set of MediaWiki extensions) used by many organizations around the world to store and publish Linked Open Data. Wikibase is the software behind [Wikidata](https://www.wikidata.org/), a free, multilingual collaborative knowledge base and a sister project of Wikipedia. Wikidata offers structured data about the world and can be edited by anyone. Wikibase also powers [structured data](https://commons.wikimedia.org/wiki/Commons:Structured_data) on [Wikimedia Commons](https://commons.wikimedia.org/), the media repository of Wikipedia.

OpenRefine provides powerful ways to both pull data from a Wikibase (including Wikidata and Wikimedia Commons) and to add data to it.

OpenRefine's Wikibase integration is provided by an extension which is available by default in OpenRefine. In this page, we present the functionalities for [Wikidata](#editing-wikidata-with-openrefine) and [Wikimedia Commons](#editing-wikimedia-commons-with-openrefine), but [any Wikibase instance can be connected to OpenRefine](./configuration) to obtain a similar integration.

## Editing Wikidata with OpenRefine {#editing-wikidata-with-openrefine}

As a user-maintained data source, Wikidata can be edited by anyone. OpenRefine makes it simple to upload information in bulk. You simply need to get your information into the correct format, and ensure that it is new (not redundant to information already on Wikidata) and does not conflict with existing Wikidata information.

You do not need a Wikidata account to reconcile your local OpenRefine project to Wikidata, but to upload your cleaned dataset to Wikidata, you will need an [autoconfirmed](https://www.wikidata.org/wiki/Wikidata:Autoconfirmed_users) account, and you must [authorize OpenRefine with that account](#manage-wikidata-account).

Wikidata is built by creating entities (such as people, organizations, or places, identified with unique numbers starting with Q), defining properties (unique numbers starting with P), and using properties to define relationships between entities (a Q has a property P, with a value of another Q).

For example, you may wish to create entities for local authors and the books they've set in your community. Each writer will be an entity with the occupation [author (Q482980)](https://www.wikidata.org/wiki/Q482980), each book will be an entity with the property “instance of” ([P31](https://www.wikidata.org/wiki/Property:P31)) linking it to a class such as [literary work (Q7725634)](https://www.wikidata.org/wiki/Q7725634), and books will be related to authors through a property [author (P50)](https://www.wikidata.org/wiki/Property:P50). Books can have places where they are set, with the property [narrative location (P840)](https://www.wikidata.org/wiki/Property:P840).

To do this with OpenRefine, you'll need a column of publication titles that you have reconciled (and create new items where needed); each publication will have one or more locations in a “setting” column, which is also reconciled to municipalities or regions where they exist (and create new items where needed). Then you can add those new relationships, and create new entities for authors, books, and places where needed. You do not need columns for properties; those are defined later, in the creation of your [schema](#edit-wikidata-schema).

There is a list of [tutorials and walkthroughs on Wikidata](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing) that will allow you to see the full process. You can save your schemas and drafts in OpenRefine, and your progress stays in draft until you are ready to upload it to Wikidata.

Batches of edits to Wikidata that are created with OpenRefine can be undone. You can test out the uploading process by reconciling to several “sandbox” entities created specifically for drafting edits and learning about Wikidata:
* https://www.wikidata.org/wiki/Q4115189
* https://www.wikidata.org/wiki/Q13406268
* https://www.wikidata.org/wiki/Q15397819
* https://www.wikidata.org/wiki/Q64768399

If you upload edits that are redundant (that is, all the statements you want to make have already been made), nothing will happen. If you upload edits that conflict with existing information (such as a different birthdate than one already in Wikidata), it will be added as a second statement. OpenRefine produces no warnings as to whether your data replicates or conflicts with existing Wikidata elements.

You can use OpenRefine's reconciliation preview to look at the target Wikidata elements and see what information they already have, and whether the elements' histories have had similar edits reverted in the past.

### Wikidata schema {#wikidata-schema}

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

You may wish to refer to [this Wikidata tutorial on how OpenRefine handles Wikidata schema](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Tutorials/Basic_editing). For details about how each data type is handled in the Wikibase schema, see [Schema alignment](./schema-alignment).

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

There are two menu options in OpenRefine for applying your edits to Wikidata, and the details of the differences between the two can be found in the [Uploading page](./uploading). Under <span class="menuItems">Export</span> you will see <span class="menuItems">Wikidata edits...</span> and under <span class="menuItems">Extensions</span> you will see <span class="menuItems">Upload edits to Wikidata</span>. Both will bring up the same window for you to [log in with a Wikidata account](#manage-wikidata-account).

Once you are authorized, you will see a window with any outstanding issues. You can ignore these issues, but we recommend you resolve them.

If you are ready to upload your edits, you can provide an “Edit summary” - a short message describing the batch of edits you are making. It can be helpful to leave notes for yourself, such as “batch 1: authors A-G” or other indicators of your workflow progress. OpenRefine will show the progress of the upload as it is happening, but does not show a confirmaton window.

If your edits have been successful, you will see them listed on [your Wikidata user contributions page](https://www.wikidata.org/wiki/Special:Contributions/), and on the [Edit groups page](https://editgroups.toolforge.org/). All edits can be undone from this second interface.

### QuickStatements export {#quickstatements-export}

Your OpenRefine data can be exported in a format recognized by [QuickStatements](https://www.wikidata.org/wiki/Help:QuickStatements), a tool that creates Wikidata edits using text commands. OpenRefine generates “version 1” QuickStatements commands.

There are advantages to using QuickStatements rather than uploading your edits directly to Wikidata, including the way QuickStatements resolves duplicates and redundancies. You can learn more on QuickStatements' [Help page](https://www.wikidata.org/wiki/Help:QuickStatements), and on OpenRefine's [Uploading page](https://www.wikidata.org/wiki/Wikidata:Tools/OpenRefine/Editing/Uploading).

In order to use QuickStatements, you must authorize it with a Wikidata account that is [autoconfirmed](https://www.wikidata.org/wiki/Wikidata:Autoconfirmed_users) (it may appear as “MediaWiki” when you authorize).

Follow the [steps listed on this page](https://www.wikidata.org/wiki/Help:QuickStatements#Running_QuickStatements).
To prepare your OpenRefine data into QuickStatements, select <span class="menuItems">Export</span> → <span class="menuItems">QuickStatements file</span>, or <span class="menuItems">Extensions</span> → <span class="menuItems">Export to QuickStatements</span>. Exporting your schema from OpenRefine will generate a text file called `statements.txt` by default. Paste the contents of the text file into a new QuickStatements batch using version 1. You can find [version 1 of the tool (no longer maintained) here](https://wikidata-todo.toolforge.org/quick_statements.php). The text commands will be processed into Wikidata edits and previewed for you to review before submitting.

### Issue detection {#issue-detection}

This section is an overview of the [Quality assurance page](./quality-assurance).

OpenRefine will analyze your schema and make suggestions. It does not check for conflicts in your proposed edits, or tell you about redundancies.

One of the most common suggestions is to attach [a reference to your edits](https://www.wikidata.org/wiki/Help:Sources) - a citation for where the information can be found. This can be a book or newspaper citation, a URL to an online page, a reference to a physical source in an archival or special collection, or another source. If the source is itself an item on Wikidata, use the relationship [stated in (P248)](https://www.wikidata.org/wiki/Property:P248); otherwise, use [reference URL (P854)](https://www.wikidata.org/wiki/Property:P854) to identify an external source.

## Editing Wikimedia Commons with OpenRefine {#editing-wikimedia-commons-with-openrefine}

:::caution OpenRefine version 3.6 or newer needed!
Wikimedia Commons editing is possible with OpenRefine version 3.6 or newer. It is **not** supported in earlier versions.

More detailed guides and howtos for Wikimedia Commons editing can be found at OpenRefine's info page there: **https://commons.wikimedia.org/wiki/Commons:OpenRefine**
:::

[Wikimedia Commons](https://commons.wikimedia.org) is the shared media repository of the Wikimedia ecosystem; it is a sister project of Wikipedia. Wikimedia Commons contains (as of mid 2022) more than 85 million freely licensed media files (images, videos, scanned books, 3D files and more) that are used as illustrations on Wikipedia and that can be freely re-used by anyone.

Like other Wikimedia projects, Wikimedia Commons is a user-maintained resource which can be edited by anyone. As of version 3.6, OpenRefine can be used to bulk add structured, multilingual, machine-readable (linked) data to files on Wikimedia Commons.

:::info Structured data on Wikimedia Commons
Structured data on Commons is multilingual information about a media file that can be understood by humans, with enough consistency that it can also be uniformly processed by machines. Files on Wikimedia Commons can be described with multilingual concepts from Wikidata, Wikimedia's knowledge base.

<iframe width="480" height="270" src="https://www.youtube.com/embed/lmWmMIuCJVM" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

Wikimedia Commons [has an information portal about structured data](https://commons.wikimedia.org/wiki/Commons:Structured_data), which contains (among others) [information for cultural institutions](https://commons.wikimedia.org/wiki/Commons:Structured_data/GLAM), and [guidelines on data modeling](https://commons.wikimedia.org/wiki/Commons:Structured_data/Modeling).
:::

In order to be able to edit Wikimedia Commons files with OpenRefine, you need a [Wikimedia account](https://commons.wikimedia.org/wiki/Commons:First_steps/Account). You can use the same account across all Wikimedia projects (including Wikipedia, Wikidata and Wikimedia Commons); if you have already created an account on one of these projects, you can use that for Wikimedia Commons editing in OpenRefine as well.

### Add the Wikimedia Commons manifest to OpenRefine

In order to make edits to Wikimedia Commons possible, start by adding the Wikimedia Commons manifest to OpenRefine. This process is the same as [the generic one for connecting OpenRefine to any Wikibase instance](./configuration#for-wikibase-end-users). For Wikimedia Commons specifically, the process is as follows.

In the Wikidata extension menu, choose <span class="menuItems">Select Wikibase instance...</span>. If you haven't added other Wikibases yet, you will only see Wikidata in the resulting dialog window. Click <span class="menuItems">Add Wikibase</span>. You will be prompted to paste either a manifest URL, or paste the JSON directly. Wikimedia Commons' manifest URL is:
```
https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/wikimedia-commons-manifest.json
```

![Adding a Wikibase manifest.](/img/add-wikibase-manifest.png)

After adding this URL, you should now see Wikimedia Commons in your list of Wikibase instances. Click Wikimedia Commons to activate it. You can now close this dialog window by clicking the <span class="menuItems">Close</span> button.

An up-to-date list of Wikibase manifests, including Wikimedia Commons' most recent manifest, is available in [OpenRefine's Wikibase manifests repository](https://github.com/OpenRefine/wikibase-manifests).

Adding the Wikimedia Commons manifest in OpenRefine will also automatically add the [Wikimedia Commons reconciliation service](https://commonsreconcile.toolforge.org).

### Reconcile file names from Wikimedia Commons

Typically, an OpenRefine project for batch editing files on Wikimedia Commons will contain:
* A column with file names of files on Wikimedia Commons, reconciled through the Wikimedia Commons reconciliation service
* One or more columns with data that you want to add to these files, reconciled through the Wikidata reconciliation service

Usually, you will start editing Wikimedia Commons files in OpenRefine from a list of file names from Commons. You can obtain file names from one or more categories by, for instance, using the [PetScan](https://petscan.wmflabs.org) tool ([example of a PetScan query](https://petscan.wmflabs.org/?search_query=&edits%5Bbots%5D=both&cb_labels_no_l=1&ns%5B6%5D=1&project=wikimedia&edits%5Banons%5D=both&search_max_results=500&interface_language=en&since_rev0=&cb_labels_any_l=1&cb_labels_yes_l=1&categories=Photographs%20of%20kittens%20by%20Harry%20Whittier%20Frees&language=commons&edits%5Bflagged%5D=both&maxlinks=&doit=) to retrieve file names for one Commons category; change to Plain text format in the Output tab for a simple plain text list).

Make sure you have these file names in a column in an OpenRefine project. The file names can be in many different formats, and can also be bare/plain M-ids (MediaInfo identifiers, example: [M7620878](https://commons.wikimedia.org/entity/M7620878)). Next, reconcile this column of file names against the Wikimedia Commons reconciliation service, which makes sure that OpenRefine recognizes these files and can edit them later. You start the reconciliation process by selecting <span class="menuItems">Reconcile</span> → <span class="menuItems">Start reconciling...</span> in the file column's menu. Then select the Wikimedia Commons reconciliation service and click the <span class="menuItems">Start reconciling...</span> button.

If you don’t have the Wikimedia Commons reconciliation service installed in OpenRefine yet, click the button <span class="menuItems">Add standard service...</span> and paste `https://commonsreconcile.toolforge.org/en/api` there. You can find more info and documentation about the Commons reconciliation service at https://commonsreconcile.toolforge.org/.

### Extract Wikitext to process it further in OpenRefine

This step is optional, but may be very useful. Existing files on Wikimedia Commons are always described with so-called Wikitext, a plain-text description which contains (among other things) information about the file's creator, license, and one or more descriptive categories. Often, Wikitext contains information which is valuable to parse in OpenRefine and which can be converted to structured data (and reconciled with Wikidata) later.

For instance, the Wikitext of [this image](https://commons.wikimedia.org/wiki/File:Harry_Whittier_Frees_-_The_Bufkins_Twins_Were_Swinging.jpg) is:
```
== {{int:filedesc}} ==
{{Information
|Description={{en|1=Illustration from "The little folks of animal land". Inscription below image: "The Bufkins Twins Were Swinging"}}
{{ru|1=Фотография [[:w:ru:Фрис, Гарри Виттер|Гарри Виттера Фриса]] из книги «Маленький народец страны зверят» (The little folks of animal land). Подпись под картинкой: «Близнецы Бафкинс качаются» («The Bufkins Twins Were Swinging»)}}
|Source={{cite book |author=Harry Whittier Frees |year=1915 |title=The little folks of animal land |publisher=Lothrop, Lee & Shepard co. |pages=25| url=https://books.google.com/books?id=HcwTIRt2FvwC&pg=PA25}} Retrieved from Google Books.
|Author=[[:w:Harry Whittier Frees|Harry Whittier Frees]]
|Date=1915
|Permission={{PD-US}}
|other_versions=
}}

[[Category:Photographs of kittens by Harry Whittier Frees]]
[[Category:Cats with objects]]
```

You can extract Wikitext (and structured data statements) from a list of file names. Select <span class="menuItems">Edit column</span> → <span class="menuItems">Add columns from reconciled values...</span>

See [Add columns from reconciled values](./reconciling#add-columns-from-reconciled-values) for general information about this feature.

### Prepare columns with structured data

Based on the Wikitext that you may have just extracted, or with data from other sources, you may have (or add) columns with additional structured data, such as the files'

* [creator](https://commons.wikimedia.org/wiki/Commons:Structured_data/Modeling/Author)
* [source](https://commons.wikimedia.org/wiki/Commons:Structured_data/Modeling/Source)
* what is [depicted](https://commons.wikimedia.org/wiki/Commons:Structured_data/Modeling/Depiction) in the files
* [copyright status and license](https://commons.wikimedia.org/wiki/Commons:Structured_data/Modeling/Copyright)

Some of these columns will need to be reconciled with Wikidata (_not_ Wikimedia Commons!), since files on Wikimedia Commons are described with data from Wikidata.

![A typical OpenRefine project with Commons files and some structured data.](/img/commons-project.png)

:::info
Data models for structured data about media files on Commons are explained and discussed at https://commons.wikimedia.org/wiki/Commons:Structured_data/Modeling.
:::

### Upload edits to Wikimedia Commons

Finally, you will build a schema to model the Wikimedia Commons edits that OpenRefine will perform for each row in your project. See [schema alignment](./schema-alignment) for general documentation about this feature.

As of OpenRefine 3.6, you will see Wikimedia Commons-specific fields in the schema editor, provided that you have installed and activated the Wikimedia Commons manifest [as described above](#add-the-wikimedia-commons-manifest-to-openrefine). One such Commons-specific 'field' is the [multilingual file caption](https://commons.wikimedia.org/wiki/Commons:File_captions), for which [best practices are documented on Wikimedia Commons](https://commons.wikimedia.org/wiki/Commons:File_captions).

![A typical Wikimedia Commons schema inside OpenRefine.](/img/commons-schema.png)

You can now drag and drop, and/or enter the desired terms and statements in the [schema](./schema-alignment), preview your edits, log in to Wikimedia Commons and [upload your edits](./uploading) in the same way as for Wikidata or another Wikibase.

### Revert mistakes with the EditGroups tool

When checking [your user contributions](https://commons.wikimedia.org/wiki/Special:MyContributions), you will see your recent Wikimedia Commons edits done with OpenRefine. Each OpenRefine edit displays a _(details)_ hyperlink after the edit summary, which links to the edit batch in the [EditGroups](https://editgroups-commons.toolforge.org/) tool.

![Edits done by OpenRefine as listed in the user's contributions.](/img/commons-contribution.png)

In EditGroups, entire batches can be easily undone, in case some mistakes have been made.

All Wikimedia Commons batches with OpenRefine are listed at https://editgroups-commons.toolforge.org/?tool=OR.
