---
id: exporting
title: Exporting your work
sidebar_label: Exporting
---

## Overview

Once your data is cleaned, you will need to get it out of OpenRefine and into the system of your choice. OpenRefine outputs a number of file formats, can upload your data directly into Google Sheets, and can export directly to Wikidata. 

You can also export your full project data so that it can be opened by someone else using OpenRefine (or yourself, on another computer).

## Export data

Note you will only export data in the current view - that is, with current filters and facets applied. 

To export from a project, click the Export button at the top right corner and pick the format you want. You options are:

*   Tab-separated value (TSV) or Comma-separated value (CSV)
*   HTML-formatted table
*   Excel (XLS or XLSX)
*   ODF spreadsheet
*   Upload to Google Sheets (requires Google account authorization)
*   [Custom tabular exporter](#custom-tabular-exporter)
*   [SQL statement exporter](#sql-statement-exporter)
*   [Templating exporter](#templating-exporter)

You can also [export reconciled data to Wikidata](wikidata#editing-wikidata-with-openrefine), or save your schema for future use with other OpenRefine projects:

*   Upload edits to Wikidata
*   Export to QuickStatement
*   Export Wikidata schema

### Custom tabular exporter

![A screenshot of the custom tabular content tab.](/img/custom-tabular-exporter.png)

With the custom tabular exporter, you can choose which of your data to export, the separator you wish to use, and whether you'd like to download it to your computer or upload it into a Google Sheet. 

You can drag and drop the columns appearing in the column list to reorder the output. The options for reconciled and date data are applied to each column individually. 

This exporter is especially useful with reconciled data, as you can choose whether you wish to output the cells' original values, the matched values, or the matched IDs. Ouputting “match entity's name”, “matched entity's ID”, or “cell's content” will output, respectively, the contents of `cell.recon.match.name`, `cell.recon.match.id`, and `cell.value`. 

“Output nothing for unmatched cells” will export empty cells for both newly-created matches and cells with no chosen matches. “Link to matched entity's page” will produce hyperlinked text in an HTML table output, but have no change in other formats.

You can also choose how to [output date-formatted cells](exploring#dates). You can create a custom date output by using [formatting according to the SimpleDateFormat parsing key found here](grelfunctions#todateo-b-monthfirst-s-format1-s-format2-).

![A screenshot of the custom tabular file download tab.](/img/custom-tabular-exporter2.png)

With the download options, you can generate a preview of how the first ten rows of your dataset will output. If you do not choose one of the file formats on the right, the "Download" button will generate a text file. 

With the <span class="menuItems">Option Code</span> tab, you can copy JSON data that saves your current settings, to reuse on another project, or you can paste in existing JSON settings data to apply to the current project. 

### SQL exporter

The SQL exporter creates a SQL statement containing the data you’ve exported, which you can use to overwrite or add to an existing database. 

![A screenshot of the SQL statement creator window.](/img/sql-exporter.png)

A window with two tabs will pop up: one to define what data to output, and another to modify the statement table. This allows you to craft a complete SQL command, including DROP and IF EXISTS if you require them. 

You can set a default value for empty cells after unchecking “Allow null” in one or more columns. Trimming column names will remove their whitespace characters. 

![A screenshot of the SQL statement download window.](/img/sql-exporter2.png)

“Include schema” means that you will start your statement with the creation of a table. Without that, you will only have an INSERT statement. 
“Include content” means the INSERT statement with data from your project. Without that, you will only create empty columns. 

You can preview the statement, which will open up a new browser tab/window showing a statement with the first ten rows of your data, or you can save a `.sql` file to your computer. 

### Templating exporter

If you pick <span class="menuItems">Templating…</span> from the <span class="menuItems">Export</span> dropdown menu, you can “roll your own” exporter. This is useful for formats that we don't support natively yet, or won't support. The Templating exporter generates JSON by default. 

The window that appears allows you to set your own separators, prefix, and suffix to create a complete dataset in the language of your choice. In the <span class="menuItems">Row Template</span> section, you can choose which columns to generate from each row by calling them with variables. 

This can be used to:
* output reconciliation data (`cells["column name"].recon.match.name`, `.recon.match.id`, and `.recon.best.name`, for example) instead of cell values
* create multiple columns of output from different member fields of a single project column
* employ GREL expressions to modify cell data for output (for example, `cells["column name"].value.toUppercase()`). 

Anything that appears inside curly braces ({{}}) is treated as a GREL expression; anything outside is generated as straight text. You can include [regular expressions](expressions#regular-expressions) as usual (inside forward slashes, with any GREL function that accepts them), with the exception that you cannot use curly braces (for example, to denote a `{min,max}` capture range). 

You could also simply output a plain-text document inserting data from your project into sentences (for example, "In `{{cells["Year"].value}}` we received `{{cells["RequestCount"].value}}` requests.").

![A screenshot of the Templating exporter generating JSON by default.](/img/templating-exporter.png)

Once you have created your template, you may wish to save the text you produced in each field, in order to reuse it in the future. Once you click “Export” OpenRefine will output a simple text file, and your template will be discarded.

We have recipes on using the Templating exporter to [produce several different formats](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#12-templating-exporter).

## Export a project

You can share a project in progress with another computer, a colleague, or with someone who wants to check your history. This can be useful for showing that your data cleanup didn’t distort or manipulate the information in any way. Once you have exported a project, another OpenRefine installation can [import it as a new project](starting#import-a-project). 

From the <span class="menuItems">Export</span> dropdown, select <span class="menuItems">OpenRefine project archive to file</span>. OpenRefine exports your full project with all of its history. It does not export any current views or applied facets. Any reconciliation information will be preserved, but the importing installation will need to add the same reconciliation services to keep working with that data. 

OpenRefine exports files in `.tar.gz` format. You can rename the file when you save it; otherwise it will bear the project name. You can either save it locally or upload it to Google Drive (which requires you to authorize a Google account), using the <span class="menuItems">OpenRefine project archive to Google Drive...</span> option. OpenRefine will not share the link with you, only confirm that the file was uploaded.

## Export operations

You can [save and re-apply the history of any project](running#reusing-operations) (all the operations shown in the Undo/Redo tab). This creates JSON that you can save for later reuse on another OpenRefine project.