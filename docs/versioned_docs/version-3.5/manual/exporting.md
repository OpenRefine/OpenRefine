---
id: exporting
title: Exporting your work
sidebar_label: Exporting
---

## Overview {#overview}

Once your dataset is ready, you will need to get it out of OpenRefine and into the system of your choice. OpenRefine outputs a number of file formats, can upload your data directly into Google Sheets, and can create or update statements on Wikidata.

You can also [export your full project data](#export-a-project) so that it can be opened by someone else using OpenRefine (or yourself, on another computer).

## Export data {#export-data}

![A screenshot of the Export dropdown.](/img/export-menu.png)

Many of the options only export data in the current view - that is, with current filters and facets applied. Some will give you the choice to export your entire dataset or just the currently-viewed rows.

To export data from a project, click the <span class="menuItems">Export</span> dropdown button in the top right corner and pick the format you want. Your options are:

*   Tab-separated value (TSV) or Comma-separated value (CSV)
*   HTML-formatted table
*   Excel spreadsheet (XLS or XLSX)
*   Open Document Format (ODF) spreadsheet (ODS)
*   Upload to Google Sheets (requires [Google account authorization](starting#google-sheet-from-drive))
*   [Custom tabular exporter](#custom-tabular-exporter)
*   [SQL statement exporter](#sql-statement-exporter)
*   [Templating exporter](#templating-exporter), which generates JSON by default

You can also export reconciled data to Wikidata, or export your Wikidata schema for future use with other OpenRefine projects:

*   [Upload edits to Wikidata](wikibase/uploading#uploading-with-openrefine)
*   [Export to QuickStatements](wikibase/uploading#uploading-with-quickstatements) (version 1)
*   [Export Wikidata schema](wikibase/overview#import-and-export-schema)

### Custom tabular exporter {#custom-tabular-exporter}

![A screenshot of the custom tabular content tab.](/img/custom-tabular-exporter.png)

With the custom tabular exporter, you can choose which of your data to export, the separator you wish to use, and whether you'd like to download the result to your computer or upload it into a Google Sheet. 

On the <span class="tabLabels">Content</span> tab, you can drag and drop the columns appearing in the column list to reorder the output. The options for reconciled and date data are applied to each column individually. 

This exporter is especially useful with reconciled data, as you can choose whether you wish to output the cells' original values, the matched values, or the matched IDs. Ouputting “match entity's name”, “matched entity's ID”, or “cell's content” will output, respectively, the contents of `cell.recon.match.name`, `cell.recon.match.id`, and `cell.value`. 

“Output nothing for unmatched cells” will export empty cells for both newly-created matches and cells with no chosen matches. “Link to matched entity's page” will produce hyperlinked text in an HTML table output, but have no effect in other formats.

At this time, the date-formatting options in this window do not work. You can [keep track of this issue on Github](https://github.com/OpenRefine/OpenRefine/issues/3368).
In the future, you will be able to choose how to [output date-formatted cells](exploring#dates). You can create a custom date output by using [formatting according to the SimpleDateFormat parsing key found here](grelfunctions#todateo-b-monthfirst-s-format1-s-format2-).

![A screenshot of the custom tabular file download tab.](/img/custom-tabular-exporter2.png)

On the <span class="tabLabels">Download</span> tab, you can generate a preview of how the first ten rows of your dataset will output. If you do not choose one of the file formats on the right, the <span class="buttonLabels">Download</span> button will generate a text file. On the <span class="tabLabels">Upload</span> tab, you can create a new Google Sheet. 

With the <span class="tabLabels">Option Code</span> tab, you can copy JSON of your current custom settings to reuse on another export, or you can paste in existing JSON settings to apply to the current project. 

### SQL exporter {#sql-exporter}

The SQL exporter creates a SQL statement containing the data you’ve exported, which you can use to overwrite or add to an existing database. Choosing <span class="menuItems">Export</span> → <span class="menuItems">SQL exporter</span> will bring up a window with two tabs: one to define what data to output, and another to modify other aspects of the SQL statement, with options to preview and download the statement.  

![A screenshot of the SQL statement content window.](/img/sql-exporter.png)

The <span class="tabLabels">Content</span> tab allows you to craft your dataset into an SQL table. From here, you can choose which columns to export, the data type to export for each (or choose "VARCHAR"), and the maximum character length for each field (if applicable based on the data type). You can set a default value for empty cells after unchecking “Allow null” in one or more columns. 

With this output tool, you can choose whether to output only currently visible rows, or all the rows in your dataset, as well as whether to include empty rows. The option to “Trim column names” will remove their whitespace characters. 

![A screenshot of the SQL statement download window.](/img/sql-exporter2.png)

The <span class="tabLabels">Download</span> tab allows you to finalize your complete SQL statement. 

<span class="fieldLabels">Include schema</span> means that you will start your statement with the creation of a table. Without that, you will only have an INSERT statement. 

<span class="fieldLabels">Include content</span> means including the INSERT statement with data from your project. Without that, you will only create empty columns. 

You can include DROP and IF EXISTS if you require them, and set a name for the table to which the statement will refer.

You can then preview your statement, which will open up a new browser tab/window showing a statement with the first ten rows of your data (if included), or you can save a `.sql` file to your computer. 

### Templating exporter {#templating-exporter}

If you pick <span class="menuItems">Templating…</span> from the <span class="menuItems">Export</span> dropdown menu, you can “roll your own” exporter. This is useful for formats that we don't support natively yet, or won't support. The Templating exporter generates JSON by default. 

![A screenshot of the Templating exporter generating JSON by default.](/img/templating-exporter.png)

The Templating Export window allows you to set your own separators, prefix, and suffix to create a complete dataset in the language of your choice. In the <span class="fieldLabels">Row template</span> section, you can choose which columns to generate from each row by calling them with [variables](expressions#variables). 

This can be used to:
* output [reconciliation data](expressions#reconciliation), such as `cells["ColumnName"].recon.match.name`
* create multiple columns of output from different [member fields](expressions#variables) of a single project column
* employ [expressions](expressions) to modify data for output: for example, `cells["ColumnName"].value.toUppercase()`. 

Anything that appears inside doubled curly braces ({{ }}) is treated as a GREL expression; anything outside is generated as straight text. You can use Jython or Clojure by declaring it at the start:  
```
{{jython:return cells["ColumnName"].value}}
```

:::caution
Note that some syntax is different in this tool than elsewhere in OpenRefine: a forward slash must be escaped with a backslash, while other characters do not need escaping. You cannot, at this time, include a closing curly brace (}) anywhere in your expression, or it will cause it to malfunction.
:::

You can include [regular expressions](expressions#regular-expressions) as usual (inside forward slashes, with any GREL function that accepts them). For example, you could output a version of your cells with punctuation removed, using an expression such as 
```
{{jsonize(cells["ColumnName"].value.replaceChars("/[.!?$&,/]/",""))}}
```  

You could also simply output a plain-text document inserting data from your project into sentences: for example, "In `{{cells["Year"].value}}` we received `{{cells["RequestCount"].value}}` requests."

You can use the shorthand `${ColumnName}` (no need for quotes) to insert column values directly. You cannot use this inside an expression, because of the closing curly brace.

If your projects is in records mode, the <span class="fieldLabels">Row separator</span> field will insert a separator between records, rather than individual rows. Rows inside a single record will be directly appended to one another as per the content in the <span class="fieldLabels">Row Template</span> field. 

Once you have created your template, you may wish to save the text you produced in each field, in order to reuse it in the future. Once you click <span class="buttonLabels">Export</span> OpenRefine will output a simple `.txt` file, and your template will be discarded.

We have recipes on using the Templating exporter to [produce several different formats](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#12-templating-exporter).

## Export a project {#export-a-project}

You can share a project in progress with another computer, a colleague, or with someone who wants to check your history. This can be useful for showing that your data cleanup didn’t distort or manipulate the information in any way. Once you have exported a project, another OpenRefine installation can [import it as a new project](starting#import-a-project). 

You can either save it locally or upload it to Google Drive (which requires you to authorize a Google account).

:::caution
OpenRefine project archives contain confidential data from previous steps, which will still be accessible to anyone who has the archive. If you are hoping to keep your original dataset hidden for privacy reasons, such as using OpenRefine to anonymize information, do not share your project archive.
:::

To save your project archive locally: from the <span class="menuItems">Export</span> dropdown, select <span class="menuItems">OpenRefine project archive to file</span>. OpenRefine exports your full project with all of its history. It does not export any current views or applied facets. Existing reconciliation information will be preserved, but the importing computer will need to add the same reconciliation services to keep working with that data. 

OpenRefine exports files in `.tar.gz` format. You can rename the file when you save it; otherwise it will bear the project name. 

To save your project archive to Google Drive: from the <span class="menuItems">Export</span> dropdown, select <span class="menuItems">OpenRefine project archive to Google Drive...</span>. OpenRefine will not share the link with you, only confirm that the file was uploaded.

## Export operations {#export-operations}

You can [save and re-apply the history of any project](running#reusing-operations) (all the operations shown in the Undo/Redo tab). This creates JSON that you can save for later reuse on another OpenRefine project.
